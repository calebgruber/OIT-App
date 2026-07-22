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

    const [rows] = await db.execute(`SELECT * FROM cartons ${whereClause} ORDER BY expiration_date ASC`, params);
    return successResponse(res, rows, 'Cartons retrieved successfully');
  })
);

router.post(
  '/',
  auditLog('create_carton', 'carton'),
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
    const barcode = req.body.barcode || generateBarcode('CAR', patient_id);

    const [patientRows] = await db.execute('SELECT id FROM patients WHERE id = ? LIMIT 1', [patient_id]);
    if (!patientRows.length) {
      return res.status(404).json({ error: true, message: 'Patient not found', details: [] });
    }

    const [result] = await db.execute(
      `
        INSERT INTO cartons (patient_id, lot_number, expiration_date, date_opened, status, barcode, notes)
        VALUES (?, ?, ?, ?, 'active', ?, ?)
      `,
      [patient_id, lot_number, expiration_date, date_opened, barcode, notes]
    );

    await req.audit({ patientId: patient_id, entityId: result.insertId, details: { lot_number, barcode } });
    const [rows] = await db.execute('SELECT * FROM cartons WHERE id = ? LIMIT 1', [result.insertId]);
    return successResponse(res, rows[0], 'Carton created successfully', 201);
  })
);

router.get(
  '/barcode/:barcode',
  [param('barcode').trim().notEmpty()],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const [rows] = await db.execute('SELECT * FROM cartons WHERE barcode = ? LIMIT 1', [req.params.barcode]);
    if (!rows.length) {
      return res.status(404).json({ error: true, message: 'Carton not found', details: [] });
    }

    return successResponse(res, rows[0], 'Carton retrieved successfully');
  })
);

router.post(
  '/:id/discard',
  auditLog('discard_carton', 'carton'),
  [param('id').isInt({ min: 1 })],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const cartonId = Number(req.params.id);
    const [rows] = await db.execute('SELECT * FROM cartons WHERE id = ? LIMIT 1', [cartonId]);
    const carton = rows[0];

    if (!carton) {
      return res.status(404).json({ error: true, message: 'Carton not found', details: [] });
    }

    await db.execute('UPDATE cartons SET status = ? WHERE id = ?', ['discarded', cartonId]);
    await req.audit({ patientId: carton.patient_id, entityId: cartonId, details: { previous_status: carton.status } });

    return successResponse(res, { id: cartonId, status: 'discarded' }, 'Carton discarded successfully');
  })
);

module.exports = router;
