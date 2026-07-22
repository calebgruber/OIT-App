const express = require('express');
const { param, query } = require('express-validator');

const db = require('../config/db');
const auth = require('../middleware/auth');
const auditLog = require('../middleware/auditLog');
const { asyncHandler, handleValidationErrors, successResponse, parseBoolean } = require('../utils/apiHelpers');

const router = express.Router();

router.use(auth);

router.get(
  '/',
  [query('patient_id').optional().isInt({ min: 1 }), query('unread').optional().isBoolean()],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const filters = [];
    const params = [];

    if (req.query.patient_id) {
      filters.push('patient_id = ?');
      params.push(Number(req.query.patient_id));
    }

    if (req.query.unread !== undefined) {
      filters.push('is_read = ?');
      params.push(parseBoolean(req.query.unread) ? 0 : 1);
    }

    const [rows] = await db.execute(
      `SELECT * FROM notifications ${filters.length ? `WHERE ${filters.join(' AND ')}` : ''} ORDER BY created_at DESC`,
      params
    );

    return successResponse(res, rows, 'Notifications retrieved successfully');
  })
);

router.post(
  '/:id/read',
  auditLog('mark_notification_read', 'notification'),
  [param('id').isInt({ min: 1 })],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const notificationId = Number(req.params.id);
    const [rows] = await db.execute('SELECT * FROM notifications WHERE id = ? LIMIT 1', [notificationId]);
    const notification = rows[0];

    if (!notification) {
      return res.status(404).json({ error: true, message: 'Notification not found', details: [] });
    }

    await db.execute('UPDATE notifications SET is_read = true WHERE id = ?', [notificationId]);
    await req.audit({ patientId: notification.patient_id, entityId: notificationId, details: { previous_is_read: notification.is_read } });

    return successResponse(res, { id: notificationId, is_read: true }, 'Notification marked as read');
  })
);

module.exports = router;
