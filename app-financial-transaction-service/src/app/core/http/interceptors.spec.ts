import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiError } from './api-error';
import { apiErrorInterceptor } from './api-error.interceptor';
import { API_BASE_URL } from './api-base-url.token';
import { AUTH_TOKEN_PROVIDER, authInterceptor } from './auth.interceptor';
import { requestIdInterceptor } from './request-id.interceptor';

function setup(token: string | null = null) {
  TestBed.configureTestingModule({
    providers: [
      { provide: API_BASE_URL, useValue: '/api' },
      { provide: AUTH_TOKEN_PROVIDER, useValue: () => token },
      provideHttpClient(withInterceptors([requestIdInterceptor, authInterceptor, apiErrorInterceptor])),
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

describe('authInterceptor', () => {
  it('adds the bearer token only to API requests', () => {
    const { http, controller } = setup('tok-123');
    http.get('/api/v1/transactions').subscribe();
    http.get('https://other.example.test/file').subscribe();
    expect(controller.expectOne('/api/v1/transactions').request.headers.get('Authorization')).toBe('Bearer tok-123');
    expect(controller.expectOne('https://other.example.test/file').request.headers.has('Authorization')).toBe(false);
  });

  it('sends no Authorization header when there is no token (dev-open mode)', () => {
    const { http, controller } = setup(null);
    http.get('/api/v1/transactions').subscribe();
    expect(controller.expectOne('/api/v1/transactions').request.headers.has('Authorization')).toBe(false);
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
