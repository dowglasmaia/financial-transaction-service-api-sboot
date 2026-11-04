import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/http/api-base-url.token';
import { WithdrawApi } from './withdraw.api';

describe('WithdrawApi', () => {
  function setup() {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_BASE_URL, useValue: '/api/v1' }],
    });
    return { api: TestBed.inject(WithdrawApi), http: TestBed.inject(HttpTestingController) };
  }

  it('posts exactly {amount} with the Idempotency-Key header', () => {
    const { api, http } = setup();
    api.withdraw('100.00', 'attempt-1').subscribe();

    const request = http.expectOne('/api/v1/transactions/atm-withdrawal');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ amount: 100 });
    expect(Object.keys(request.request.body)).toEqual(['amount']);
    expect(request.request.headers.get('Idempotency-Key')).toBe('attempt-1');
    request.flush(null, { status: 204, statusText: 'No Content' });
    http.verify();
  });

  it('keeps cents exact', () => {
    const { api, http } = setup();
    api.withdraw('1234.56', 'k').subscribe();
    expect(http.expectOne('/api/v1/transactions/atm-withdrawal').request.body).toEqual({ amount: 1234.56 });
  });
});
