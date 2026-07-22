const express = require('express');
const { body, param, query } = require('express-validator');

const db = require('../config/db');
const auth = require('../middleware/auth');
const auditLog = require('../middleware/auditLog');
const { asyncHandler, handleValidationErrors, successResponse } = require('../utils/apiHelpers');

const router = express.Router();

router.use(auth);

router.get(
  '/',
  [query('patient_id').optional().isInt({ min: 1 }), query('status').optional().isIn(['ready', 'used', 'expired', 'discarded'])],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const filters = [];
    const params = [];

    if (req.query.patient_id) {
      filters.push('syringes.patient_id = ?');
      params.push(Number(req.query.patient_id));
    }

    if (req.query.status) {
      filters.push('syringes.status = ?');
      params.push(req.query.status);
    }

    const [rows] = await db.execute(
      `
        SELECT syringes.*, patients.first_name, patients.last_name, patients.mrn
        FROM syringes
        INNER JOIN patients ON patients.id = syringes.patient_id
        ${filters.length ? `WHERE ${filters.join(' AND ')}` : ''}
        ORDER BY syringes.created_at DESC
      `,
      params
    );

    return successResponse(res, rows, 'Syringes retrieved successfully');
  })
);

router.get(
  '/barcode/:barcode',
  [param('barcode').trim().notEmpty()],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const [rows] = await db.execute(
      `
        SELECT syringes.*, patients.first_name, patients.last_name
        FROM syringes
        INNER JOIN patients ON patients.id = syringes.patient_id
        WHERE syringes.barcode = ?
        LIMIT 1
      `,
      [req.params.barcode]
    );

    if (!rows.length) {
      return res.status(404).json({ error: true, message: 'Syringe not found', details: [] });
    }

    return successResponse(res, rows[0], 'Syringe retrieved successfully');
  })
);

router.get(
  '/:id',
  [param('id').isInt({ min: 1 })],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const [rows] = await db.execute(
      `
        SELECT syringes.*, patients.first_name, patients.last_name, patients.mrn
        FROM syringes
        INNER JOIN patients ON patients.id = syringes.patient_id
        WHERE syringes.id = ?
        LIMIT 1
      `,
      [Number(req.params.id)]
    );

    if (!rows.length) {
      return res.status(404).json({ error: true, message: 'Syringe not found', details: [] });
    }

    return successResponse(res, rows[0], 'Syringe retrieved successfully');
  })
);

router.post(
  '/:id/discard',
  auditLog('discard_syringe', 'syringe'),
  [param('id').isInt({ min: 1 }), body('notes').optional().isString()],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const syringeId = Number(req.params.id);
    const [rows] = await db.execute('SELECT * FROM syringes WHERE id = ? LIMIT 1', [syringeId]);
    const syringe = rows[0];

    if (!syringe) {
      return res.status(404).json({ error: true, message: 'Syringe not found', details: [] });
    }

    if (syringe.status === 'used') {
      return res.status(400).json({ error: true, message: 'Used syringe cannot be discarded', details: [] });
    }

    await db.execute('UPDATE syringes SET status = ? WHERE id = ?', ['discarded', syringeId]);
    await req.audit({
      patientId: syringe.patient_id,
      entityId: syringeId,
      details: { previous_status: syringe.status, notes: req.body.notes || null },
    });

    return successResponse(res, { id: syringeId, status: 'discarded' }, 'Syringe discarded successfully');
  })
);

module.exports = router;
