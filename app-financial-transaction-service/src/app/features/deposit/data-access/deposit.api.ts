import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { components } from '../../../core/api/schema';
import { API_BASE_URL } from '../../../core/http/api-base-url.token';

type DepositBody = components['schemas']['DepositRequest'];

/** Where the deposit goes: the signed-in customer's own agency and account number (from `/accounts/me`). */
export interface DepositTarget {
  readonly agency: string;
  readonly account: string;
}

/** The only place that talks HTTP about deposits. There is no boleto on the server: only this one money movement. */
@Injectable({ providedIn: 'root' })
export class DepositApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  /** `amount` is the normalized two-decimal string; `idempotencyKey` identifies this user attempt. */
  deposit(target: DepositTarget, amount: string, idempotencyKey: string): Observable<void> {
    const body: DepositBody = { agency: target.agency, account: target.account, amount: Number(amount) };
    return this.http.post<void>(`${this.baseUrl}/transactions/deposit`, body, { headers: { 'Idempotency-Key': idempotencyKey } });
  }
}
