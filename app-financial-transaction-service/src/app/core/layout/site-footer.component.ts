import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { TagComponent } from '../../shared/ui/tag.component';

interface ProfileLink {
  readonly label: string;
  readonly handle: string;
  readonly href: string;
  readonly icon: string;
}

export const DEVELOPER = {
  name: 'Dowglas Maia',
  role: 'Senior Java Engineer',
  initials: 'DM',
  /** 192 px square avatar served from `public/img` (the original 2.4 MB picture is not shipped). */
  photo: 'img/dowglas.png',
} as const;

export const PROFILE_LINKS: readonly ProfileLink[] = [
  { label: 'LinkedIn', handle: 'in/dowglasmaia', href: 'https://www.linkedin.com/in/dowglasmaia/', icon: 'ph ph-linkedin-logo' },
  { label: 'GitHub', handle: 'dowglasmaia', href: 'https://github.com/dowglasmaia', icon: 'ph ph-github-logo' },
  { label: 'Malt', handle: 'dowglasmaia', href: 'https://www.malt.com/profile/dowglasmaia', icon: 'ph ph-briefcase' },
  { label: 'Website', handle: 'www.dowglasmaia.com', href: 'https://www.dowglasmaia.com', icon: 'ph ph-globe' },
];

/** What this prototype is built with, for the "references" column. */
export const STACK: readonly string[] = ['Angular 21', 'Tailwind CSS 4', 'Java 25', 'Spring Boot', 'PostgreSQL', 'Kafka', 'OpenAPI'];

/** Portfolio credit footer (same layout as the check-in prototype): author, profiles, stack and notices. */
@Component({
  selector: 'app-site-footer',
  imports: [TagComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block print:hidden' },
  template: `
    <footer class="mx-auto w-full max-w-[1200px] px-5 pt-4 pb-8 sm:px-12">
      <div class="h-px w-full bg-linear-to-r from-transparent via-accent/50 to-transparent" aria-hidden="true"></div>

      <div class="grid gap-8 py-8 md:grid-cols-[1.2fr_1fr_1fr] md:gap-10">
        <section aria-labelledby="footer-author" class="flex items-start gap-4">
          @if (photoFailed()) {
            <span
              class="grid size-12 shrink-0 place-items-center rounded-full border border-accent bg-accent/10 text-base font-medium text-accent shadow-[0_0_22px_color-mix(in_srgb,var(--accent)_30%,transparent)]"
              aria-hidden="true"
            >
              {{ developer.initials }}
            </span>
          } @else {
            <img
              [src]="developer.photo"
              alt="Photo of {{ developer.name }}"
              width="48"
              height="48"
              loading="lazy"
              class="size-12 shrink-0 rounded-full border border-accent object-cover shadow-[0_0_22px_color-mix(in_srgb,var(--accent)_30%,transparent)]"
              (error)="photoFailed.set(true)"
            />
          }
          <div class="min-w-0">
            <p class="text-[11px] tracking-[0.14em] text-content-muted uppercase">Designed &amp; developed by</p>
            <h2 id="footer-author" class="mt-1 font-display text-lg font-medium tracking-tight">{{ developer.name }}</h2>
            <p class="text-sm text-accent">{{ developer.role }}</p>
            <p class="mt-3 max-w-[38ch] text-[13px] leading-relaxed text-content-muted">
              A full-stack portfolio prototype: a Spring Boot API and this Angular app, built contract-first and covered by
              automated tests.
            </p>
          </div>
        </section>

        <nav aria-labelledby="footer-profiles">
          <h2 id="footer-profiles" class="text-[11px] tracking-[0.14em] text-content-muted uppercase">Find me</h2>
          <ul class="mt-3 flex flex-col gap-2">
            @for (link of links; track link.label) {
              <li>
                <a
                  [href]="link.href"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="group flex items-center gap-3 rounded-md border border-line px-3 py-2 text-sm text-content no-underline transition-colors hover:border-accent/60 hover:bg-accent/10 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
                >
                  <i [class]="link.icon + ' text-lg text-accent'" aria-hidden="true"></i>
                  <span class="flex min-w-0 flex-1 flex-col leading-tight">
                    <span class="font-medium">{{ link.label }}</span>
                    <span class="truncate text-xs text-content-muted">{{ link.handle }}</span>
                  </span>
                  <i
                    class="ph ph-arrow-up-right text-content-muted transition-transform group-hover:-translate-y-0.5 group-hover:translate-x-0.5 group-hover:text-accent"
                    aria-hidden="true"
                  ></i>
                  <span class="sr-only">(opens in a new tab)</span>
                </a>
              </li>
            }
          </ul>
        </nav>

        <section aria-labelledby="footer-stack">
          <h2 id="footer-stack" class="text-[11px] tracking-[0.14em] text-content-muted uppercase">Built with</h2>
          <ul class="mt-3 flex flex-wrap gap-2">
            @for (item of stack; track item) {
              <li>
                <app-tag tone="outline">{{ item }}</app-tag>
              </li>
            }
          </ul>
          <p class="mt-4 flex gap-2 text-xs leading-relaxed text-content-muted">
            <i class="ph ph-flask mt-0.5 shrink-0" aria-hidden="true"></i>
            <span>All names, accounts and amounts in this app are fictitious.</span>
          </p>
        </section>
      </div>

      <div class="flex flex-col gap-1 border-t border-line pt-4 text-xs text-content-muted sm:flex-row sm:items-center sm:justify-between">
        <p>© {{ year }} {{ developer.name }}. Maia Bank is a demo product, not a real bank. Demo environment · test data only.</p>
        <p>Times are shown in your device's time zone.</p>
      </div>
    </footer>
  `,
})
export class SiteFooterComponent {
  protected readonly developer = DEVELOPER;
  protected readonly links = PROFILE_LINKS;
  protected readonly stack = STACK;
  protected readonly year = new Date().getFullYear();
  protected readonly photoFailed = signal(false);
}
