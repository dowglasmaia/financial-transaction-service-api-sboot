import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { SessionService } from '../../core/auth/session.service';
import { FakeSession } from '../../core/auth/testing/fake-session';
import { NoAccessPage } from './no-access-page';
import { NotFoundPage } from './not-found-page';

describe('system pages', () => {
  it('"no access" explains the situation and signs the customer out on request', async () => {
    const session = new FakeSession();
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: SessionService, useValue: session }] });
    const fixture = TestBed.createComponent(NoAccessPage);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('h1')?.textContent).toContain('No access to this app yet');
    expect(el.textContent).toContain('not linked to a Maia Bank customer');
    el.querySelector<HTMLButtonElement>('button')!.click();
    expect(session.logoutCalls).toBe(1);
  });

  it('"not found" offers a way back', async () => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(NotFoundPage);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('h1')?.textContent).toContain('Page not found');
    expect(el.querySelector('a')?.getAttribute('href')).toBe('/');
  });
});
