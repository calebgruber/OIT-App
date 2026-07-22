const express = require('express');
const { param, query } = require('express-validator');

const db = require('../config/db');
const auth = require('../middleware/auth');
const auditLog = require('../middleware/auditLog');
const {
  asyncHandler,
  handleValidationErrors,
  successResponse,
  formatDate,
  formatDateTime,
} = require('../utils/apiHelpers');

const router = express.Router();

async function validateMissionRequirements(connection, mission) {
  const missionDate = formatDate(mission.mission_date);
  const patientId = mission.patient_id;

  if (mission.mission_type === 'log_food') {
    const [rows] = await connection.execute(
      'SELECT id FROM food_logs WHERE patient_id = ? AND DATE(logged_at) = ? LIMIT 1',
      [patientId, missionDate]
    );
    return rows.length > 0;
  }

  if (mission.mission_type === 'complete_dose') {
    const [rows] = await connection.execute(
      `
        SELECT id FROM mar_entries
        WHERE patient_id = ? AND DATE(administered_at) = ? AND outcome IN ('completed', 'reaction')
        LIMIT 1
      `,
      [patientId, missionDate]
    );
    return rows.length > 0;
  }

  if (mission.mission_type === 'record_symptoms') {
    const [rows] = await connection.execute(
      'SELECT id FROM symptom_logs WHERE patient_id = ? AND DATE(logged_at) = ? LIMIT 1',
      [patientId, missionDate]
    );
    return rows.length > 0;
  }

  const [foodRows, marRows, symptomRows] = await Promise.all([
    connection.execute('SELECT id FROM food_logs WHERE patient_id = ? AND DATE(logged_at) = ? LIMIT 1', [patientId, missionDate]),
    connection.execute(
      `SELECT id FROM mar_entries WHERE patient_id = ? AND DATE(administered_at) = ? AND outcome IN ('completed', 'reaction') LIMIT 1`,
      [patientId, missionDate]
    ),
    connection.execute('SELECT id FROM symptom_logs WHERE patient_id = ? AND DATE(logged_at) = ? LIMIT 1', [patientId, missionDate]),
  ]);

  return foodRows[0].length > 0 && marRows[0].length > 0 && symptomRows[0].length > 0;
}

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
      filters.push('mission_date = ?');
      params.push(req.query.date.slice(0, 10));
    }

    const [rows] = await db.execute(
      `SELECT * FROM missions ${filters.length ? `WHERE ${filters.join(' AND ')}` : ''} ORDER BY mission_date DESC, created_at DESC`,
      params
    );

    return successResponse(res, rows, 'Missions retrieved successfully');
  })
);

router.post(
  '/:id/complete',
  auditLog('complete_mission', 'mission'),
  [param('id').isInt({ min: 1 })],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const missionId = Number(req.params.id);
    const connection = await db.getConnection();

    try {
      await connection.beginTransaction();
      const [missionRows] = await connection.execute('SELECT * FROM missions WHERE id = ? LIMIT 1 FOR UPDATE', [missionId]);
      const mission = missionRows[0];

      if (!mission) {
        await connection.rollback();
        return res.status(404).json({ error: true, message: 'Mission not found', details: [] });
      }

      const requirementsMet = await validateMissionRequirements(connection, mission);
      if (!requirementsMet) {
        await connection.rollback();
        return res.status(400).json({ error: true, message: 'Mission requirements not met', details: [] });
      }

      const completedAt = new Date();
      await connection.execute(
        'UPDATE missions SET status = ?, completed_at = ? WHERE id = ?',
        ['completed', formatDateTime(completedAt), missionId]
      );

      const [pendingRows] = await connection.execute(
        'SELECT id FROM missions WHERE patient_id = ? AND mission_date = ? AND status != ? LIMIT 1',
        [mission.patient_id, formatDate(mission.mission_date), 'completed']
      );

      let badge = null;
      if (!pendingRows.length) {
        const [badgeRows] = await connection.execute(
          `
            SELECT * FROM badges
            WHERE patient_id = ? AND badge_type = 'daily_mission_complete'
              AND JSON_EXTRACT(metadata, '$.mission_date') = JSON_QUOTE(?)
            LIMIT 1
          `,
          [mission.patient_id, formatDate(mission.mission_date)]
        );

        if (!badgeRows.length) {
          const metadata = { mission_date: formatDate(mission.mission_date) };
          const [badgeResult] = await connection.execute(
            `
              INSERT INTO badges (patient_id, badge_type, earned_at, metadata)
              VALUES (?, 'daily_mission_complete', ?, ?)
            `,
            [mission.patient_id, formatDateTime(completedAt), JSON.stringify(metadata)]
          );
          badge = { id: badgeResult.insertId, badge_type: 'daily_mission_complete', metadata };
        }
      }

      await req.audit(
        {
          patientId: mission.patient_id,
          entityId: missionId,
          details: { mission_type: mission.mission_type, badge_awarded: badge?.badge_type || null },
        },
        connection
      );

      await connection.commit();
      return successResponse(
        res,
        { id: missionId, status: 'completed', completed_at: completedAt, badge },
        'Mission completed successfully'
      );
    } catch (error) {
      await connection.rollback();
      throw error;
    } finally {
      connection.release();
    }
  })
);

router.get(
  '/:patient_id/badges',
  [param('patient_id').isInt({ min: 1 })],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const [rows] = await db.execute('SELECT * FROM badges WHERE patient_id = ? ORDER BY earned_at DESC', [Number(req.params.patient_id)]);
    return successResponse(res, rows, 'Badges retrieved successfully');
  })
);

module.exports = router;
