import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { components } from '../../../core/api/schema';
import { API_BASE_URL } from '../../../core/http/api-base-url.token';
import type { TransactionType } from '../../../shared/ui/transaction-types';
import type { OrderKey, SortKey } from './statement-query';

export type StatementResponse = components['schemas']['TransactionsResponse'];
export type TransactionRow = components['schemas']['TransactionResponse'];
export type PeriodSummary = components['schemas']['TransactionsSummaryResponse'];

export interface StatementRequest {
  readonly from: string;
  readonly to: string;
  readonly type: TransactionType | null;
  readonly sort: SortKey;
  readonly order: OrderKey;
  readonly page: number;
  readonly size: number;
}

/** The only place that talks HTTP about the ledger (statement, one transaction, period summary). */
@Injectable({ providedIn: 'root' })
export class StatementApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  list(request: StatementRequest): Observable<StatementResponse> {
    let params = new HttpParams()
      .set('from', request.from)
      .set('to', request.to)
      .set('sort', request.sort)
      .set('order', request.order)
      .set('page', request.page)
      .set('size', request.size);
    if (request.type) params = params.set('type', request.type);
    return this.http.get<StatementResponse>(`${this.baseUrl}/transactions`, { params });
  }

  get(id: string): Observable<TransactionRow> {
    return this.http.get<TransactionRow>(`${this.baseUrl}/transactions/${encodeURIComponent(id)}`);
  }

  summary(from: string, to: string): Observable<PeriodSummary> {
    return this.http.get<PeriodSummary>(`${this.baseUrl}/transactions/summary`, { params: new HttpParams().set('from', from).set('to', to) });
  }
}
