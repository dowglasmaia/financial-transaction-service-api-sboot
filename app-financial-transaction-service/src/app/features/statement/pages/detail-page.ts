import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { AlertComponent } from '../../../shared/ui/alert.component';
import { ButtonDirective } from '../../../shared/ui/button.directive';
import { EmptyStateComponent } from '../../../shared/ui/empty-state.component';
import { IconTileComponent } from '../../../shared/ui/icon-tile.component';
import { SkeletonComponent } from '../../../shared/ui/skeleton.component';
import { ToastService } from '../../../shared/ui/toast.service';
import { TRANSACTION_TYPES } from '../../../shared/ui/transaction-types';
import { formatBrl } from '../../../shared/util/money';
import { TransactionDetailStore } from '../data-access/ledger.stores';

/** One transaction of the signed-in customer. A foreign or unknown id looks exactly like a missing one. */
@Component({
  selector: 'app-detail-page',
  imports: [AlertComponent, ButtonDirective, DatePipe, EmptyStateComponent, IconTileComponent, RouterLink, SkeletonComponent],
  templateUrl: './detail-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DetailPage {
  protected readonly store = inject(TransactionDetailStore);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(ToastService);

  private readonly id = toSignal(this.route.paramMap.pipe(map((params) => params.get('id') ?? '')), { initialValue: '' });

  protected readonly info = computed(() => {
    const type = this.store.row()?.transactionType;
    return type ? TRANSACTION_TYPES[type] : null;
  });
  protected readonly signed = computed(() => {
    const row = this.store.row();
    const info = this.info();
    return row && info ? `${info.direction === 'in' ? '+' : '−'} ${formatBrl(Math.abs(Number(row.amount)))}` : '';
  });
  protected readonly shortId = computed(() => {
    const id = this.store.row()?.id ?? '';
    return id.length > 13 ? `${id.slice(0, 8)}…${id.slice(-4)}` : id;
  });

  constructor() {
    effect(() => {
      const id = this.id();
      if (id) this.store.load(id);
    });
  }

  protected retry(): void {
    this.store.load(this.id());
  }

  protected async copyId(): Promise<void> {
    const id = this.store.row()?.id;
    if (!id) return;
    try {
      await navigator.clipboard.writeText(id);
      this.toast.show('Transaction id copied');
    } catch {
      this.toast.show('Could not copy the id');
    }
  }
}
