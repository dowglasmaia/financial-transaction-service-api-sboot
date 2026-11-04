import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/http/api-base-url.token';
import { DepositApi } from './deposit.api';

describe('DepositApi', () => {
  function setup() {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_BASE_URL, useValue: '/api/v1' }],
    });
    return { api: TestBed.inject(DepositApi), http: TestBed.inject(HttpTestingController) };
  }

  it('posts {agency, account, amount} of the caller with the Idempotency-Key header', () => {
    const { api, http } = setup();
    api.deposit({ agency: '0211', account: '2022' }, '250.00', 'attempt-1').subscribe();

    const request = http.expectOne('/api/v1/transactions/deposit');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ agency: '0211', account: '2022', amount: 250 });
    expect(request.request.headers.get('Idempotency-Key')).toBe('attempt-1');
    request.flush(null, { status: 204, statusText: 'No Content' });
    http.verify();
  });

  it('keeps cents exact', () => {
    const { api, http } = setup();
    api.deposit({ agency: '0211', account: '2022' }, '1234.56', 'k').subscribe();
    expect(http.expectOne('/api/v1/transactions/deposit').request.body.amount).toBe(1234.56);
  });
});
