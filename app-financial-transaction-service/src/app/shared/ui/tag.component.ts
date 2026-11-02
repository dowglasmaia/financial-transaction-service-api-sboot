import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export type TagTone = 'accent' | 'neutral' | 'outline';

const TONES: Record<TagTone, string> = {
  accent: 'border-accent/40 bg-accent/15 text-accent',
  neutral: 'border-transparent bg-surface-sunken text-content-muted',
  outline: 'border-line-input text-content-muted',
};

/** Small non-interactive pill (labels such as the stack list in the footer). */
@Component({
  selector: 'app-tag',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class]': 'classes()' },
  template: '<ng-content />',
})
export class TagComponent {
  readonly tone = input<TagTone>('neutral');

  protected readonly classes = computed(
    () => `inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs ${TONES[this.tone()]}`,
  );
}
