import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { components } from '../../../core/api/schema';
import { API_BASE_URL } from '../../../core/http/api-base-url.token';

type RechargeBody = components['schemas']['MobileRechargeRequest'];
export type Carrier = RechargeBody['carrier'];

/** Who is recharged: the phone split in area code + number, and the carrier. */
export interface RechargeTarget {
  readonly areaCode: number;
  readonly phoneNumber: number;
  readonly carrier: Carrier;
}

/** The only place that talks HTTP about recharges. The carrier credit is asynchronous: 204 means "requested". */
@Injectable({ providedIn: 'root' })
export class RechargeApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  /** `amount` is the normalized two-decimal string; `idempotencyKey` identifies this user attempt. */
  recharge(target: RechargeTarget, amount: string, idempotencyKey: string): Observable<void> {
    const body: RechargeBody = {
      areaCode: target.areaCode,
      phoneNumber: target.phoneNumber,
      carrier: target.carrier,
      amount: Number(amount),
    };
    return this.http.post<void>(`${this.baseUrl}/transactions/recharge-mobile`, body, {
      headers: { 'Idempotency-Key': idempotencyKey },
    });
  }
}
