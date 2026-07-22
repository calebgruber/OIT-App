const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { body } = require('express-validator');

const db = require('../config/db');
const auth = require('../middleware/auth');
const auditLog = require('../middleware/auditLog');
const { asyncHandler, handleValidationErrors, successResponse } = require('../utils/apiHelpers');

const router = express.Router();

router.post(
  '/login',
  auditLog('login', 'user'),
  [body('username').trim().notEmpty(), body('password').notEmpty()],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const { username, password } = req.body;
    const [rows] = await db.execute(
      'SELECT id, username, email, password_hash, role FROM users WHERE username = ? LIMIT 1',
      [username]
    );

    const user = rows[0];
    if (!user || !(await bcrypt.compare(password, user.password_hash))) {
      return res.status(401).json({ error: true, message: 'Invalid username or password', details: [] });
    }

    const token = jwt.sign(
      { id: user.id, username: user.username, email: user.email, role: user.role },
      process.env.JWT_SECRET,
      { expiresIn: process.env.JWT_EXPIRES_IN || '24h' }
    );

    await req.audit({ userId: user.id, entityId: user.id, details: { username: user.username } });

    return successResponse(
      res,
      {
        token,
        user: {
          id: user.id,
          username: user.username,
          email: user.email,
          role: user.role,
        },
      },
      'Login successful'
    );
  })
);

router.post(
  '/register',
  auth,
  auditLog('register_user', 'user'),
  auth.requireRole('admin'),
  [
    body('username').trim().isLength({ min: 3, max: 100 }),
    body('email').isEmail().normalizeEmail(),
    body('password').isLength({ min: 8 }),
    body('role').optional().isIn(['admin', 'clinician']),
  ],
  handleValidationErrors,
  asyncHandler(async (req, res) => {
    const { username, email, password, role = 'clinician' } = req.body;

    const [existing] = await db.execute('SELECT id FROM users WHERE username = ? OR email = ? LIMIT 1', [username, email]);
    if (existing.length) {
      return res.status(400).json({ error: true, message: 'Username or email already exists', details: [] });
    }

    const passwordHash = await bcrypt.hash(password, 10);
    const [result] = await db.execute(
      'INSERT INTO users (username, email, password_hash, role) VALUES (?, ?, ?, ?)',
      [username, email, passwordHash, role]
    );

    await req.audit({
      entityId: result.insertId,
      details: { username, email, role, createdBy: req.user.id },
    });

    return successResponse(
      res,
      { id: result.insertId, username, email, role },
      'Clinician created successfully',
      201
    );
  })
);

module.exports = router;
