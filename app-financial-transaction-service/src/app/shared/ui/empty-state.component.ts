import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Empty result with one clear next action (project it into the default slot). */
@Component({
  selector: 'app-empty-state',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex flex-col items-start gap-3 rounded-card border border-dashed border-line-input bg-surface-raised p-8">
      <i [class]="icon()" aria-hidden="true" class="text-3xl text-accent"></i>
      <h3 class="font-display text-lg font-semibold">{{ title() }}</h3>
      <p class="max-w-prose text-content-muted">{{ message() }}</p>
      <ng-content />
    </div>
  `,
})
export class EmptyStateComponent {
  readonly title = input.required<string>();
  readonly message = input('');
  readonly icon = input('ph-bold ph-tray');
}
