import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { components } from '../../../core/api/schema';
import { API_BASE_URL } from '../../../core/http/api-base-url.token';

/** The signed-in customer's account, exactly as the contract defines it (`GET /accounts/me`). */
export type AccountMe = components['schemas']['AccountResponse'];

/** The only place that talks HTTP about the account. */
@Injectable({ providedIn: 'root' })
export class AccountApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  me(): Observable<AccountMe> {
    return this.http.get<AccountMe>(`${this.baseUrl}/accounts/me`);
  }
}
