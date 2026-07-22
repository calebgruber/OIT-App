const jwt = require('jsonwebtoken');

function auth(req, res, next) {
  const authorization = req.headers.authorization || '';

  if (!authorization.startsWith('Bearer ')) {
    return res.status(401).json({ error: true, message: 'Authorization token required', details: [] });
  }

  const token = authorization.slice(7).trim();

  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    req.user = decoded;
    return next();
  } catch (error) {
    return res.status(401).json({ error: true, message: 'Invalid or expired token', details: [] });
  }
}

function requireRole(...roles) {
  return (req, res, next) => {
    if (!req.user) {
      return res.status(401).json({ error: true, message: 'Authentication required', details: [] });
    }

    if (!roles.includes(req.user.role)) {
      return res.status(403).json({ error: true, message: 'Insufficient permissions', details: [] });
    }

    return next();
  };
}

module.exports = auth;
module.exports.requireRole = requireRole;
