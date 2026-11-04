/** Brazilian mobile number as the form shows it: `(11) 9 1234-5678`. */
export function maskPhone(typed: string): string {
  const d = typed.replace(/\D/g, '').slice(0, 11);
  if (d.length === 0) return '';
  if (d.length <= 2) return `(${d}`;
  let masked = `(${d.slice(0, 2)}) ${d[2]}`;
  if (d.length > 3) masked += ` ${d.slice(3, 7)}`;
  if (d.length > 7) masked += `-${d.slice(7)}`;
  return masked;
}

export interface PhoneParts {
  readonly areaCode: number;
  readonly phoneNumber: number;
}

/**
 * Splits a masked (or plain) number into the `areaCode` and `phoneNumber` the API wants, or null when it is not a mobile
 * number. The rule is the backend's (`^[1-9]{2}9[0-9]{8}$` over area code + number); the UI only gives early feedback.
 */
export function splitPhone(value: string): PhoneParts | null {
  const digits = value.replace(/\D/g, '');
  if (!/^[1-9]{2}9\d{8}$/.test(digits)) return null;
  return { areaCode: Number(digits.slice(0, 2)), phoneNumber: Number(digits.slice(2)) };
}
