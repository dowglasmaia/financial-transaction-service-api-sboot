import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    // Public start page; also where the identity provider sends the browser back to after a sign-in.
    path: '',
    pathMatch: 'full',
    title: 'Maia Bank',
    loadComponent: () => import('./features/landing/landing-page').then((module) => module.LandingPage),
  },
  {
    path: 'signin',
    title: 'Sign in · Maia Bank',
    loadComponent: () => import('./features/signin/signin-page').then((module) => module.SignInPage),
  },
  {
    path: 'home',
    canActivate: [authGuard],
    // `data.nav` puts the route in the rail / bottom bar (see core/layout/nav-items.ts). Each slice declares its own entry.
    data: { nav: { label: 'Home', icon: 'ph ph-house', order: 1 } },
    loadChildren: () => import('./features/home/home.routes').then((module) => module.HOME_ROUTES),
  },
  {
    path: 'transactions',
    canActivate: [authGuard],
    data: { nav: { label: 'Transactions', icon: 'ph ph-receipt', order: 2 } },
    loadChildren: () => import('./features/statement/statement.routes').then((module) => module.STATEMENT_ROUTES),
  },
  {
    path: 'no-access',
    canActivate: [authGuard],
    title: 'No access · Maia Bank',
    loadComponent: () => import('./features/system/no-access-page').then((module) => module.NoAccessPage),
  },
  {
    // Living style guide: tokens and shared components in both themes (kept in all environments for the demo).
    path: 'design',
    title: 'Design system · Maia Bank',
    loadChildren: () => import('./features/design/design.routes').then((module) => module.DESIGN_ROUTES),
  },
  {
    path: '**',
    title: 'Page not found · Maia Bank',
    loadComponent: () => import('./features/system/not-found-page').then((module) => module.NotFoundPage),
  },
];
