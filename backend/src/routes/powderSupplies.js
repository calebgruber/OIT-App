const express = require('express');
const { body, param, query } = require('express-validator');

const db = require('../config/db');
const auth = require('../middleware/auth');
const auditLog = require('../middleware/auditLog');
const {
  asyncHandler,
  handleValidationErrors,
  successResponse,
  generateBarcode,
} = require('../utils/apiHelpers');

const router = express.Router();

router.use(auth);

router.get(
  '/',
  [query('patient_id').optional().isInt({ min: 1 })],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const params = [];
    const whereClause = req.query.patient_id ? 'WHERE patient_id = ?' : '';
    if (req.query.patient_id) {
      params.push(Number(req.query.patient_id));
    }

    const [rows] = await db.execute(`SELECT * FROM powder_supplies ${whereClause} ORDER BY expiration_date ASC`, params);
    return successResponse(res, rows, 'Powder supplies retrieved successfully');
  })
);

router.post(
  '/',
  auditLog('create_powder_supply', 'powder_supply'),
  [
    body('patient_id').isInt({ min: 1 }),
    body('lot_number').trim().notEmpty().isLength({ max: 150 }),
    body('expiration_date').isISO8601(),
    body('date_opened').optional({ nullable: true }).isISO8601(),
    body('barcode').optional().trim().isLength({ max: 150 }),
    body('notes').optional().isString(),
  ],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const { patient_id, lot_number, expiration_date, date_opened = null, notes = null } = req.body;
    const barcode = req.body.barcode || generateBarcode('PWD', patient_id);

    const [patientRows] = await db.execute('SELECT id FROM patients WHERE id = ? LIMIT 1', [patient_id]);
    if (!patientRows.length) {
      return res.status(404).json({ error: true, message: 'Patient not found', details: [] });
    }

    const [result] = await db.execute(
      `
        INSERT INTO powder_supplies (patient_id, lot_number, expiration_date, date_opened, status, barcode, notes)
        VALUES (?, ?, ?, ?, 'active', ?, ?)
      `,
      [patient_id, lot_number, expiration_date, date_opened, barcode, notes]
    );

    await req.audit({ patientId: patient_id, entityId: result.insertId, details: { lot_number, barcode } });
    const [rows] = await db.execute('SELECT * FROM powder_supplies WHERE id = ? LIMIT 1', [result.insertId]);
    return successResponse(res, rows[0], 'Powder supply created successfully', 201);
  })
);

router.get(
  '/barcode/:barcode',
  [param('barcode').trim().notEmpty()],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const [rows] = await db.execute('SELECT * FROM powder_supplies WHERE barcode = ? LIMIT 1', [req.params.barcode]);
    if (!rows.length) {
      return res.status(404).json({ error: true, message: 'Powder supply not found', details: [] });
    }

    return successResponse(res, rows[0], 'Powder supply retrieved successfully');
  })
);

router.post(
  '/:id/discard',
  auditLog('discard_powder_supply', 'powder_supply'),
  [param('id').isInt({ min: 1 })],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const powderSupplyId = Number(req.params.id);
    const [rows] = await db.execute('SELECT * FROM powder_supplies WHERE id = ? LIMIT 1', [powderSupplyId]);
    const powderSupply = rows[0];

    if (!powderSupply) {
      return res.status(404).json({ error: true, message: 'Powder supply not found', details: [] });
    }

    await db.execute('UPDATE powder_supplies SET status = ? WHERE id = ?', ['discarded', powderSupplyId]);
    await req.audit({ patientId: powderSupply.patient_id, entityId: powderSupplyId, details: { previous_status: powderSupply.status } });

    return successResponse(res, { id: powderSupplyId, status: 'discarded' }, 'Powder supply discarded successfully');
  })
);

module.exports = router;
