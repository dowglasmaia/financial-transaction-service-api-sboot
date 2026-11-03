import { Injectable, inject, signal } from '@angular/core';
import { ApiError } from '../../../core/http/api-error';
import { AccountApi, AccountMe } from './account.api';

export type AccountState = 'idle' | 'loading' | 'ready' | 'error';

/** The customer's account (agency, number, balance) as one shared, read-only state: the backend is the only authority. */
@Injectable({ providedIn: 'root' })
export class AccountStore {
  private readonly api = inject(AccountApi);

  readonly account = signal<AccountMe | null>(null);
  readonly state = signal<AccountState>('idle');
  readonly error = signal<ApiError | null>(null);

  load(): void {
    this.state.set('loading');
    this.error.set(null);
    this.api.me().subscribe({
      next: (account) => {
        this.account.set(account);
        this.state.set('ready');
      },
      error: (error: ApiError) => {
        this.error.set(error);
        this.state.set('error');
      },
    });
  }

  /** Forget everything (sign-out): the next customer must never see the previous one's data. */
  clear(): void {
    this.account.set(null);
    this.error.set(null);
    this.state.set('idle');
  }
}
