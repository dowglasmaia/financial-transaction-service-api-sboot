import { maskPhone } from './phone';

/** The key types of the contract (`keyType`, lower case). */
export type PixKeyKind = 'cpf' | 'email' | 'phone';

export const PIX_KEY_KINDS: readonly {
  readonly value: PixKeyKind;
  readonly label: string;
  readonly name: string;
}[] = [
  { value: 'cpf', label: 'CPF', name: 'CPF' },
  { value: 'email', label: 'E-mail', name: 'e-mail' },
  { value: 'phone', label: 'Mobile', name: 'mobile number' },
];

/*
 * These rules mirror the backend (`CPFValidator`, `EmailValidator`, `PhoneValidator`; SL-06, F-11) and are checked
 * against the SAME case lists in `pix-key.spec.ts` and `PixKeyRulesTest`. They only give early feedback: the server decides.
 */
const CPF_PLAIN = /^\d{11}$/;
const CPF_MASKED = /^\d{3}\.\d{3}\.\d{3}-\d{2}$/;
const EMAIL =
  /^[A-Za-z0-9]+([._%+-][A-Za-z0-9]+)*@([A-Za-z0-9]+(-+[A-Za-z0-9]+)*\.)+[A-Za-z]{2,24}$/;
const PHONE = /^[1-9]{2}9\d{8}$/;
const EMAIL_MAX_LENGTH = 77;

function checkDigit(digits: string, length: number): number {
  let sum = 0;
  for (let i = 0; i < length; i++) sum += Number(digits[i]) * (length + 1 - i);
  const rest = sum % 11;
  return rest < 2 ? 0 : 11 - rest;
}

function isValidCpf(value: string): boolean {
  if (!CPF_PLAIN.test(value) && !CPF_MASKED.test(value)) return false;
  const digits = value.replace(/[.-]/g, '');
  if (new Set(digits).size === 1) return false;
  return (
    checkDigit(digits, 9) === Number(digits[9]) && checkDigit(digits, 10) === Number(digits[10])
  );
}

/** Is `wire` (the value that would be sent) a valid key of this type, by the backend's rules? */
export function isValidPixKey(kind: PixKeyKind, wire: string): boolean {
  switch (kind) {
    case 'cpf':
      return isValidCpf(wire);
    case 'email':
      return wire.length <= EMAIL_MAX_LENGTH && EMAIL.test(wire);
    case 'phone':
      return PHONE.test(wire);
  }
}

/** What the field shows while typing: a CPF as 999.999.999-99, a mobile as (11) 9 1234-5678, an e-mail as typed. */
export function maskPixInput(kind: PixKeyKind, typed: string): string {
  switch (kind) {
    case 'cpf': {
      const d = typed.replace(/\D/g, '').slice(0, 11);
      let masked = d.slice(0, 3);
      if (d.length > 3) masked += `.${d.slice(3, 6)}`;
      if (d.length > 6) masked += `.${d.slice(6, 9)}`;
      if (d.length > 9) masked += `-${d.slice(9)}`;
      return masked;
    }
    case 'phone':
      return maskPhone(typed);
    case 'email':
      return typed;
  }
}

/** The value that goes in the request: digits only for a CPF and a mobile, the trimmed text for an e-mail. */
export function toWireKey(kind: PixKeyKind, typed: string): string {
  return kind === 'email' ? typed.trim() : typed.replace(/\D/g, '');
}

const MESSAGES: Record<PixKeyKind, string> = {
  cpf: 'Enter a valid CPF, for example 529.982.247-25.',
  email: 'Enter a valid e-mail address.',
  phone: 'Enter a valid mobile number, for example (11) 9 1234-5678.',
};

export interface PixKeyFeedback {
  /** The wire key when the typed text is valid, else null. */
  readonly key: string | null;
  /** Early feedback, or null (also null while the field is empty). */
  readonly message: string | null;
}

export function pixKeyFeedback(kind: PixKeyKind, typed: string): PixKeyFeedback {
  if (typed.trim() === '') return { key: null, message: null };
  const wire = toWireKey(kind, typed);
  return isValidPixKey(kind, wire)
    ? { key: wire, message: null }
    : { key: null, message: MESSAGES[kind] };
}

/** The key as the review and the receipt show it: never the whole value. */
export function maskPixKeyForDisplay(kind: PixKeyKind, wire: string): string {
  switch (kind) {
    case 'cpf':
      return `•••.${wire.slice(3, 6)}.•••-${wire.slice(-2)}`;
    case 'phone':
      return `(${wire.slice(0, 2)}) ${wire[2]} ••••-${wire.slice(-4)}`;
    case 'email':
      return `${wire[0]}•••${wire.slice(wire.indexOf('@'))}`;
  }
}
