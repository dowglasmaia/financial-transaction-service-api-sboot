import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiError } from './api-error';
import { apiErrorInterceptor } from './api-error.interceptor';
import { API_BASE_URL } from './api-base-url.token';
import { requestIdInterceptor } from './request-id.interceptor';

function setup() {
  TestBed.configureTestingModule({
    providers: [
      { provide: API_BASE_URL, useValue: '/api' },
      provideHttpClient(withInterceptors([requestIdInterceptor, apiErrorInterceptor])),
      provideHttpClientTesting(),
    ],
  });
  return { http: TestBed.inject(HttpClient), controller: TestBed.inject(HttpTestingController) };
}

describe('apiErrorInterceptor', () => {
  it('maps a backend error body to an ApiError with code, message and request id', () => {
    const { http, controller } = setup();
    let error: ApiError | undefined;
    http.post('/api/v1/transactions/deposit', {}).subscribe({ error: (e: ApiError) => (error = e) });
    const request = controller.expectOne('/api/v1/transactions/deposit');
    request.flush({ code: 'UNPROCESSABLE_ENTITY', message: 'Amount must be positive and non-null' }, { status: 422, statusText: 'Unprocessable' });
    expect(error?.status).toBe(422);
    expect(error?.code).toBe('UNPROCESSABLE_ENTITY');
    expect(error?.message).toBe('Amount must be positive and non-null');
    expect(error?.requestId).toBe(request.request.headers.get('X-Request-Id'));
  });

  it('maps a network failure to status 0 and code NETWORK', () => {
    const { http, controller } = setup();
    let error: ApiError | undefined;
    http.get('/api/v1/transactions').subscribe({ error: (e: ApiError) => (error = e) });
    controller.expectOne('/api/v1/transactions').error(new ProgressEvent('error'));
    expect(error?.status).toBe(0);
    expect(error?.code).toBe('NETWORK');
  });

  it('falls back to a generic message when the body is not the backend shape', () => {
    const { http, controller } = setup();
    let error: ApiError | undefined;
    http.get('/api/x').subscribe({ error: (e: ApiError) => (error = e) });
    controller.expectOne('/api/x').flush('<html>boom</html>', { status: 500, statusText: 'Server Error' });
    expect(error?.status).toBe(500);
    expect(error?.code).toBe('HTTP_500');
    expect(error?.message).toBe('Something went wrong. Try again.');
  });
});

describe('requestIdInterceptor', () => {
  it('adds a UUID X-Request-Id to API requests and keeps an existing one', () => {
    const { http, controller } = setup();
    http.get('/api/a').subscribe();
    http.get('/api/b', { headers: { 'X-Request-Id': 'fixed' } }).subscribe();
    expect(controller.expectOne('/api/a').request.headers.get('X-Request-Id')).toMatch(/^[0-9a-f-]{36}$/);
    expect(controller.expectOne('/api/b').request.headers.get('X-Request-Id')).toBe('fixed');
  });
});
