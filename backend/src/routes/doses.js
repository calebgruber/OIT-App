const express = require('express');
const { param, query } = require('express-validator');

const db = require('../config/db');
const auth = require('../middleware/auth');
const { asyncHandler, handleValidationErrors, successResponse } = require('../utils/apiHelpers');

const router = express.Router();

router.use(auth);

router.get(
  '/',
  [
    query('patient_id').optional().isInt({ min: 1 }),
    query('status').optional().isIn(['scheduled', 'completed', 'skipped']),
    query('date_from').optional().isISO8601(),
    query('date_to').optional().isISO8601(),
  ],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const filters = [];
    const params = [];

    if (req.query.patient_id) {
      filters.push('patient_id = ?');
      params.push(Number(req.query.patient_id));
    }

    if (req.query.status) {
      filters.push('status = ?');
      params.push(req.query.status);
    }

    if (req.query.date_from) {
      filters.push('scheduled_date >= ?');
      params.push(req.query.date_from);
    }

    if (req.query.date_to) {
      filters.push('scheduled_date <= ?');
      params.push(req.query.date_to);
    }

    const sql = `
      SELECT dose_plans.*, patients.first_name, patients.last_name, patients.mrn
      FROM dose_plans
      INNER JOIN patients ON patients.id = dose_plans.patient_id
      ${filters.length ? `WHERE ${filters.join(' AND ')}` : ''}
      ORDER BY scheduled_date ASC
    `;

    const [rows] = await db.execute(sql, params);
    return successResponse(res, rows, 'Dose plans retrieved successfully');
  })
);

router.get(
  '/:id',
  [param('id').isInt({ min: 1 })],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const [rows] = await db.execute(
      `
        SELECT dose_plans.*, patients.first_name, patients.last_name, patients.mrn
        FROM dose_plans
        INNER JOIN patients ON patients.id = dose_plans.patient_id
        WHERE dose_plans.id = ?
        LIMIT 1
      `,
      [Number(req.params.id)]
    );

    if (!rows.length) {
      return res.status(404).json({ error: true, message: 'Dose plan not found', details: [] });
    }

    return successResponse(res, rows[0], 'Dose plan retrieved successfully');
  })
);

module.exports = router;
