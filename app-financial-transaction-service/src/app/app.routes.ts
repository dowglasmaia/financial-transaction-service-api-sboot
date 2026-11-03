import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    // `data.nav` puts the route in the rail / bottom bar (see core/layout/nav-items.ts). Each slice declares its own entry.
    data: { nav: { label: 'Home', icon: 'ph ph-house', order: 1 } },
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
