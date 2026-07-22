require('dotenv').config();

const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');

const authRoutes = require('./src/routes/auth');
const patientRoutes = require('./src/routes/patients');
const doseRoutes = require('./src/routes/doses');
const pullDoseRoutes = require('./src/routes/pullDose');
const completeDoseRoutes = require('./src/routes/completeDose');
const syringeRoutes = require('./src/routes/syringes');
const cartonRoutes = require('./src/routes/cartons');
const powderSupplyRoutes = require('./src/routes/powderSupplies');
const foodLogRoutes = require('./src/routes/foodLogs');
const symptomLogRoutes = require('./src/routes/symptomLogs');
const marRoutes = require('./src/routes/mar');
const barcodeRoutes = require('./src/routes/barcodes');
const printJobRoutes = require('./src/routes/printJobs');
const notificationRoutes = require('./src/routes/notifications');
const auditLogRoutes = require('./src/routes/auditLogs');
const missionRoutes = require('./src/routes/missions');

const app = express();
const port = Number(process.env.PORT) || 3000;
const corsOrigin = process.env.CORS_ORIGIN || '*';

app.use(
  cors({
    origin: corsOrigin === '*' ? true : corsOrigin.split(',').map((value) => value.trim()),
    credentials: true,
  })
);
app.use(helmet());
app.use(morgan(process.env.NODE_ENV === 'production' ? 'combined' : 'dev'));
app.use(express.json({ limit: '1mb' }));

app.get('/health', (req, res) => {
  res.status(200).json({ data: { status: 'ok' }, message: 'Service healthy' });
});

app.use('/api/v1/auth', authRoutes);
app.use('/api/v1/patients', patientRoutes);
app.use('/api/v1/doses', doseRoutes);
app.use('/api/v1/pull-dose', pullDoseRoutes);
app.use('/api/v1/complete-dose', completeDoseRoutes);
app.use('/api/v1/syringes', syringeRoutes);
app.use('/api/v1/cartons', cartonRoutes);
app.use('/api/v1/powder-supplies', powderSupplyRoutes);
app.use('/api/v1/food-logs', foodLogRoutes);
app.use('/api/v1/symptom-logs', symptomLogRoutes);
app.use('/api/v1/mar', marRoutes);
app.use('/api/v1/barcodes', barcodeRoutes);
app.use('/api/v1/print-jobs', printJobRoutes);
app.use('/api/v1/notifications', notificationRoutes);
app.use('/api/v1/audit-logs', auditLogRoutes);
app.use('/api/v1/missions', missionRoutes);

app.use((req, res) => {
  res.status(404).json({ error: true, message: 'Route not found', details: [] });
});

app.use((err, req, res, next) => {
  const statusCode = err.statusCode || 500;
  const payload = {
    error: true,
    message: err.message || 'Internal server error',
    details: err.details || [],
  };

  if (process.env.NODE_ENV !== 'production' && err.stack) {
    payload.details = [...payload.details, err.stack];
  }

  if (statusCode >= 500) {
    console.error(err);
  }

  res.status(statusCode).json(payload);
});

app.listen(port, () => {
  console.log(`OIT backend listening on port ${port}`);
});
