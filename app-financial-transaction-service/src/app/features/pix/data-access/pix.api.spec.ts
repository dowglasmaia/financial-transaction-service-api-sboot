import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/http/api-base-url.token';
import { PixApi } from './pix.api';

describe('PixApi', () => {
  it('posts {pixKey, keyType, transactionAmount} with the Idempotency-Key header', () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: '/api/v1' },
      ],
    });
    const api = TestBed.inject(PixApi);
    const http = TestBed.inject(HttpTestingController);

    api.send('cpf', '52998224725', '50.00', 'attempt-1').subscribe();

    const request = http.expectOne('/api/v1/transactions/send-pix');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      pixKey: '52998224725',
      keyType: 'cpf',
      transactionAmount: 50,
    });
    expect(request.request.headers.get('Idempotency-Key')).toBe('attempt-1');
    request.flush(null, { status: 204, statusText: 'No Content' });
    http.verify();
  });
});
