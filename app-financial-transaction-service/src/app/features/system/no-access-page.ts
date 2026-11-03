import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { SessionService } from '../../core/auth/session.service';
import { ButtonDirective } from '../../shared/ui/button.directive';
import { IconTileComponent } from '../../shared/ui/icon-tile.component';

/** The sign-in worked but the identity is not linked to a Maia Bank customer (the API answered 403). */
@Component({
  selector: 'app-no-access-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ButtonDirective, IconTileComponent],
  template: `
    <section class="mx-auto grid max-w-md justify-items-center gap-4 rounded-panel border border-line bg-glass p-8 text-center shadow-card" aria-labelledby="no-access-title">
      <app-icon-tile icon="ph-bold ph-lock-key" tone="warning" size="lg" />
      <h1 id="no-access-title" class="font-display text-2xl font-bold">No access to this app yet</h1>
      <p class="text-content-muted">
        You signed in, but this identity is not linked to a Maia Bank customer. Ask the project owner to link it, or sign out and use another account.
      </p>
      <button appButton type="button" (click)="session.logout()">Sign out</button>
    </section>
  `,
})
export class NoAccessPage {
  protected readonly session = inject(SessionService);
}
