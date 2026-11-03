import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SessionService } from '../auth/session.service';
import { FakeSession } from '../auth/testing/fake-session';
import { ApiError } from './api-error';
import { apiErrorInterceptor } from './api-error.interceptor';
import { API_BASE_URL } from './api-base-url.token';
import { authInterceptor } from './auth.interceptor';
import { requestIdInterceptor } from './request-id.interceptor';

function setup() {
  const session = new FakeSession();
  session.state.set('authenticated');
  TestBed.configureTestingModule({
    providers: [
      { provide: API_BASE_URL, useValue: '/api/v1' },
      { provide: SessionService, useValue: session },
      provideHttpClient(withInterceptors([requestIdInterceptor, authInterceptor, apiErrorInterceptor])),
      provideHttpClientTesting(),
    ],
  });
  return { session, http: TestBed.inject(HttpClient), controller: TestBed.inject(HttpTestingController) };
}

/** The interceptor works asynchronously (it asks the session for a token first). */
const tick = () => new Promise((resolve) => setTimeout(resolve));

describe('authInterceptor', () => {
  it('adds the bearer token only to requests for the API', async () => {
    const { http, controller } = setup();
    http.get('/api/v1/accounts/me').subscribe();
    http.get('https://other.example.test/file').subscribe();
    await tick();

    expect(controller.expectOne('/api/v1/accounts/me').request.headers.get('Authorization')).toBe('Bearer tok-1');
    expect(controller.expectOne('https://other.example.test/file').request.headers.has('Authorization')).toBe(false);
  });

  it('does not touch a request that is not for the API, even when the path merely looks similar', async () => {
    const { http, controller, session } = setup();
    http.get('/apiary/v1/x').subscribe();
    await tick();

    expect(controller.expectOne('/apiary/v1/x').request.headers.has('Authorization')).toBe(false);
    expect(session.tokenCalls).toHaveLength(0);
  });

  it('a 401 on a GET triggers exactly one silent renewal and one retry with the new token', async () => {
    const { http, controller, session } = setup();
    let result: unknown;
    http.get('/api/v1/accounts/me').subscribe((body) => (result = body));
    await tick();

    controller.expectOne('/api/v1/accounts/me').flush({ code: 'UNAUTHORIZED', message: 'x' }, { status: 401, statusText: 'Unauthorized' });
    await tick();
    const retry = controller.expectOne('/api/v1/accounts/me');
    expect(retry.request.headers.get('Authorization')).toBe('Bearer tok-2');
    retry.flush({ number: '2022' });

    expect(result).toEqual({ number: '2022' });
    expect(session.tokenCalls).toEqual([{ renew: false }, { renew: true }]);
    expect(session.expired()).toBe(false);
  });

  it('when the retry is also 401 the session is marked expired (one renewal only, no loop)', async () => {
    const { http, controller, session } = setup();
    let error: ApiError | undefined;
    http.get('/api/v1/accounts/me').subscribe({ error: (e: ApiError) => (error = e) });
    await tick();

    controller.expectOne('/api/v1/accounts/me').flush({}, { status: 401, statusText: 'Unauthorized' });
    await tick();
    controller.expectOne('/api/v1/accounts/me').flush({}, { status: 401, statusText: 'Unauthorized' });
    await tick();

    expect(error?.status).toBe(401);
    expect(session.expired()).toBe(true);
    controller.expectNone('/api/v1/accounts/me');
  });

  it('a write is never retried: a 401 on a POST only marks the session expired', async () => {
    const { http, controller, session } = setup();
    let error: ApiError | undefined;
    http.post('/api/v1/transactions/deposit', {}).subscribe({ error: (e: ApiError) => (error = e) });
    await tick();

    controller.expectOne('/api/v1/transactions/deposit').flush({}, { status: 401, statusText: 'Unauthorized' });
    await tick();

    expect(error?.status).toBe(401);
    expect(session.expired()).toBe(true);
    expect(session.tokenCalls).toEqual([{ renew: false }]);
    controller.expectNone('/api/v1/transactions/deposit');
  });

  it('without a token the request is not sent: the session is marked expired and the caller gets a 401 error', async () => {
    const { http, controller, session } = setup();
    session.token = null;
    let error: ApiError | undefined;
    http.get('/api/v1/accounts/me').subscribe({ error: (e: ApiError) => (error = e) });
    await tick();

    controller.expectNone('/api/v1/accounts/me');
    expect(error?.status).toBe(401);
    expect(error?.code).toBe('UNAUTHENTICATED');
    expect(session.expired()).toBe(true);
  });

  it('other statuses (403, 422, 500) pass through untouched and do not renew or expire the session', async () => {
    const { http, controller, session } = setup();
    for (const status of [403, 422, 500]) {
      let error: ApiError | undefined;
      http.get(`/api/v1/x${status}`).subscribe({ error: (e: ApiError) => (error = e) });
      await tick();
      controller.expectOne(`/api/v1/x${status}`).flush({ code: 'X', message: 'm' }, { status, statusText: 's' });
      expect(error?.status).toBe(status);
    }
    expect(session.expired()).toBe(false);
    expect(session.tokenCalls.every((call) => !call.renew)).toBe(true);
  });
});
