const express = require('express');
const { body } = require('express-validator');

const db = require('../config/db');
const auth = require('../middleware/auth');
const { asyncHandler, handleValidationErrors, successResponse } = require('../utils/apiHelpers');

const router = express.Router();
const lookupQueries = {
  patient: 'SELECT id, mrn AS barcode, first_name, last_name, date_of_birth FROM patients WHERE mrn = ? LIMIT 1',
  syringe: 'SELECT id, barcode, patient_id, status, final_expiry FROM syringes WHERE barcode = ? LIMIT 1',
  carton: 'SELECT id, barcode, patient_id, status, expiration_date FROM cartons WHERE barcode = ? LIMIT 1',
  powder_supply: 'SELECT id, barcode, patient_id, status, expiration_date FROM powder_supplies WHERE barcode = ? LIMIT 1',
};

router.use(auth);

router.post(
  '/validate',
  [body('barcode').trim().notEmpty(), body('expected_type').isIn(['patient', 'syringe', 'carton', 'powder_supply'])],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const { barcode, expected_type } = req.body;
    const [rows] = await db.execute(lookupQueries[expected_type], [barcode]);
    const record = rows[0] || null;

    return successResponse(
      res,
      { valid: Boolean(record), type: record ? expected_type : null, record },
      record ? 'Barcode validated successfully' : 'Barcode not found'
    );
  })
);

module.exports = router;
