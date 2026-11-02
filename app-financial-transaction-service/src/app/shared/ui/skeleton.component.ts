import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Placeholder shaped like the final content (reserves the space; no spinner). Hidden from assistive tech. */
@Component({
  selector: 'app-skeleton',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      aria-hidden="true"
      class="w-full animate-[shimmer_1.5s_linear_infinite] rounded-card border border-line bg-linear-to-r from-surface-raised via-surface-overlay to-surface-raised bg-[length:200%_100%] motion-reduce:animate-none"
      [style.height.px]="height()"
    ></div>
  `,
})
export class SkeletonComponent {
  readonly height = input(64);
}
