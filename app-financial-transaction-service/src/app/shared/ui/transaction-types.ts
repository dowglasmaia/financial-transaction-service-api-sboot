/** Ledger types returned by the API, with their display label, icon and direction (income = DEPOSIT only). */
export type TransactionType = 'DEPOSIT' | 'PIX' | 'PHONE_RECHARGE' | 'WITHDRAW';

export interface TransactionTypeInfo {
  readonly label: string;
  readonly icon: string;
  readonly direction: 'in' | 'out';
}

export const TRANSACTION_TYPES: Readonly<Record<TransactionType, TransactionTypeInfo>> = {
  DEPOSIT: { label: 'Deposit', icon: 'ph-bold ph-arrow-down-left', direction: 'in' },
  PIX: { label: 'Pix sent', icon: 'ph-bold ph-paper-plane-tilt', direction: 'out' },
  PHONE_RECHARGE: { label: 'Mobile recharge', icon: 'ph-bold ph-device-mobile', direction: 'out' },
  WITHDRAW: { label: 'ATM withdrawal', icon: 'ph-bold ph-money', direction: 'out' },
};
