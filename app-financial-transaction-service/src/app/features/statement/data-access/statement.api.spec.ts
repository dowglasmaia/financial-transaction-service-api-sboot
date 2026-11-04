import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/http/api-base-url.token';
import { StatementApi } from './statement.api';

function setup() {
  TestBed.configureTestingModule({
    providers: [{ provide: API_BASE_URL, useValue: '/api/v1' }, provideHttpClient(), provideHttpClientTesting()],
  });
  return { api: TestBed.inject(StatementApi), controller: TestBed.inject(HttpTestingController) };
}

describe('StatementApi', () => {
  it('asks for the period with from/to and sends only the filters that are set', () => {
    const { api, controller } = setup();
    api.list({ from: '2026-03-11', to: '2026-03-11', type: null, sort: 'date', order: 'desc', page: 0, size: 10 }).subscribe();

    const request = controller.expectOne((r) => r.url === '/api/v1/transactions');
    expect(request.request.params.get('from')).toBe('2026-03-11');
    expect(request.request.params.get('to')).toBe('2026-03-11');
    expect(request.request.params.get('sort')).toBe('date');
    expect(request.request.params.get('order')).toBe('desc');
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('10');
    expect(request.request.params.has('type')).toBe(false);
  });

  it('adds the type filter when one is chosen', () => {
    const { api, controller } = setup();
    api.list({ from: '2026-03-01', to: '2026-03-31', type: 'PIX', sort: 'amount', order: 'asc', page: 1, size: 10 }).subscribe();
    expect(controller.expectOne((r) => r.url === '/api/v1/transactions').request.params.get('type')).toBe('PIX');
  });

  it('reads one transaction by id and the summary of a period', () => {
    const { api, controller } = setup();
    api.get('6c6fa8c9-743a-4c79-9cf6-e1d5e25ad4a5').subscribe();
    controller.expectOne('/api/v1/transactions/6c6fa8c9-743a-4c79-9cf6-e1d5e25ad4a5');
    api.summary('2026-03-10', '2026-03-12').subscribe();
    const summary = controller.expectOne((r) => r.url === '/api/v1/transactions/summary');
    expect(summary.request.params.get('from')).toBe('2026-03-10');
    expect(summary.request.params.get('to')).toBe('2026-03-12');
  });
});
