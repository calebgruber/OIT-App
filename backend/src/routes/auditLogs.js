const express = require('express');
const { query } = require('express-validator');

const db = require('../config/db');
const auth = require('../middleware/auth');
const { asyncHandler, handleValidationErrors, successResponse } = require('../utils/apiHelpers');

const router = express.Router();

router.use(auth);

router.get(
  '/',
  [
    query('patient_id').optional().isInt({ min: 1 }),
    query('entity_type').optional().trim().notEmpty(),
    query('action').optional().trim().notEmpty(),
    query('date_from').optional().isISO8601(),
    query('date_to').optional().isISO8601(),
  ],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const filters = [];
    const params = [];

    if (req.query.patient_id) {
      filters.push('audit_logs.patient_id = ?');
      params.push(Number(req.query.patient_id));
    }

    if (req.query.entity_type) {
      filters.push('audit_logs.entity_type = ?');
      params.push(req.query.entity_type);
    }

    if (req.query.action) {
      filters.push('audit_logs.action = ?');
      params.push(req.query.action);
    }

    if (req.query.date_from) {
      filters.push('DATE(audit_logs.created_at) >= ?');
      params.push(req.query.date_from.slice(0, 10));
    }

    if (req.query.date_to) {
      filters.push('DATE(audit_logs.created_at) <= ?');
      params.push(req.query.date_to.slice(0, 10));
    }

    const [rows] = await db.execute(
      `
        SELECT audit_logs.*, users.username, patients.first_name, patients.last_name
        FROM audit_logs
        LEFT JOIN users ON users.id = audit_logs.user_id
        LEFT JOIN patients ON patients.id = audit_logs.patient_id
        ${filters.length ? `WHERE ${filters.join(' AND ')}` : ''}
        ORDER BY audit_logs.created_at DESC
      `,
      params
    );

    return successResponse(res, rows, 'Audit logs retrieved successfully');
  })
);

module.exports = router;
