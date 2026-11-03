import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/http/api-base-url.token';
import { AccountStore } from './account.store';

function setup() {
  TestBed.configureTestingModule({
    providers: [{ provide: API_BASE_URL, useValue: '/api/v1' }, provideHttpClient(), provideHttpClientTesting()],
  });
  return { store: TestBed.inject(AccountStore), controller: TestBed.inject(HttpTestingController) };
}

describe('AccountStore', () => {
  it('starts idle with no account', () => {
    const { store } = setup();
    expect(store.state()).toBe('idle');
    expect(store.account()).toBeNull();
  });

  it('loads the account from GET /accounts/me and keeps the backend values untouched', () => {
    const { store, controller } = setup();
    store.load();
    expect(store.state()).toBe('loading');

    controller.expectOne('/api/v1/accounts/me').flush({ agency: '0211', number: '2022', balance: 5000 });

    expect(store.state()).toBe('ready');
    expect(store.account()).toEqual({ agency: '0211', number: '2022', balance: 5000 });
  });

  it('keeps the error (status, code, message) so the screen can react to a 403', () => {
    const { store, controller } = setup();
    store.load();

    controller.expectOne('/api/v1/accounts/me').flush({ code: 'FORBIDDEN', message: 'Access denied' }, { status: 403, statusText: 'Forbidden' });

    expect(store.state()).toBe('error');
    expect(store.account()).toBeNull();
  });

  it('clear() forgets the previous customer completely', () => {
    const { store, controller } = setup();
    store.load();
    controller.expectOne('/api/v1/accounts/me').flush({ agency: '0211', number: '2022', balance: 5000 });

    store.clear();

    expect(store.account()).toBeNull();
    expect(store.state()).toBe('idle');
  });
});
