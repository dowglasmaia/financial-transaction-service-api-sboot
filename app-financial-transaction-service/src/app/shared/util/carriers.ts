/** The mobile carriers of the contract (the backend enum). v2's "Oi" does not exist in the API: it became Algar. */
export type CarrierId = 'VIVO' | 'CLARO' | 'TIM' | 'ALGAR';

export interface CarrierInfo {
  readonly id: CarrierId;
  readonly label: string;
  /** Brand colour of the text wordmark (no logos). White text on it is at least 4.5:1. */
  readonly classes: string;
}

export const CARRIERS: readonly CarrierInfo[] = [
  { id: 'CLARO', label: 'Claro', classes: 'bg-[#da291c]' },
  { id: 'VIVO', label: 'Vivo', classes: 'bg-[#660099]' },
  { id: 'TIM', label: 'TIM', classes: 'bg-[#0033a0]' },
  { id: 'ALGAR', label: 'Algar', classes: 'bg-[#00703c]' },
];

export function carrierLabel(id: CarrierId | null): string {
  return CARRIERS.find((carrier) => carrier.id === id)?.label ?? '';
}
