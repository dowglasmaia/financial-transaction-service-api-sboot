import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, provideRouter } from '@angular/router';
import { authGuard } from './auth.guard';
import { SessionService } from './session.service';
import { FakeSession } from './testing/fake-session';

describe('authGuard', () => {
  function run(state: 'authenticated' | 'anonymous') {
    const session = new FakeSession();
    session.state.set(state);
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: SessionService, useValue: session }] });
    return TestBed.runInInjectionContext(() => authGuard({} as never, { url: '/home' } as never)) as Promise<boolean | UrlTree>;
  }

  it('lets a signed-in customer through', async () => {
    expect(await run('authenticated')).toBe(true);
  });

  it('sends an unauthenticated visitor to the landing page', async () => {
    const result = await run('anonymous');
    expect(result instanceof UrlTree).toBe(true);
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/');
  });
});
