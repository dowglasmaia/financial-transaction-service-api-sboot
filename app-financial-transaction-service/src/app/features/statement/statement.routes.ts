import { Routes } from '@angular/router';
import { DetailPage } from './pages/detail-page';
import { StatementPage } from './pages/statement-page';

export const STATEMENT_ROUTES: Routes = [
  { path: '', component: StatementPage, title: 'Transactions · Maia Bank' },
  { path: ':id', component: DetailPage, title: 'Transaction · Maia Bank' },
];
