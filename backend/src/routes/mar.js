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
    query('date_from').optional().isISO8601(),
    query('date_to').optional().isISO8601(),
  ],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const filters = [];
    const params = [];

    if (req.query.patient_id) {
      filters.push('mar_entries.patient_id = ?');
      params.push(Number(req.query.patient_id));
    }

    if (req.query.date_from) {
      filters.push('DATE(mar_entries.administered_at) >= ?');
      params.push(req.query.date_from.slice(0, 10));
    }

    if (req.query.date_to) {
      filters.push('DATE(mar_entries.administered_at) <= ?');
      params.push(req.query.date_to.slice(0, 10));
    }

    const [rows] = await db.execute(
      `
        SELECT mar_entries.*, users.username AS administered_by_username, patients.first_name, patients.last_name, patients.mrn
        FROM mar_entries
        INNER JOIN users ON users.id = mar_entries.administered_by_user_id
        INNER JOIN patients ON patients.id = mar_entries.patient_id
        ${filters.length ? `WHERE ${filters.join(' AND ')}` : ''}
        ORDER BY mar_entries.administered_at DESC
      `,
      params
    );

    return successResponse(res, rows, 'MAR entries retrieved successfully');
  })
);

router.get(
  '/:id',
  [param('id').isInt({ min: 1 })],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const [rows] = await db.execute(
      `
        SELECT mar_entries.*, users.username AS administered_by_username, patients.first_name, patients.last_name, patients.mrn
        FROM mar_entries
        INNER JOIN users ON users.id = mar_entries.administered_by_user_id
        INNER JOIN patients ON patients.id = mar_entries.patient_id
        WHERE mar_entries.id = ?
        LIMIT 1
      `,
      [Number(req.params.id)]
    );

    if (!rows.length) {
      return res.status(404).json({ error: true, message: 'MAR entry not found', details: [] });
    }

    return successResponse(res, rows[0], 'MAR entry retrieved successfully');
  })
);

module.exports = router;
