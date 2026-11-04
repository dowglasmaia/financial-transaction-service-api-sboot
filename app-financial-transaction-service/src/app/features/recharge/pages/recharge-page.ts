import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
  untracked,
} from '@angular/core';
import { OperationController } from '../../../shared/operation/operation';
import { ButtonDirective } from '../../../shared/ui/button.directive';
import { IconTileComponent } from '../../../shared/ui/icon-tile.component';
import { RechargeFormComponent } from '../../../shared/ui/recharge-form.component';
import { ResultPanelComponent } from '../../../shared/ui/result-panel.component';
import { ReceiptLine, ReviewDialogComponent } from '../../../shared/ui/review-dialog.component';
import { CarrierId, carrierLabel } from '../../../shared/util/carriers';
import { formatBrl, parseAmountInput } from '../../../shared/util/money';
import { maskPhone, splitPhone } from '../../../shared/util/phone';
import { AccountStore } from '../../account/data-access/account.store';
import { RechargeApi, RechargeTarget } from '../data-access/recharge.api';

const UNCERTAIN_MESSAGE =
  "We couldn't confirm the operation. Check your transactions before trying again, so you don't pay twice.";

/** What the Home quick-recharge card hands over (router state): the same three fields of the same form. */
interface RechargeSeed {
  readonly carrier: CarrierId;
  readonly phone: string;
  readonly amount: string;
}

/**
 * Recharge a mobile phone. The debit and the ledger row are immediate; the carrier credit is asynchronous (an event on
 * Kafka through the transactional outbox), so the page only ever says the recharge was REQUESTED.
 */
@Component({
  selector: 'app-recharge-page',
  templateUrl: './recharge-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ButtonDirective,
    IconTileComponent,
    RechargeFormComponent,
    ResultPanelComponent,
    ReviewDialogComponent,
  ],
})
export class RechargePage {
  private readonly api = inject(RechargeApi);
  protected readonly account = inject(AccountStore);
  protected readonly operation = new OperationController();

  protected readonly carrier = signal<CarrierId | null>(null);
  protected readonly phone = signal('');
  protected readonly amountText = signal('');
  protected readonly reviewOpen = signal(false);
  protected readonly showResult = signal(false);
  protected readonly doneAt = signal('');

  protected readonly balance = computed(() => this.account.account()?.balance ?? null);
  protected readonly formattedBalance = computed(() => formatBrl(this.balance() ?? 0));
  protected readonly amount = computed(() => parseAmountInput(this.amountText()));
  protected readonly target = computed<RechargeTarget | null>(() => {
    const carrier = this.carrier();
    const parts = splitPhone(this.phone());
    return carrier && parts ? { ...parts, carrier } : null;
  });
  protected readonly lines = computed<ReceiptLine[]>(() => [
    { label: 'Carrier', value: carrierLabel(this.carrier()) },
    { label: 'Mobile number', value: this.phone() },
    { label: 'Amount', value: formatBrl(this.amount() ?? 0) },
  ]);
  protected readonly successMessage =
    'Recharge requested. Your carrier will apply the credit shortly.';
  protected readonly uncertainMessage = UNCERTAIN_MESSAGE;

  constructor() {
    this.account.load();
    this.takeSeedFromHome();
    effect(() => {
      const phase = this.operation.phase();
      untracked(() => {
        if (phase === 'pending' || phase === 'idle') return; // idle at start must not close a review opened from Home
        this.reviewOpen.set(false);
        this.showResult.set(true);
        if (phase === 'success') {
          this.doneAt.set(
            new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
          );
          this.account.load(); // the balance on screen is always the API's, never a local calculation
        }
      });
    });
  }

  /** Arriving from the Home card: fill the same form and open the same review (nothing is sent). */
  private takeSeedFromHome(): void {
    const seed = (history.state as { recharge?: RechargeSeed } | null)?.recharge;
    if (!seed) return;
    this.carrier.set(seed.carrier);
    this.phone.set(maskPhone(seed.phone));
    this.amountText.set(seed.amount);
    if (this.target() && this.amount()) this.reviewOpen.set(true);
  }

  protected openReview(): void {
    if (this.target() && this.amount()) this.reviewOpen.set(true);
  }

  protected confirm(): void {
    const target = this.target();
    const amount = this.amount();
    if (!target || !amount) return;
    this.operation.run((key) => this.api.recharge(target, amount, key));
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
    this.phone.set('');
    this.amountText.set('');
    this.carrier.set(null);
    this.edit();
  }
}
