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
  auditLog('create_print_job', 'print_job'),
  [
    body('syringe_id').isInt({ min: 1 }),
    body('printer_id').trim().notEmpty().isLength({ max: 100 }),
    body('printer_type').trim().notEmpty().isLength({ max: 100 }),
  ],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const { syringe_id, printer_id, printer_type } = req.body;
    const [syringeRows] = await db.execute(
      `
        SELECT syringes.*, patients.first_name, patients.last_name, patients.mrn
        FROM syringes
        INNER JOIN patients ON patients.id = syringes.patient_id
        WHERE syringes.id = ?
        LIMIT 1
      `,
      [syringe_id]
    );
    const syringe = syringeRows[0];

    if (!syringe) {
      return res.status(404).json({ error: true, message: 'Syringe not found', details: [] });
    }

    const printedAt = new Date();
    const [result] = await db.execute(
      `
        INSERT INTO print_jobs (syringe_id, printed_at, printer_id, printer_type, status)
        VALUES (?, ?, ?, ?, 'success')
      `,
      [syringe_id, formatDateTime(printedAt), printer_id, printer_type]
    );

    await req.audit({ patientId: syringe.patient_id, entityId: result.insertId, details: { syringe_id, printer_id, printer_type } });

    return successResponse(
      res,
      {
        id: result.insertId,
        syringe_id,
        printed_at: printedAt,
        printer_id,
        printer_type,
        status: 'success',
        label: {
          patient_id: syringe.patient_id,
          patient_name: `${syringe.first_name} ${syringe.last_name}`,
          mrn: syringe.mrn,
          barcode: syringe.barcode,
          dose_mcg: syringe.dose_mcg,
          dose_form: syringe.dose_form,
          final_expiry: syringe.final_expiry,
          scheduled_dose_date: syringe.scheduled_dose_date,
        },
      },
      'Print job created successfully',
      201
    );
  })
);

module.exports = router;
