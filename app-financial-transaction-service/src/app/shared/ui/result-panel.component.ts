import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonDirective } from './button.directive';
import type { ReceiptLine } from './review-dialog.component';

export type ResultKind = 'success' | 'business' | 'uncertain';

const LOOK: Record<ResultKind, { icon: string; badge: string; ring: string }> = {
  success: { icon: 'ph-bold ph-check', badge: 'bg-income text-on-accent', ring: 'border-income' },
  business: { icon: 'ph-bold ph-x', badge: 'bg-expense text-on-accent', ring: 'border-expense' },
  uncertain: { icon: 'ph-bold ph-warning', badge: 'bg-warning text-on-accent', ring: 'border-warning' },
};

/**
 * The result of a money operation, in its three outcomes (shared by Withdraw, Deposit, Recharge and Pix):
 * - `success`: the operation was registered; actions: View transactions, a new operation, Back to home;
 * - `business`: the server refused it and nothing moved; action: edit and try again;
 * - `uncertain`: no answer, so the outcome is unknown; actions: View transactions (check first) and an explicit Try again,
 *   which the page answers by repeating the SAME request with the SAME Idempotency-Key.
 *
 * Copy is the page's (`title`, `message`); the panel never invents a claim about the operation. Icon and ring animate
 * once (not with reduced motion); colour is never the only cue (icon + title).
 */
@Component({
  selector: 'app-result-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ButtonDirective],
  template: `
    <div class="mx-auto grid w-full max-w-xl gap-4" role="status">
      <div class="grid justify-items-center gap-3 rounded-panel border border-line bg-glass p-8 text-center shadow-card">
        <span class="relative grid place-items-center" aria-hidden="true">
          <span class="absolute size-16 animate-[ring_1.1s_ease-out_1] rounded-full border-2 motion-reduce:animate-none" [class]="look().ring"></span>
          <span class="grid size-16 animate-[pop_0.5s_ease-out_both] place-items-center rounded-full text-3xl motion-reduce:animate-none" [class]="look().badge">
            <i [class]="look().icon"></i>
          </span>
        </span>
        <h1 class="font-display text-2xl font-bold">{{ title() }}</h1>
        <p class="max-w-sm text-[15px] text-content-muted">{{ message() }}</p>

        @if (lines().length > 0) {
          <dl class="w-full divide-y divide-line rounded-control border border-line bg-surface-sunken px-4 text-left">
            @for (line of lines(); track line.label) {
              <div class="flex justify-between gap-3 py-2 text-[15px]">
                <dt class="text-content-muted">{{ line.label }}</dt>
                <dd class="font-bold tabular-nums">{{ line.value }}</dd>
              </div>
            }
          </dl>
        }
        @if (note()) {
          <p class="text-xs text-content-muted">{{ note() }}</p>
        }
      </div>

      <div class="grid gap-2.5">
        @switch (kind()) {
          @case ('success') {
            <a appButton routerLink="/transactions">View transactions</a>
            <button appButton variant="secondary" type="button" (click)="again.emit()">{{ againLabel() }}</button>
            <a appButton variant="ghost" routerLink="/home">Back to home</a>
          }
          @case ('business') {
            <button appButton type="button" (click)="edit.emit()">Edit and try again</button>
            <a appButton variant="ghost" routerLink="/home">Back to home</a>
          }
          @case ('uncertain') {
            <a appButton routerLink="/transactions">View transactions</a>
            <button appButton variant="secondary" type="button" [loading]="busy()" (click)="retry.emit()">Try again</button>
            <a appButton variant="ghost" routerLink="/home">Back to home</a>
          }
        }
      </div>
    </div>
  `,
})
export class ResultPanelComponent {
  readonly kind = input.required<ResultKind>();
  readonly title = input.required<string>();
  readonly message = input.required<string>();
  readonly lines = input<readonly ReceiptLine[]>([]);
  /** A small print line, e.g. "Registered on this device at 10:32". */
  readonly note = input('');
  /** Label of the "start another one" action of the success outcome, e.g. "New withdrawal". */
  readonly againLabel = input('New operation');
  /** A request is in flight (the explicit retry): the retry action is inert so it cannot be sent twice. */
  readonly busy = input(false);

  readonly again = output<void>();
  readonly edit = output<void>();
  readonly retry = output<void>();

  protected readonly look = computed(() => LOOK[this.kind()]);
}
