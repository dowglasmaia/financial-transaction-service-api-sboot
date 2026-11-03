import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { APP_CONFIG } from '../config/app-config';
import { DialogComponent } from '../../shared/ui/dialog.component';
import { ThemeToggleComponent } from '../../shared/ui/theme-toggle.component';
import { ToastHostComponent } from '../../shared/ui/toast-host.component';
import { navItemsFrom } from './nav-items';
import { SiteFooterComponent } from './site-footer.component';

const RAIL_ACTIVE = 'bg-linear-to-br from-primary to-primary-strong text-on-primary! shadow-glow';
const BOTTOM_VISIBLE = 3;

/**
 * Page frame (v2): skip link, left rail (labels on desktop, icons on tablet), bottom bar with a "More" dialog on phones,
 * top bar with the demo chip and theme toggle, main landmark and the portfolio credit footer.
 * Menu entries come from the route table (`data.nav`), so a slice only declares its route.
 */
@Component({
  selector: 'app-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ThemeToggleComponent, SiteFooterComponent, DialogComponent, ToastHostComponent],
  template: `
    <a
      href="#main"
      class="sr-only focus:not-sr-only focus:fixed focus:top-3 focus:left-3 focus:z-[60] focus:rounded-control focus:bg-primary focus:px-4 focus:py-2 focus:text-on-primary"
    >
      Skip to main content
    </a>

    <div class="min-h-screen md:flex">
      <aside class="hidden shrink-0 flex-col border-r border-line bg-brand px-3.5 py-5 md:sticky md:top-0 md:flex md:h-screen md:w-20 lg:w-64">
        <a routerLink="/" class="flex items-center gap-2.5 rounded-control px-1.5 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent md:justify-center lg:justify-start">
          <span class="flex size-10 shrink-0 items-center justify-center rounded-control bg-linear-to-br from-accent via-primary to-violet text-xl text-on-primary shadow-glow-accent" aria-hidden="true">
            <i class="ph-bold ph-bank"></i>
          </span>
          <span class="hidden font-display text-lg font-extrabold text-on-brand lg:inline">Maia Bank</span>
          <span class="sr-only md:inline lg:hidden">Maia Bank</span>
        </a>

        <nav aria-label="Main" class="mt-6 flex flex-col gap-1">
          @for (item of items; track item.path) {
            <a
              [routerLink]="item.path"
              [routerLinkActive]="railActive"
              [routerLinkActiveOptions]="{ exact: item.path === '/' }"
              ariaCurrentWhenActive="page"
              class="flex h-12 items-center gap-3.5 rounded-control px-4 text-[15px] font-semibold text-on-brand-muted transition hover:bg-white/10 hover:text-on-brand focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent md:justify-center lg:justify-start motion-reduce:transition-none"
            >
              <i [class]="item.icon" class="text-xl" aria-hidden="true"></i>
              <span class="md:sr-only lg:not-sr-only">{{ item.label }}</span>
            </a>
          }
        </nav>
      </aside>

      <div class="flex min-w-0 flex-1 flex-col">
        <header class="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-line bg-surface/80 px-4 backdrop-blur sm:px-6">
          <a routerLink="/" class="flex items-center gap-2 rounded-control focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent md:hidden">
            <span class="flex size-9 items-center justify-center rounded-control bg-linear-to-br from-accent via-primary to-violet text-on-primary" aria-hidden="true">
              <i class="ph-bold ph-bank"></i>
            </span>
            <span class="font-display text-lg font-extrabold">Maia Bank</span>
          </a>

          @if (config.demoBanner) {
            <span class="hidden items-center gap-1.5 rounded-full border border-line-input px-3 py-1 text-xs font-semibold text-content-muted sm:inline-flex">
              <i class="ph ph-flask" aria-hidden="true"></i>
              Demo environment · test data only
            </span>
          }

          <span class="flex-1"></span>
          <app-theme-toggle />
        </header>

        @if (config.demoBanner) {
          <p class="border-b border-line bg-surface-sunken px-4 py-1.5 text-center text-xs font-semibold text-content-muted sm:hidden">
            <i class="ph ph-flask" aria-hidden="true"></i>
            Demo environment · test data only
          </p>
        }

        <main id="main" class="mx-auto w-full max-w-[1200px] flex-1 px-4 py-6 pb-28 sm:px-6 md:pb-8">
          <router-outlet />
        </main>

        <app-site-footer />
      </div>
    </div>

    <nav aria-label="Quick navigation" class="fixed inset-x-0 bottom-0 z-40 flex border-t border-line bg-brand md:hidden">
      @for (item of bottomItems; track item.path) {
        <a
          [routerLink]="item.path"
          routerLinkActive="text-accent!"
          [routerLinkActiveOptions]="{ exact: item.path === '/' }"
          ariaCurrentWhenActive="page"
          class="flex min-h-[58px] flex-1 flex-col items-center justify-center gap-0.5 text-[11px] font-semibold text-on-brand-muted focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-accent"
        >
          <i [class]="item.icon" class="text-xl" aria-hidden="true"></i>
          {{ item.label }}
        </a>
      }
      @if (moreItems.length > 0) {
        <button
          type="button"
          class="flex min-h-[58px] flex-1 flex-col items-center justify-center gap-0.5 text-[11px] font-semibold text-on-brand-muted focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-accent"
          (click)="moreOpen.set(true)"
        >
          <i class="ph ph-dots-three-outline text-xl" aria-hidden="true"></i>
          More
        </button>
      }
    </nav>

    <app-dialog [(open)]="moreOpen" title="More">
      <div class="flex w-full flex-col gap-1">
        @for (item of moreItems; track item.path) {
          <a
            [routerLink]="item.path"
            class="flex h-12 items-center gap-3.5 rounded-control px-3 text-[15px] font-semibold hover:bg-surface-raised focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
            (click)="moreOpen.set(false)"
          >
            <i [class]="item.icon" class="text-xl text-accent" aria-hidden="true"></i>
            {{ item.label }}
          </a>
        }
      </div>
    </app-dialog>

    <app-toast-host />
  `,
})
export class AppShellComponent {
  protected readonly config = inject(APP_CONFIG);
  protected readonly railActive = RAIL_ACTIVE;
  protected readonly moreOpen = signal(false);

  protected readonly items = navItemsFrom(inject(Router).config);
  protected readonly bottomItems = this.items.length > BOTTOM_VISIBLE ? this.items.slice(0, BOTTOM_VISIBLE) : this.items;
  protected readonly moreItems = this.items.length > BOTTOM_VISIBLE ? this.items.slice(BOTTOM_VISIBLE) : [];
}
