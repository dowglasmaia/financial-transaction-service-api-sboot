import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { components } from '../../../core/api/schema';
import { API_BASE_URL } from '../../../core/http/api-base-url.token';
import type { PixKeyKind } from '../../../shared/util/pix-key';

type PixBody = components['schemas']['PixRequest'];

/** The only place that talks HTTP about Pix. The key is never logged here or anywhere else. */
@Injectable({ providedIn: 'root' })
export class PixApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  /** `pixKey` is the wire key, `amount` the normalized two-decimal string, `idempotencyKey` identifies this user attempt. */
  send(
    keyType: PixKeyKind,
    pixKey: string,
    amount: string,
    idempotencyKey: string,
  ): Observable<void> {
    const body: PixBody = { pixKey, keyType, transactionAmount: Number(amount) };
    return this.http.post<void>(`${this.baseUrl}/transactions/send-pix`, body, {
      headers: { 'Idempotency-Key': idempotencyKey },
    });
  }
}
