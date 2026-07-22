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
  [query('patient_id').optional().isInt({ min: 1 }), query('date').optional().isISO8601()],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const filters = [];
    const params = [];

    if (req.query.patient_id) {
      filters.push('patient_id = ?');
      params.push(Number(req.query.patient_id));
    }

    if (req.query.date) {
      filters.push('DATE(logged_at) = ?');
      params.push(req.query.date.slice(0, 10));
    }

    const [rows] = await db.execute(
      `SELECT * FROM food_logs ${filters.length ? `WHERE ${filters.join(' AND ')}` : ''} ORDER BY logged_at DESC`,
      params
    );

    return successResponse(res, rows, 'Food logs retrieved successfully');
  })
);

router.post(
  '/',
  auditLog('create_food_log', 'food_log'),
  [
    body('patient_id').isInt({ min: 1 }),
    body('food_description').trim().notEmpty(),
    body('logged_at').isISO8601(),
  ],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const { patient_id, food_description, logged_at } = req.body;
    const [patientRows] = await db.execute('SELECT id FROM patients WHERE id = ? LIMIT 1', [patient_id]);
    if (!patientRows.length) {
      return res.status(404).json({ error: true, message: 'Patient not found', details: [] });
    }

    const loggedAtDate = new Date(logged_at);
    const [doseRows] = await db.execute(
      `
        SELECT scheduled_date
        FROM dose_plans
        WHERE patient_id = ? AND status = 'scheduled' AND scheduled_date >= DATE(?)
        ORDER BY scheduled_date ASC
        LIMIT 1
      `,
      [patient_id, formatDateTime(loggedAtDate)]
    );

    let timingOk = true;
    if (doseRows.length) {
      const scheduledDoseReference = new Date(`${doseRows[0].scheduled_date}T12:00:00Z`);
      const cutoff = new Date(scheduledDoseReference.getTime() - 2 * 60 * 60 * 1000);
      timingOk = loggedAtDate <= cutoff;
    }

    if (!timingOk) {
      return res.status(400).json({
        error: true,
        message: 'Food must be logged at least 2 hours before the scheduled dose',
        details: [],
      });
    }

    const [result] = await db.execute(
      'INSERT INTO food_logs (patient_id, logged_at, food_description, timing_ok) VALUES (?, ?, ?, ?)',
      [patient_id, formatDateTime(loggedAtDate), food_description, timingOk]
    );

    await req.audit({ patientId: patient_id, entityId: result.insertId, details: { logged_at } });
    const [rows] = await db.execute('SELECT * FROM food_logs WHERE id = ? LIMIT 1', [result.insertId]);
    return successResponse(res, rows[0], 'Food log created successfully', 201);
  })
);

module.exports = router;
