import { ChangeDetectionStrategy, Component, computed, input, model, signal } from '@angular/core';
import { CARRIERS, CarrierId } from '../util/carriers';
import { maskPhone, splitPhone } from '../util/phone';
import { AmountPresetsComponent } from './amount-presets.component';
import { FieldControlDirective, FormFieldComponent } from './form-field';

/**
 * The recharge form (carrier cards, masked mobile number, amount with proposed values), shared by the Recharge page and the
 * Home quick-recharge card. It only collects; the host owns the review dialog and the request. The early feedback
 * (number rule, amount) never blocks anything by itself: the server decides.
 */
@Component({
  selector: 'app-recharge-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AmountPresetsComponent, FieldControlDirective, FormFieldComponent],
  template: `
    <div class="grid gap-5">
      <div>
        <p id="carrier-label" class="mb-2 text-sm font-semibold">Carrier</p>
        <div class="grid grid-cols-4 gap-2" role="group" aria-labelledby="carrier-label">
          @for (option of carriers; track option.id) {
            <button
              type="button"
              class="relative h-[58px] rounded-control font-display text-base font-extrabold text-white transition focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent motion-reduce:transition-none"
              [class]="
                option.classes +
                (carrier() === option.id ? ' outline-[3px] outline-offset-2 outline-accent' : '')
              "
              [attr.aria-pressed]="carrier() === option.id"
              (click)="carrier.set(option.id)"
            >
              {{ option.label }}
              @if (carrier() === option.id) {
                <i
                  class="ph-bold ph-check-circle absolute top-1 right-1 text-sm"
                  aria-hidden="true"
                ></i>
              }
            </button>
          }
        </div>
      </div>

      <app-form-field label="Mobile number" [error]="phoneError()">
        <input
          appFieldControl
          type="tel"
          inputmode="tel"
          autocomplete="off"
          placeholder="(11) 9 1234-5678"
          [value]="phone()"
          (input)="phone.set(mask($event))"
          (blur)="touched.set(true)"
        />
      </app-form-field>

      <app-amount-presets
        label="Recharge amount"
        [(value)]="amountText"
        [balance]="balance()"
        [fullBalance]="false"
        [presets]="presets"
      />
    </div>
  `,
})
export class RechargeFormComponent {
  readonly carrier = model<CarrierId | null>(null);
  /** The masked number (`(11) 9 1234-5678`). */
  readonly phone = model('');
  readonly amountText = model('');
  readonly balance = input<number | null>(null);

  protected readonly carriers = CARRIERS;
  protected readonly presets = [15, 30, 50, 100] as const;
  protected readonly touched = signal(false);
  protected readonly phoneError = computed(() => {
    const value = this.phone();
    if (value === '' || splitPhone(value)) return '';
    const complete = value.replace(/\D/g, '').length === 11;
    return complete || this.touched()
      ? 'Enter a valid mobile number, for example (11) 9 1234-5678.'
      : '';
  });

  protected mask(event: Event): string {
    const input = event.target as HTMLInputElement;
    const masked = maskPhone(input.value);
    input.value = masked; // the signal may not change (digits past the 11th), so the field is rewritten here too
    return masked;
  }
}
