import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { SessionService } from './session.service';

/** Protects the customer screens: wait for the library to settle, then let a signed-in customer in or go to the landing page. */
export const authGuard: CanActivateFn = async (): Promise<boolean | UrlTree> => {
  const session = inject(SessionService);
  const router = inject(Router);
  return (await session.whenSettled()) === 'authenticated' ? true : router.createUrlTree(['/']);
};
