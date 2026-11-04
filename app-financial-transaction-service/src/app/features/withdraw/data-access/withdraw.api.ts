import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { components } from '../../../core/api/schema';
import { API_BASE_URL } from '../../../core/http/api-base-url.token';

type WithdrawBody = Pick<components['schemas']['WithdrawRequest'], 'amount'>;

/** The only place that talks HTTP about withdrawals. The body is `{amount}` only: the account is always the signed-in customer's. */
@Injectable({ providedIn: 'root' })
export class WithdrawApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  /** `amount` is the normalized two-decimal string; `idempotencyKey` identifies this user attempt. */
  withdraw(amount: string, idempotencyKey: string): Observable<void> {
    const body: WithdrawBody = { amount: Number(amount) };
    return this.http.post<void>(`${this.baseUrl}/transactions/atm-withdrawal`, body, { headers: { 'Idempotency-Key': idempotencyKey } });
  }
}
