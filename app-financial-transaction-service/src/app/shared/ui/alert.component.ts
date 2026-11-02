import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export type AlertTone = 'info' | 'success' | 'warning' | 'error';

const TONES: Record<AlertTone, { icon: string; box: string; role: 'status' | 'alert' }> = {
  info: { icon: 'ph-bold ph-info', box: 'border-line-input text-content', role: 'status' },
  success: { icon: 'ph-bold ph-check-circle', box: 'border-income text-content', role: 'status' },
  warning: { icon: 'ph-bold ph-warning', box: 'border-warning text-content', role: 'status' },
  error: { icon: 'ph-bold ph-warning-circle', box: 'border-expense text-content', role: 'alert' },
};

const ICON_COLOR: Record<AlertTone, string> = {
  info: 'text-content-muted',
  success: 'text-income',
  warning: 'text-warning',
  error: 'text-expense',
};

/** Inline message. Colour is never the only cue: every tone has its own icon and the text carries the meaning. */
@Component({
  selector: 'app-alert',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div [attr.role]="tone().role" class="flex items-start gap-3 rounded-card border bg-surface-raised p-4 shadow-card" [class]="tone().box">
      <i [class]="icon()" aria-hidden="true" class="mt-0.5 text-lg"></i>
      <div class="min-w-0 flex-1">
        @if (title()) {
          <p class="font-semibold">{{ title() }}</p>
        }
        <div class="text-[15px] text-content-muted"><ng-content /></div>
      </div>
    </div>
  `,
})
export class AlertComponent {
  readonly kind = input<AlertTone>('info');
  readonly title = input('');

  protected readonly tone = computed(() => TONES[this.kind()]);
  protected readonly icon = computed(() => `${TONES[this.kind()].icon} ${ICON_COLOR[this.kind()]}`);
}
