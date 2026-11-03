import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonDirective } from '../../shared/ui/button.directive';
import { IconTileComponent } from '../../shared/ui/icon-tile.component';

@Component({
  selector: 'app-not-found-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ButtonDirective, IconTileComponent],
  template: `
    <section class="mx-auto grid max-w-md justify-items-center gap-4 rounded-panel border border-line bg-glass p-8 text-center shadow-card" aria-labelledby="not-found-title">
      <app-icon-tile icon="ph-bold ph-compass" tone="accent" size="lg" />
      <h1 id="not-found-title" class="font-display text-2xl font-bold">Page not found</h1>
      <p class="text-content-muted">The page you are looking for does not exist or has moved.</p>
      <a appButton routerLink="/">Back to the start</a>
    </section>
  `,
})
export class NotFoundPage {}
