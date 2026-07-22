const express = require('express');
const { body, query } = require('express-validator');

const db = require('../config/db');
const auth = require('../middleware/auth');
const auditLog = require('../middleware/auditLog');
const {
  asyncHandler,
  handleValidationErrors,
  successResponse,
  formatDateTime,
} = require('../utils/apiHelpers');

const router = express.Router();

router.use(auth);

router.get(
  '/',
  [query('patient_id').optional().isInt({ min: 1 })],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const params = [];
    const whereClause = req.query.patient_id ? 'WHERE symptom_logs.patient_id = ?' : '';
    if (req.query.patient_id) {
      params.push(Number(req.query.patient_id));
    }

    const [rows] = await db.execute(
      `
        SELECT symptom_logs.*, patients.first_name, patients.last_name
        FROM symptom_logs
        INNER JOIN patients ON patients.id = symptom_logs.patient_id
        ${whereClause}
        ORDER BY symptom_logs.logged_at DESC
      `,
      params
    );

    return successResponse(res, rows, 'Symptom logs retrieved successfully');
  })
);

router.post(
  '/',
  auditLog('create_symptom_log', 'symptom_log'),
  [
    body('patient_id').isInt({ min: 1 }),
    body('mar_entry_id').optional({ nullable: true }).isInt({ min: 1 }),
    body('severity').isInt({ min: 1, max: 5 }),
    body('symptoms').custom((value) => typeof value === 'object' && value !== null),
    body('notes').optional().isString(),
    body('logged_at').isISO8601(),
  ],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const { patient_id, mar_entry_id = null, severity, symptoms, notes = null, logged_at } = req.body;

    const [patientRows] = await db.execute('SELECT id FROM patients WHERE id = ? LIMIT 1', [patient_id]);
    if (!patientRows.length) {
      return res.status(404).json({ error: true, message: 'Patient not found', details: [] });
    }

    if (mar_entry_id) {
      const [marRows] = await db.execute('SELECT id FROM mar_entries WHERE id = ? AND patient_id = ? LIMIT 1', [mar_entry_id, patient_id]);
      if (!marRows.length) {
        return res.status(400).json({ error: true, message: 'MAR entry does not belong to patient', details: [] });
      }
    }

    const [result] = await db.execute(
      `
        INSERT INTO symptom_logs (patient_id, mar_entry_id, logged_at, severity, symptoms, notes)
        VALUES (?, ?, ?, ?, ?, ?)
      `,
      [patient_id, mar_entry_id, formatDateTime(logged_at), severity, JSON.stringify(symptoms), notes]
    );

    await req.audit({ patientId: patient_id, entityId: result.insertId, details: { severity, mar_entry_id } });
    const [rows] = await db.execute('SELECT * FROM symptom_logs WHERE id = ? LIMIT 1', [result.insertId]);
    return successResponse(res, rows[0], 'Symptom log created successfully', 201);
  })
);

module.exports = router;
