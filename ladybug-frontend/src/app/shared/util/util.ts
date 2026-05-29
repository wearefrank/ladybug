export function isNumber(s: string | null): boolean {
  return s !== null && !Number.isNaN(Number(s)) && s.trim() !== '';
}
