import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { APP_CONFIG } from '../config/app-config';
import { ThemeToggleComponent } from '../../shared/ui/theme-toggle.component';
import { SiteFooterComponent } from './site-footer.component';

/** Page frame: skip link, header (brand, demo chip, theme toggle), main landmark and the portfolio credit footer. */
@Component({
  selector: 'app-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, ThemeToggleComponent, SiteFooterComponent],
  template: `
    <a
      href="#main"
      class="sr-only focus:not-sr-only focus:fixed focus:top-3 focus:left-3 focus:z-50 focus:rounded-control focus:bg-accent focus:px-4 focus:py-2 focus:text-on-accent"
    >
      Skip to main content
    </a>

    <header class="sticky top-0 z-30 border-b border-line bg-brand/90 backdrop-blur">
      <div class="mx-auto flex h-16 w-full max-w-[1200px] items-center gap-3 px-4 sm:px-6">
        <a routerLink="/" class="flex items-center gap-2.5 rounded-control focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent">
          <span class="flex size-9 items-center justify-center rounded-control bg-linear-to-br from-accent to-accent-2 text-on-accent" aria-hidden="true">
            <i class="ph-bold ph-bank"></i>
          </span>
          <span class="font-display text-lg font-semibold text-on-brand">Maia Bank</span>
        </a>

        @if (config.demoBanner) {
          <span class="ml-1 hidden items-center gap-1.5 rounded-full border border-on-brand-muted/50 px-3 py-1 text-xs font-medium text-on-brand-muted sm:inline-flex">
            <i class="ph ph-flask" aria-hidden="true"></i>
            Demo environment · test data only
          </span>
        }

        <span class="flex-1"></span>
        <app-theme-toggle />
      </div>
    </header>

    <main id="main" class="mx-auto w-full max-w-[1200px] px-4 py-6 sm:px-6">
      <router-outlet />
    </main>

    <app-site-footer />
  `,
})
export class AppShellComponent {
  protected readonly config = inject(APP_CONFIG);
}
