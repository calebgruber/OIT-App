const express = require('express');
const { body } = require('express-validator');

const db = require('../config/db');
const auth = require('../middleware/auth');
const auditLog = require('../middleware/auditLog');
const { calculateFinalExpiry } = require('../utils/expiryCalculator');
const {
  asyncHandler,
  handleValidationErrors,
  successResponse,
  formatDate,
  formatDateTime,
  generateBarcode,
} = require('../utils/apiHelpers');

const router = express.Router();

router.use(auth);

router.post(
  '/',
  auditLog('pull_dose', 'syringe'),
  [
    body('patient_id').isInt({ min: 1 }),
    body('dose_mcg').isFloat({ min: 0 }),
    body('dose_form').isIn(['milk', 'powder']),
    body('carton_id_or_powder_supply_id').isInt({ min: 1 }),
    body('shelf_life_hours').isInt({ min: 1, max: 168 }),
    body('seal_type').trim().notEmpty().isLength({ max: 100 }),
    body('count').isInt({ min: 1, max: 50 }),
  ],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const {
      patient_id,
      dose_mcg,
      dose_form,
      carton_id_or_powder_supply_id,
      shelf_life_hours,
      seal_type,
      count,
    } = req.body;
    const connection = await db.getConnection();

    try {
      await connection.beginTransaction();

      const [patientRows] = await connection.execute('SELECT * FROM patients WHERE id = ? LIMIT 1 FOR UPDATE', [patient_id]);
      const patient = patientRows[0];
      if (!patient) {
        await connection.rollback();
        return res.status(404).json({ error: true, message: 'Patient not found', details: [] });
      }

      const sourceTable = dose_form === 'milk' ? 'cartons' : 'powder_supplies';
      const sourceKey = dose_form === 'milk' ? 'carton_id' : 'powder_supply_id';
      const [sourceRows] = await connection.execute(
        `SELECT * FROM ${sourceTable} WHERE id = ? AND patient_id = ? LIMIT 1 FOR UPDATE`,
        [carton_id_or_powder_supply_id, patient_id]
      );
      const source = sourceRows[0];

      if (!source) {
        await connection.rollback();
        return res.status(404).json({ error: true, message: `${dose_form === 'milk' ? 'Carton' : 'Powder supply'} not found`, details: [] });
      }

      if (source.status !== 'active') {
        await connection.rollback();
        return res.status(400).json({ error: true, message: 'Source supply is not active', details: [] });
      }

      const drawTime = new Date();
      const finalExpiry = calculateFinalExpiry(drawTime, shelf_life_hours, source.expiration_date);
      if (finalExpiry <= drawTime) {
        await connection.execute(`UPDATE ${sourceTable} SET status = 'expired' WHERE id = ?`, [source.id]);
        await req.audit(
          {
            patientId: patient_id,
            entityId: source.id,
            details: { source_table: sourceTable, source_status: 'expired', reason: 'expired_before_pull' },
          },
          connection
        );
        await connection.commit();
        return res.status(400).json({ error: true, message: 'Source supply has expired', details: [] });
      }

      const [dosePlanRows] = await connection.execute(
        `
          SELECT id, scheduled_date
          FROM dose_plans
          WHERE patient_id = ? AND status = 'scheduled' AND scheduled_date >= CURDATE()
          ORDER BY scheduled_date ASC
          LIMIT ?
        `,
        [patient_id, Number(count)]
      );

      const syringeIds = [];
      for (let index = 0; index < Number(count); index += 1) {
        const barcode = generateBarcode('SYR', patient_id);
        const scheduledDoseDate = dosePlanRows[index] ? dosePlanRows[index].scheduled_date : null;
        const calculatedExpiry = new Date(new Date(drawTime).getTime() + Number(shelf_life_hours) * 60 * 60 * 1000);
        const [insertResult] = await connection.execute(
          `
            INSERT INTO syringes
            (patient_id, carton_id, powder_supply_id, dose_mcg, dose_form, barcode, date_drawn, scheduled_dose_date, shelf_life_hours, calculated_expiry, final_expiry, seal_type, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ready')
          `,
          [
            patient_id,
            dose_form === 'milk' ? carton_id_or_powder_supply_id : null,
            dose_form === 'powder' ? carton_id_or_powder_supply_id : null,
            dose_mcg,
            dose_form,
            barcode,
            formatDateTime(drawTime),
            scheduledDoseDate ? formatDate(scheduledDoseDate) : null,
            shelf_life_hours,
            formatDateTime(calculatedExpiry),
            formatDateTime(finalExpiry),
            seal_type,
          ]
        );
        syringeIds.push(insertResult.insertId);
      }

      const [createdRows] = await connection.execute(
        `
          SELECT * FROM syringes
          WHERE id IN (${syringeIds.map(() => '?').join(', ')})
          ORDER BY created_at ASC
        `,
        syringeIds
      );

      await req.audit(
        {
          patientId: patient_id,
          entityId: syringeIds[0],
          details: {
            count,
            source_table: sourceTable,
            source_id: source.id,
            dose_mcg,
            dose_form,
          },
        },
        connection
      );

      await connection.commit();
      return successResponse(res, createdRows, 'Syringes created successfully', 201);
    } catch (error) {
      await connection.rollback();
      throw error;
    } finally {
      connection.release();
    }
  })
);

module.exports = router;
