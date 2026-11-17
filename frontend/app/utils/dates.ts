export function formatDayDate(
  startDate: string | undefined,
  dayNumber: number,
): string | undefined {
  const match = startDate?.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (!match) return undefined;
  const [, year, month, day] = match.map(Number);
  const date = new Date(year, month - 1, day + dayNumber - 1);
  if (Number.isNaN(date.getTime())) return undefined;
  return date.toLocaleDateString("en-GB", {
    weekday: "short",
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}
