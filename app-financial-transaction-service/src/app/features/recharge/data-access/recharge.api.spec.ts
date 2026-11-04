import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/http/api-base-url.token';
import { RechargeApi } from './recharge.api';

describe('RechargeApi', () => {
  it('posts the area code and the number apart, the carrier and the amount, with the Idempotency-Key header', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: '/api/v1' },
      ],
    });
    const api = TestBed.inject(RechargeApi);
    const http = TestBed.inject(HttpTestingController);

    api
      .recharge({ areaCode: 11, phoneNumber: 912345678, carrier: 'VIVO' }, '30.00', 'attempt-1')
      .subscribe();

    const request = http.expectOne('/api/v1/transactions/recharge-mobile');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      areaCode: 11,
      phoneNumber: 912345678,
      carrier: 'VIVO',
      amount: 30,
    });
    expect(request.request.headers.get('Idempotency-Key')).toBe('attempt-1');
    request.flush(null, { status: 204, statusText: 'No Content' });
    http.verify();
  });
});
