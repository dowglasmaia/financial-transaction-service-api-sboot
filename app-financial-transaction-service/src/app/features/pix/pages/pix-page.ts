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
import { AmountPresetsComponent } from '../../../shared/ui/amount-presets.component';
import { ButtonDirective } from '../../../shared/ui/button.directive';
import { FieldControlDirective, FormFieldComponent } from '../../../shared/ui/form-field';
import { IconTileComponent } from '../../../shared/ui/icon-tile.component';
import { ResultPanelComponent } from '../../../shared/ui/result-panel.component';
import { ReceiptLine } from '../../../shared/ui/review-dialog.component';
import { SegmentedComponent } from '../../../shared/ui/segmented.component';
import { StepperComponent } from '../../../shared/ui/stepper.component';
import { formatBrl, parseAmountInput } from '../../../shared/util/money';
import {
  PIX_KEY_KINDS,
  PixKeyKind,
  maskPixInput,
  maskPixKeyForDisplay,
  pixKeyFeedback,
} from '../../../shared/util/pix-key';
import { AccountStore } from '../../account/data-access/account.store';
import { PixApi } from '../data-access/pix.api';

const UNCERTAIN_MESSAGE =
  "We couldn't confirm the operation. Check your transactions before trying again, so you don't pay twice.";
const NOT_SENT_MESSAGE = "We couldn't send this Pix. Your balance was not changed.";

/**
 * Send a Pix by key in three steps (Key, Amount, Review) and then one of three results. The client rules only give
 * early feedback (the server decides). The key is held only in this component, is masked in the review and the receipt,
 * and is never logged or put in a URL.
 */
@Component({
  selector: 'app-pix-page',
  templateUrl: './pix-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AmountPresetsComponent,
    ButtonDirective,
    FieldControlDirective,
    FormFieldComponent,
    IconTileComponent,
    ResultPanelComponent,
    SegmentedComponent,
    StepperComponent,
  ],
})
export class PixPage {
  private readonly api = inject(PixApi);
  protected readonly account = inject(AccountStore);
  protected readonly operation = new OperationController();

  protected readonly kinds = PIX_KEY_KINDS;
  protected readonly steps = ['Key', 'Amount', 'Review'] as const;
  protected readonly presets = [50, 100, 200, 500] as const;
  protected readonly step = signal(1);
  protected readonly kind = signal<PixKeyKind>('cpf');
  protected readonly keyText = signal('');
  protected readonly amountText = signal('');
  protected readonly showResult = signal(false);
  protected readonly doneAt = signal('');

  protected readonly balance = computed(() => this.account.account()?.balance ?? null);
  protected readonly formattedBalance = computed(() => formatBrl(this.balance() ?? 0));
  protected readonly keyFeedback = computed(() => pixKeyFeedback(this.kind(), this.keyText()));
  protected readonly kindLabel = computed(
    () => this.kinds.find((kind) => kind.value === this.kind())!.label,
  );
  protected readonly amount = computed(() => parseAmountInput(this.amountText()));
  protected readonly amountOk = computed(() => {
    const amount = this.amount();
    const balance = this.balance();
    return amount !== null && (balance === null || Number(amount) <= balance);
  });
  protected readonly lines = computed<ReceiptLine[]>(() => {
    const key = this.keyFeedback().key;
    return [
      { label: 'Key type', value: this.kindLabel() },
      { label: 'Pix key', value: key ? maskPixKeyForDisplay(this.kind(), key) : '' },
      { label: 'Amount', value: formatBrl(this.amount() ?? 0) },
      { label: 'From', value: 'Your account' },
    ];
  });
  protected readonly notSentMessage = NOT_SENT_MESSAGE;
  protected readonly uncertainMessage = UNCERTAIN_MESSAGE;

  constructor() {
    this.account.load();
    effect(() => {
      const phase = this.operation.phase();
      untracked(() => {
        if (phase === 'pending' || phase === 'idle') return;
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

  protected selectKind(value: string): void {
    this.kind.set(value as PixKeyKind);
    this.keyText.set(''); // a key of another type is another key
  }

  /** The field shows the masked text; it is rewritten even when the signal does not change (digits past the end). */
  protected typeKey(event: Event): void {
    const input = event.target as HTMLInputElement;
    const masked = maskPixInput(this.kind(), input.value);
    input.value = masked;
    this.keyText.set(masked);
  }

  protected next(): void {
    if (this.step() === 1 && this.keyFeedback().key) this.step.set(2);
    else if (this.step() === 2 && this.amountOk()) this.step.set(3);
  }

  protected back(): void {
    if (this.step() > 1) this.step.update((step) => step - 1);
  }

  protected confirm(): void {
    const key = this.keyFeedback().key;
    const amount = this.amount();
    if (!key || !amount) return;
    const kind = this.kind();
    this.operation.run((idempotencyKey) => this.api.send(kind, key, amount, idempotencyKey));
  }

  protected tryAgain(): void {
    this.operation.retry();
  }

  /** Business failure: back to the review with everything still filled. */
  protected edit(): void {
    this.operation.reset();
    this.showResult.set(false);
  }

  protected another(): void {
    this.keyText.set('');
    this.amountText.set('');
    this.step.set(1);
    this.edit();
  }
}
