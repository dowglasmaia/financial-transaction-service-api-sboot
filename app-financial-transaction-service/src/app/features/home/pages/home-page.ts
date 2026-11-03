import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core';
import { Router } from '@angular/router';
import { SessionService } from '../../../core/auth/session.service';
import { AlertComponent } from '../../../shared/ui/alert.component';
import { BalanceCardComponent } from '../../../shared/ui/balance-card.component';
import { ButtonDirective } from '../../../shared/ui/button.directive';
import { SkeletonComponent } from '../../../shared/ui/skeleton.component';
import { AccountStore } from '../../account/data-access/account.store';

/** Home: who is signed in, their account and the balance. Every figure comes from `GET /accounts/me`; nothing is computed here. */
@Component({
  selector: 'app-home-page',
  imports: [AlertComponent, BalanceCardComponent, ButtonDirective, SkeletonComponent],
  templateUrl: './home-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomePage {
  protected readonly store = inject(AccountStore);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);

  protected readonly firstName = computed(() => (this.session.user()?.name ?? '').trim().split(/\s+/)[0] || 'there');

  constructor() {
    this.store.load();
    effect(() => {
      // A valid sign-in that is not linked to a customer: say so on its own page instead of a broken Home.
      if (this.store.error()?.status === 403) void this.router.navigateByUrl('/no-access', { replaceUrl: true });
    });
  }

  protected retry(): void {
    this.store.load();
  }
}
