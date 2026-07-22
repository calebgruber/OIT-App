function calculateFinalExpiry(drawTime, shelfLifeHours, sourceExpiration) {
  const drawDate = new Date(drawTime);
  const shelfExpiry = new Date(drawDate.getTime() + Number(shelfLifeHours) * 60 * 60 * 1000);
  const sourceExpiry = new Date(sourceExpiration);

  return shelfExpiry <= sourceExpiry ? shelfExpiry : sourceExpiry;
}

module.exports = { calculateFinalExpiry };
