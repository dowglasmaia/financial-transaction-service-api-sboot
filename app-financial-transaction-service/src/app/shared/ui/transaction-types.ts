import type { IconTileTone } from './icon-tile.component';

/** Ledger types returned by the API, with their display label, icon, tile tone and direction (income = DEPOSIT only). */
export type TransactionType = 'DEPOSIT' | 'PIX' | 'PHONE_RECHARGE' | 'WITHDRAW';

export interface TransactionTypeInfo {
  readonly label: string;
  readonly icon: string;
  readonly direction: 'in' | 'out';
  /** Tone of the icon tile (v2: deposit green, Pix cyan, recharge violet, withdrawal amber). Label and sign always accompany it. */
  readonly tile: IconTileTone;
}

export const TRANSACTION_TYPES: Readonly<Record<TransactionType, TransactionTypeInfo>> = {
  DEPOSIT: { label: 'Deposit', icon: 'ph-bold ph-bank', direction: 'in', tile: 'income' },
  PIX: { label: 'Pix sent', icon: 'ph-bold ph-diamonds-four', direction: 'out', tile: 'accent' },
  PHONE_RECHARGE: { label: 'Mobile recharge', icon: 'ph-bold ph-device-mobile', direction: 'out', tile: 'violet' },
  WITHDRAW: { label: 'ATM withdrawal', icon: 'ph-bold ph-money', direction: 'out', tile: 'warning' },
};
