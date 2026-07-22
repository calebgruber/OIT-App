const db = require('../config/db');

async function createAuditLogEntry(executor, entry) {
  const runner = executor && typeof executor.execute === 'function' ? executor : db;
  const sql = `
    INSERT INTO audit_logs (user_id, patient_id, action, entity_type, entity_id, details, ip_address)
    VALUES (?, ?, ?, ?, ?, ?, ?)
  `;

  await runner.execute(sql, [
    entry.userId || null,
    entry.patientId || null,
    entry.action,
    entry.entityType,
    entry.entityId !== undefined && entry.entityId !== null ? String(entry.entityId) : null,
    entry.details ? JSON.stringify(entry.details) : null,
    entry.ipAddress || null,
  ]);
}

function auditLog(action, entityType) {
  return (req, res, next) => {
    req.audit = async (details = {}, executor = db) =>
      createAuditLogEntry(executor, {
        action,
        entityType,
        userId: details.userId ?? req.user?.id ?? null,
        patientId: details.patientId ?? null,
        entityId: details.entityId ?? null,
        details: details.details ?? null,
        ipAddress: details.ipAddress ?? req.ip,
      });

    next();
  };
}

module.exports = auditLog;
module.exports.createAuditLogEntry = createAuditLogEntry;
