const express = require('express');
const { body, param, query } = require('express-validator');

const db = require('../config/db');
const auth = require('../middleware/auth');
const auditLog = require('../middleware/auditLog');
const { recalculateScheduleFromDecision } = require('../utils/scheduleCalculator');
const {
  asyncHandler,
  handleValidationErrors,
  successResponse,
  formatDate,
  formatDateTime,
} = require('../utils/apiHelpers');

const router = express.Router();

router.use(auth);

router.get(
  '/',
  [query('search').optional().trim().isLength({ max: 100 })],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const search = req.query.search ? `%${req.query.search.trim()}%` : null;
    const sql = search
      ? `
        SELECT *
        FROM patients
        WHERE mrn LIKE ?
          OR first_name LIKE ?
          OR last_name LIKE ?
          OR CONCAT(first_name, ' ', last_name) LIKE ?
          OR DATE_FORMAT(date_of_birth, '%Y-%m-%d') LIKE ?
        ORDER BY last_name ASC, first_name ASC
      `
      : 'SELECT * FROM patients ORDER BY last_name ASC, first_name ASC';
    const params = search ? [search, search, search, search, search] : [];
    const [rows] = await db.execute(sql, params);

    return successResponse(res, rows, 'Patients retrieved successfully');
  })
);

router.post(
  '/',
  auditLog('create_patient', 'patient'),
  [
    body('mrn').trim().notEmpty().isLength({ max: 100 }),
    body('first_name').trim().notEmpty().isLength({ max: 100 }),
    body('last_name').trim().notEmpty().isLength({ max: 100 }),
    body('date_of_birth').isISO8601(),
    body('allergen').trim().notEmpty().isLength({ max: 255 }),
    body('oit_protocol').trim().notEmpty().isLength({ max: 255 }),
    body('current_dose_mcg').optional().isFloat({ min: 0 }),
  ],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const { mrn, first_name, last_name, date_of_birth, allergen, oit_protocol, current_dose_mcg = 0 } = req.body;

    const [existing] = await db.execute('SELECT id FROM patients WHERE mrn = ? LIMIT 1', [mrn]);
    if (existing.length) {
      return res.status(400).json({ error: true, message: 'Patient MRN already exists', details: [] });
    }

    const [result] = await db.execute(
      `
        INSERT INTO patients (mrn, first_name, last_name, date_of_birth, allergen, oit_protocol, current_dose_mcg)
        VALUES (?, ?, ?, ?, ?, ?, ?)
      `,
      [mrn, first_name, last_name, date_of_birth, allergen, oit_protocol, current_dose_mcg]
    );

    await req.audit({
      patientId: result.insertId,
      entityId: result.insertId,
      details: { mrn, first_name, last_name, current_dose_mcg },
    });

    return successResponse(
      res,
      {
        id: result.insertId,
        mrn,
        first_name,
        last_name,
        date_of_birth,
        allergen,
        oit_protocol,
        current_dose_mcg,
      },
      'Patient created successfully',
      201
    );
  })
);

router.get(
  '/barcode/:barcode',
  [param('barcode').trim().notEmpty()],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const [rows] = await db.execute('SELECT * FROM patients WHERE mrn = ? LIMIT 1', [req.params.barcode]);
    const patient = rows[0];

    if (!patient) {
      return res.status(404).json({ error: true, message: 'Patient not found', details: [] });
    }

    return successResponse(res, patient, 'Patient retrieved successfully');
  })
);

router.get(
  '/:id/dose-plan',
  [param('id').isInt({ min: 1 })],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const patientId = Number(req.params.id);
    const [patientRows] = await db.execute('SELECT id FROM patients WHERE id = ? LIMIT 1', [patientId]);
    if (!patientRows.length) {
      return res.status(404).json({ error: true, message: 'Patient not found', details: [] });
    }

    const [dosePlans] = await db.execute(
      'SELECT * FROM dose_plans WHERE patient_id = ? ORDER BY scheduled_date ASC',
      [patientId]
    );

    return successResponse(res, dosePlans, 'Dose plan retrieved successfully');
  })
);

router.post(
  '/:id/updose',
  auditLog('record_updose_decision', 'updose_decision'),
  [
    param('id').isInt({ min: 1 }),
    body('decision_type').isIn(['continue', 'advance', 'custom']),
    body('new_dose_mcg').isFloat({ min: 0 }),
    body('notes').optional().isString(),
    body('dose_form').optional().isIn(['milk', 'powder']),
    body('interval_days').optional().isInt({ min: 1, max: 365 }),
    body('count').optional().isInt({ min: 1, max: 104 }),
    body('start_date').optional().isISO8601(),
  ],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const patientId = Number(req.params.id);
    const { decision_type, new_dose_mcg, notes = null, dose_form = 'milk', interval_days = 14, count = 52, start_date } = req.body;
    const connection = await db.getConnection();

    try {
      await connection.beginTransaction();

      const [patientRows] = await connection.execute('SELECT * FROM patients WHERE id = ? LIMIT 1 FOR UPDATE', [patientId]);
      const patient = patientRows[0];

      if (!patient) {
        await connection.rollback();
        return res.status(404).json({ error: true, message: 'Patient not found', details: [] });
      }

      const decisionDate = new Date();
      const [decisionResult] = await connection.execute(
        `
          INSERT INTO updose_decisions
          (patient_id, previous_dose_mcg, new_dose_mcg, decision_date, decision_type, notes, decided_by_user_id)
          VALUES (?, ?, ?, ?, ?, ?, ?)
        `,
        [patientId, patient.current_dose_mcg, new_dose_mcg, formatDateTime(decisionDate), decision_type, notes, req.user.id]
      );

      await connection.execute('UPDATE patients SET current_dose_mcg = ? WHERE id = ?', [new_dose_mcg, patientId]);

      const scheduleDates = await recalculateScheduleFromDecision(
        patientId,
        {
          decisionDate,
          startDate: start_date || decisionDate,
          intervalDays: interval_days,
          count,
          newDoseMcg: new_dose_mcg,
          doseForm: dose_form,
        },
        connection
      );

      await req.audit(
        {
          patientId,
          entityId: decisionResult.insertId,
          details: {
            previous_dose_mcg: patient.current_dose_mcg,
            new_dose_mcg,
            decision_type,
            schedule_count: scheduleDates.length,
          },
        },
        connection
      );

      await connection.commit();

      return successResponse(
        res,
        {
          decision: {
            id: decisionResult.insertId,
            patient_id: patientId,
            previous_dose_mcg: patient.current_dose_mcg,
            new_dose_mcg,
            decision_type,
            notes,
            decision_date: decisionDate,
          },
          schedule: scheduleDates,
        },
        'Updose decision recorded successfully',
        201
      );
    } catch (error) {
      await connection.rollback();
      throw error;
    } finally {
      connection.release();
    }
  })
);

router.get(
  '/:id',
  [param('id').isInt({ min: 1 })],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const patientId = Number(req.params.id);
    const [patientRows] = await db.execute('SELECT * FROM patients WHERE id = ? LIMIT 1', [patientId]);
    const patient = patientRows[0];

    if (!patient) {
      return res.status(404).json({ error: true, message: 'Patient not found', details: [] });
    }

    const [nextDoseRows, activeSyringes, activeCartons, powderSupplies, recentMar] = await Promise.all([
      db.execute(
        `
          SELECT * FROM dose_plans
          WHERE patient_id = ? AND status = 'scheduled' AND scheduled_date >= CURDATE()
          ORDER BY scheduled_date ASC
          LIMIT 1
        `,
        [patientId]
      ),
      db.execute(
        `
          SELECT * FROM syringes
          WHERE patient_id = ? AND status = 'ready'
          ORDER BY scheduled_dose_date ASC, created_at DESC
        `,
        [patientId]
      ),
      db.execute(
        `
          SELECT * FROM cartons
          WHERE patient_id = ? AND status = 'active'
          ORDER BY expiration_date ASC
        `,
        [patientId]
      ),
      db.execute(
        `
          SELECT * FROM powder_supplies
          WHERE patient_id = ? AND status = 'active'
          ORDER BY expiration_date ASC
        `,
        [patientId]
      ),
      db.execute(
        `
          SELECT mar_entries.*, users.username AS administered_by_username
          FROM mar_entries
          INNER JOIN users ON users.id = mar_entries.administered_by_user_id
          WHERE mar_entries.patient_id = ?
          ORDER BY mar_entries.administered_at DESC
          LIMIT 10
        `,
        [patientId]
      ),
    ]);

    return successResponse(
      res,
      {
        patient,
        current_dose: patient.current_dose_mcg,
        next_updose: nextDoseRows[0][0] || null,
        active_syringes: activeSyringes[0],
        active_cartons: activeCartons[0],
        powder_supplies: powderSupplies[0],
        recent_mar: recentMar[0],
      },
      'Patient profile retrieved successfully'
    );
  })
);

module.exports = router;
