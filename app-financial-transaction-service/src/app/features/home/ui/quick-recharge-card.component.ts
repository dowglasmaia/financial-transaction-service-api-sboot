import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ButtonDirective } from '../../../shared/ui/button.directive';
import { IconTileComponent } from '../../../shared/ui/icon-tile.component';
import { RechargeFormComponent } from '../../../shared/ui/recharge-form.component';
import { CarrierId } from '../../../shared/util/carriers';
import { parseAmountInput } from '../../../shared/util/money';
import { splitPhone } from '../../../shared/util/phone';

/**
 * The Home quick-recharge card: the SAME form as the Recharge page. "Review recharge" hands what was typed to the Recharge
 * page, which opens the same review dialog; nothing is sent from Home.
 */
@Component({
  selector: 'app-quick-recharge-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ButtonDirective, IconTileComponent, RechargeFormComponent],
  template: `
    <section
      class="grid gap-5 rounded-panel border border-line bg-glass p-6 shadow-card"
      aria-label="Quick recharge"
    >
      <div class="flex items-center gap-3">
        <app-icon-tile icon="ph-bold ph-device-mobile" tone="violet" />
        <h2 class="font-display text-lg font-semibold">Quick recharge</h2>
      </div>
      <app-recharge-form
        [(carrier)]="carrier"
        [(phone)]="phone"
        [(amountText)]="amountText"
        [balance]="balance()"
      />
      <button appButton type="button" [disabled]="!ready()" (click)="review()">
        Review recharge
      </button>
    </section>
  `,
})
export class QuickRechargeCardComponent {
  private readonly router = inject(Router);

  readonly balance = input<number | null>(null);

  protected readonly carrier = signal<CarrierId | null>(null);
  protected readonly phone = signal('');
  protected readonly amountText = signal('');
  protected readonly ready = computed(
    () =>
      this.carrier() !== null &&
      splitPhone(this.phone()) !== null &&
      parseAmountInput(this.amountText()) !== null,
  );

  protected review(): void {
    const amount = parseAmountInput(this.amountText());
    if (!this.carrier() || !splitPhone(this.phone()) || !amount) return;
    void this.router.navigateByUrl('/recharge', {
      state: { recharge: { carrier: this.carrier(), phone: this.phone(), amount } },
    });
  }
}
