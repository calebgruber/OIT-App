function formatDate(date) {
  return new Date(date).toISOString().slice(0, 10);
}

function calculateNextDoseDates(startDate, intervalDays = 14, count = 52) {
  const dates = [];
  const baseDate = new Date(startDate);

  for (let index = 0; index < count; index += 1) {
    const nextDate = new Date(baseDate);
    nextDate.setUTCDate(nextDate.getUTCDate() + intervalDays * index);
    dates.push(formatDate(nextDate));
  }

  return dates;
}

async function recalculateScheduleFromDecision(patientId, decision, db) {
  const decisionDate = decision.decisionDate ? new Date(decision.decisionDate) : new Date();
  const startDate = decision.startDate ? new Date(decision.startDate) : decisionDate;
  const intervalDays = Number(decision.intervalDays || 14);
  const count = Number(decision.count || 52);
  const doseMcg = Number(decision.newDoseMcg);
  const doseForm = decision.doseForm || 'milk';
  const scheduleDates = calculateNextDoseDates(startDate, intervalDays, count);

  await db.execute(
    'DELETE FROM dose_plans WHERE patient_id = ? AND scheduled_date >= ?',
    [patientId, formatDate(decisionDate)]
  );

  if (scheduleDates.length === 0) {
    return [];
  }

  const placeholders = scheduleDates.map(() => '(?, ?, ?, ?, ?)').join(', ');
  const values = scheduleDates.flatMap((scheduledDate) => [patientId, scheduledDate, doseMcg, doseForm, 'scheduled']);

  await db.execute(
    `INSERT INTO dose_plans (patient_id, scheduled_date, dose_mcg, dose_form, status) VALUES ${placeholders}`,
    values
  );

  return scheduleDates;
}

module.exports = { calculateNextDoseDates, recalculateScheduleFromDecision };
