import { Directive, computed, input } from '@angular/core';

export type ButtonVariant = 'primary' | 'secondary' | 'ghost';

const BASE =
  'relative inline-flex h-12 items-center justify-center gap-2 rounded-control px-5 text-[15px] font-semibold ' +
  'transition duration-150 ease-standard select-none focus-visible:outline-2 focus-visible:outline-offset-2 ' +
  'focus-visible:outline-accent disabled:cursor-not-allowed disabled:opacity-45 aria-disabled:cursor-not-allowed aria-disabled:opacity-45 ' +
  'active:scale-[0.98] motion-reduce:transition-none motion-reduce:active:scale-100';

const VARIANTS: Record<ButtonVariant, string> = {
  primary:
    'bg-linear-to-br from-accent to-accent-2 text-on-accent shadow-glow hover:brightness-110 ' +
    'after:border-on-accent',
  secondary: 'border border-line-input bg-transparent text-content hover:bg-surface-sunken after:border-content',
  ghost: 'bg-transparent text-content-muted hover:bg-surface-sunken hover:text-content after:border-content',
};

const LOADING =
  'pointer-events-none cursor-progress text-transparent after:absolute after:size-4 after:animate-spin ' +
  'after:rounded-full after:border-2 after:border-t-transparent after:content-[""]';

/** Themed button. Loading keeps the width, shows a spinner and is announced through `aria-busy`. */
@Directive({
  selector: 'button[appButton], a[appButton]',
  host: {
    '[class]': 'classes()',
    '[attr.aria-busy]': 'loading() ? true : null',
    '[attr.aria-disabled]': 'loading() ? true : null',
  },
})
export class ButtonDirective {
  readonly variant = input<ButtonVariant>('primary');
  readonly loading = input(false);

  protected readonly classes = computed(() => [BASE, VARIANTS[this.variant()], this.loading() ? LOADING : ''].join(' '));
}
