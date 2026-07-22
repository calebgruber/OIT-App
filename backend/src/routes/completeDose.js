const express = require('express');
const { body } = require('express-validator');

const db = require('../config/db');
const auth = require('../middleware/auth');
const auditLog = require('../middleware/auditLog');
const { asyncHandler, handleValidationErrors, successResponse, formatDateTime } = require('../utils/apiHelpers');

const router = express.Router();

router.use(auth);

router.post(
  '/',
  auditLog('complete_dose', 'mar_entry'),
  [
    body('patient_id').isInt({ min: 1 }),
    body('syringe_id').isInt({ min: 1 }),
    body('outcome').isIn(['completed', 'reaction', 'missed']),
    body('notes').optional().isString(),
  ],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const { patient_id, syringe_id, outcome, notes = null } = req.body;
    const connection = await db.getConnection();

    try {
      await connection.beginTransaction();

      const [patientRows] = await connection.execute('SELECT id FROM patients WHERE id = ? LIMIT 1 FOR UPDATE', [patient_id]);
      if (!patientRows.length) {
        await connection.rollback();
        return res.status(404).json({ error: true, message: 'Patient not found', details: [] });
      }

      const [syringeRows] = await connection.execute(
        'SELECT * FROM syringes WHERE id = ? AND patient_id = ? LIMIT 1 FOR UPDATE',
        [syringe_id, patient_id]
      );
      const syringe = syringeRows[0];

      if (!syringe) {
        await connection.rollback();
        return res.status(404).json({ error: true, message: 'Syringe not found for patient', details: [] });
      }

      if (syringe.status === 'used') {
        await connection.rollback();
        return res.status(400).json({ error: true, message: 'Syringe has already been used', details: [] });
      }

      if (syringe.status === 'discarded') {
        await connection.rollback();
        return res.status(400).json({ error: true, message: 'Syringe has been discarded', details: [] });
      }

      if (syringe.status === 'expired') {
        await connection.rollback();
        return res.status(400).json({ error: true, message: 'Syringe has expired', details: [] });
      }

      const now = new Date();
      if (new Date(syringe.final_expiry) <= now) {
        await connection.execute('UPDATE syringes SET status = ? WHERE id = ?', ['expired', syringe_id]);
        await req.audit(
          {
            patientId: patient_id,
            entityId: syringe_id,
            details: { previous_status: syringe.status, new_status: 'expired', reason: 'expired_before_administration' },
          },
          connection
        );
        await connection.commit();
        return res.status(400).json({ error: true, message: 'Syringe has expired', details: [] });
      }

      const administeredAt = formatDateTime(now);
      const [marResult] = await connection.execute(
        `
          INSERT INTO mar_entries
          (patient_id, syringe_id, dose_mcg, dose_form, administered_at, administered_by_user_id, outcome, notes)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        `,
        [patient_id, syringe_id, syringe.dose_mcg, syringe.dose_form, administeredAt, req.user.id, outcome, notes]
      );

      await connection.execute('UPDATE syringes SET status = ? WHERE id = ?', ['used', syringe_id]);
      await connection.execute(
        `
          UPDATE dose_plans
          SET status = ?
          WHERE id = (
            SELECT target.id FROM (
              SELECT id
              FROM dose_plans
              WHERE patient_id = ? AND status = 'scheduled'
              ORDER BY ABS(DATEDIFF(scheduled_date, CURDATE())), scheduled_date ASC
              LIMIT 1
            ) AS target
          )
        `,
        [outcome === 'missed' ? 'skipped' : 'completed', patient_id]
      );

      if (outcome === 'reaction') {
        await connection.execute(
          `
            INSERT INTO notifications (patient_id, type, title, message, is_read)
            VALUES (?, 'dose_reaction', 'Dose reaction recorded', ?, false)
          `,
          [patient_id, `Reaction recorded for syringe ${syringe.barcode}`]
        );
      }

      await req.audit(
        {
          patientId: patient_id,
          entityId: marResult.insertId,
          details: { syringe_id, outcome, dose_mcg: syringe.dose_mcg },
        },
        connection
      );

      await connection.commit();

      const [marRows] = await db.execute('SELECT * FROM mar_entries WHERE id = ? LIMIT 1', [marResult.insertId]);
      return successResponse(res, marRows[0], 'Dose completed successfully', 201);
    } catch (error) {
      await connection.rollback();
      throw error;
    } finally {
      connection.release();
    }
  })
);

module.exports = router;
