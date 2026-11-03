import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { IconTileComponent } from './icon-tile.component';

/** Empty result with one clear next action (project it into the default slot). */
@Component({
  selector: 'app-empty-state',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IconTileComponent],
  template: `
    <div class="flex flex-col items-start gap-3 rounded-card border border-dashed border-line-input bg-glass p-8 shadow-card">
      <app-icon-tile [icon]="icon()" tone="accent" size="lg" />
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
