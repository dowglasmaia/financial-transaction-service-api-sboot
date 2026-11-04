import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { SessionService } from '../../../core/auth/session.service';
import { OperationController } from '../../../shared/operation/operation';
import { AccountStore } from '../../account/data-access/account.store';
import { AmountPresetsComponent } from '../../../shared/ui/amount-presets.component';
import { ButtonDirective } from '../../../shared/ui/button.directive';
import { IconTileComponent } from '../../../shared/ui/icon-tile.component';
import { ResultPanelComponent } from '../../../shared/ui/result-panel.component';
import { ReceiptLine } from '../../../shared/ui/review-dialog.component';
import { ToastService } from '../../../shared/ui/toast.service';
import { formatBrl, parseAmountInput } from '../../../shared/util/money';
import { DepositApi } from '../data-access/deposit.api';
import { Boleto, buildBoleto } from '../domain/boleto';

const UNCERTAIN_MESSAGE = "We couldn't confirm the operation. Check your transactions before trying again, so you don't pay twice.";
const PRESETS = [100, 250, 500, 1000] as const;

/**
 * Deposit into the signed-in customer's own account, shown as a DEMO boleto: "Generate boleto" builds a boleto on this
 * device from the amount (nothing is issued, stored or fetched) and "Simulate payment" sends the one real call,
 * `POST /transactions/deposit` for the customer's own agency and account. Every boleto surface says it is a demo.
 */
@Component({
  selector: 'app-deposit-page',
  templateUrl: './deposit-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AmountPresetsComponent, ButtonDirective, IconTileComponent, ResultPanelComponent],
})
export class DepositPage {
  private readonly api = inject(DepositApi);
  private readonly session = inject(SessionService);
  private readonly toast = inject(ToastService);
  protected readonly account = inject(AccountStore);
  protected readonly operation = new OperationController();

  protected readonly presets = PRESETS;
  protected readonly text = signal('');
  protected readonly boleto = signal<Boleto | null>(null);
  protected readonly fieldError = signal('');
  protected readonly showResult = signal(false);
  protected readonly doneAt = signal('');

  protected readonly amount = computed(() => parseAmountInput(this.text()));
  protected readonly boletoAmount = computed(() => formatBrl(this.boleto()?.amount ?? 0));
  protected readonly lines = computed<ReceiptLine[]>(() => [
    { label: 'Amount', value: this.boletoAmount() },
    { label: 'To', value: 'Your account' },
  ]);
  protected readonly uncertainMessage = UNCERTAIN_MESSAGE;

  constructor() {
    this.account.load();
    effect(() => {
      this.text(); // typing again clears the error the server gave about the previous attempt
      untracked(() => this.fieldError.set(''));
    });
    effect(() => {
      const phase = this.operation.phase();
      const error = this.operation.error();
      untracked(() => {
        if (phase === 'pending' || phase === 'idle') return;
        if (phase === 'business' && error?.status === 404) {
          // the account was not found: a field-level problem, back to the form with what was typed
          this.operation.reset();
          this.boleto.set(null);
          this.fieldError.set(error.message);
          return;
        }
        this.showResult.set(true);
        if (phase === 'success') {
          this.doneAt.set(new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }));
          this.account.load(); // the balance on screen is always the API's, never a local calculation
        }
      });
    });
  }

  protected generate(): void {
    const amount = this.amount();
    if (!amount) return;
    this.boleto.set(buildBoleto(amount, this.session.user()?.name ?? 'Account holder', new Date()));
  }

  protected newAmount(): void {
    this.boleto.set(null);
  }

  protected demoAction(): void {
    this.toast.show('Demo action');
  }

  protected pay(): void {
    const boleto = this.boleto();
    const account = this.account.account();
    if (!boleto || !account) return;
    this.operation.run((key) => this.api.deposit({ agency: account.agency, account: account.number }, boleto.amount, key));
  }

  protected tryAgain(): void {
    this.operation.retry();
  }

  /** Business failure: back to the boleto, nothing lost. */
  protected edit(): void {
    this.operation.reset();
    this.showResult.set(false);
  }

  protected another(): void {
    this.text.set('');
    this.boleto.set(null);
    this.edit();
  }
}
