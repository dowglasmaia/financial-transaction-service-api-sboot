import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { AccountStore } from '../../account/data-access/account.store';
import { OperationController } from '../../../shared/operation/operation';
import { AmountPresetsComponent } from '../../../shared/ui/amount-presets.component';
import { ButtonDirective } from '../../../shared/ui/button.directive';
import { IconTileComponent } from '../../../shared/ui/icon-tile.component';
import { ResultPanelComponent } from '../../../shared/ui/result-panel.component';
import { ReceiptLine, ReviewDialogComponent } from '../../../shared/ui/review-dialog.component';
import { formatBrl, parseAmountInput } from '../../../shared/util/money';
import { WithdrawApi } from '../data-access/withdraw.api';

const UNCERTAIN_MESSAGE = "We couldn't confirm the operation. Check your transactions before trying again, so you don't pay twice.";

/**
 * Withdraw from the signed-in customer's own account. The body is `{amount}` only. The page shows what the customer typed,
 * asks for a review, and lets the operation shell send ONE request; the server decides whether it is accepted.
 */
@Component({
  selector: 'app-withdraw-page',
  templateUrl: './withdraw-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AmountPresetsComponent, ButtonDirective, IconTileComponent, ResultPanelComponent, ReviewDialogComponent],
})
export class WithdrawPage {
  private readonly api = inject(WithdrawApi);
  protected readonly account = inject(AccountStore);
  protected readonly operation = new OperationController();

  protected readonly text = signal('');
  protected readonly reviewOpen = signal(false);
  protected readonly showResult = signal(false);
  protected readonly doneAt = signal('');

  protected readonly balance = computed(() => this.account.account()?.balance ?? null);
  protected readonly formattedBalance = computed(() => formatBrl(this.balance() ?? 0));
  protected readonly amount = computed(() => parseAmountInput(this.text()));
  protected readonly lines = computed<ReceiptLine[]>(() => [
    { label: 'Amount', value: formatBrl(this.amount() ?? 0) },
    { label: 'From', value: 'Your account' },
  ]);
  protected readonly uncertainMessage = UNCERTAIN_MESSAGE;

  constructor() {
    this.account.load();
    effect(() => {
      const phase = this.operation.phase();
      untracked(() => {
        if (phase === 'pending') return;
        this.reviewOpen.set(false);
        if (phase === 'idle') return;
        this.showResult.set(true);
        if (phase === 'success') {
          this.doneAt.set(new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }));
          this.account.load(); // the balance on screen is always the API's, never a local calculation
        }
      });
    });
  }

  protected openReview(): void {
    if (this.amount()) this.reviewOpen.set(true);
  }

  protected confirm(): void {
    const amount = this.amount();
    if (!amount) return;
    this.operation.run((key) => this.api.withdraw(amount, key));
  }

  protected tryAgain(): void {
    this.operation.retry();
  }

  /** Business failure: back to the form with what was typed. */
  protected edit(): void {
    this.operation.reset();
    this.showResult.set(false);
  }

  protected another(): void {
    this.text.set('');
    this.edit();
  }
}
