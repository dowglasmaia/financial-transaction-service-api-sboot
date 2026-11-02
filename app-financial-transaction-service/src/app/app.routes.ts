import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadChildren: () => import('./features/home/home.routes').then((module) => module.HOME_ROUTES),
  },
  {
    // Living style guide: tokens and shared components in both themes (kept in all environments for the demo).
    path: 'design',
    title: 'Design system · Maia Bank',
    loadChildren: () => import('./features/design/design.routes').then((module) => module.DESIGN_ROUTES),
  },
  { path: '**', redirectTo: '' },
];
