const crypto = require('crypto');
const { validationResult } = require('express-validator');

function asyncHandler(handler) {
  return (req, res, next) => Promise.resolve(handler(req, res, next)).catch(next);
}

function handleValidationErrors(req, res, next) {
  const errors = validationResult(req);

  if (errors.isEmpty()) {
    return next();
  }

  return res.status(400).json({
    error: true,
    message: 'Validation failed',
    details: errors.array(),
  });
}

function successResponse(res, data, message = 'Success', status = 200) {
  return res.status(status).json({ data, message });
}

function formatDate(value) {
  if (!value) {
    return null;
  }

  return new Date(value).toISOString().slice(0, 10);
}

function formatDateTime(value) {
  if (!value) {
    return null;
  }

  return new Date(value).toISOString().slice(0, 19).replace('T', ' ');
}

function parseBoolean(value) {
  if (typeof value === 'boolean') {
    return value;
  }

  if (typeof value === 'string') {
    return value.toLowerCase() === 'true';
  }

  return false;
}

function generateBarcode(prefix, patientId) {
  const timestamp = Date.now().toString(36).toUpperCase();
  const random = crypto.randomBytes(3).toString('hex').toUpperCase();
  return `${prefix}-${patientId}-${timestamp}-${random}`;
}

function createError(message, statusCode = 500, details = []) {
  const error = new Error(message);
  error.statusCode = statusCode;
  error.details = details;
  return error;
}

module.exports = {
  asyncHandler,
  handleValidationErrors,
  successResponse,
  formatDate,
  formatDateTime,
  parseBoolean,
  generateBarcode,
  createError,
};
