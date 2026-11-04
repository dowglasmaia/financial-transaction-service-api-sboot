import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { SessionService } from '../../../core/auth/session.service';
import { AlertComponent } from '../../../shared/ui/alert.component';
import { BalanceCardComponent } from '../../../shared/ui/balance-card.component';
import { ButtonDirective } from '../../../shared/ui/button.directive';
import { ChipComponent } from '../../../shared/ui/chip.component';
import { EmptyStateComponent } from '../../../shared/ui/empty-state.component';
import { KpiTileComponent } from '../../../shared/ui/kpi-tile.component';
import { LedgerRowComponent } from '../../../shared/ui/ledger-row.component';
import { LineChartComponent } from '../../../shared/ui/line-chart.component';
import { SkeletonComponent } from '../../../shared/ui/skeleton.component';
import { formatBrl } from '../../../shared/util/money';
import { AccountStore } from '../../account/data-access/account.store';
import { SummaryStore } from '../../statement/data-access/ledger.stores';
import { periodRange } from '../../statement/data-access/statement-query';

/**
 * Home: who is signed in, their account and balance (`GET /accounts/me`) and the activity summary of a period
 * (`GET /transactions/summary` + the 5 latest rows). Every figure comes from the backend; nothing is added up here.
 */
@Component({
  selector: 'app-home-page',
  imports: [AlertComponent, BalanceCardComponent, ButtonDirective, ChipComponent, EmptyStateComponent, KpiTileComponent, LedgerRowComponent, LineChartComponent, RouterLink, SkeletonComponent],
  templateUrl: './home-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomePage {
  protected readonly store = inject(AccountStore);
  protected readonly summary = inject(SummaryStore);
  protected readonly periods = [7, 30, 90] as const;
  protected readonly days = signal<7 | 30 | 90>(30);
  protected readonly format = formatBrl;
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);

  protected readonly firstName = computed(() => (this.session.user()?.name ?? '').trim().split(/\s+/)[0] || 'there');

  constructor() {
    this.store.load();
    effect(() => {
      const { from, to } = periodRange(this.days());
      this.summary.load(from, to);
    });
    effect(() => {
      // A valid sign-in that is not linked to a customer: say so on its own page instead of a broken Home.
      if (this.store.error()?.status === 403) void this.router.navigateByUrl('/no-access', { replaceUrl: true });
    });
  }

  protected retry(): void {
    this.store.load();
  }

  protected retrySummary(): void {
    const { from, to } = periodRange(this.days());
    this.summary.load(from, to);
  }
}
