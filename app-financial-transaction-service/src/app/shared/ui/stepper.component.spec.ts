import { TestBed } from '@angular/core/testing';
import { StepperComponent } from './stepper.component';

describe('StepperComponent', () => {
  async function render(current: number) {
    const fixture = TestBed.createComponent(StepperComponent);
    fixture.componentRef.setInput('steps', ['Key', 'Amount', 'Review']);
    fixture.componentRef.setInput('current', current);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    return { el, items: Array.from(el.querySelectorAll('li')) };
  }

  it('marks only the current step with aria-current', async () => {
    const { items } = await render(2);
    expect(items.map((li) => li.getAttribute('aria-current'))).toEqual([null, 'step', null]);
  });

  it('announces completed steps in text, not by colour alone', async () => {
    const { items } = await render(3);
    expect(items[0].textContent).toContain('completed');
    expect(items[1].textContent).toContain('completed');
    expect(items[2].textContent).not.toContain('completed');
  });

  it('exposes the progress as text for screen readers', async () => {
    const { el } = await render(2);
    expect(el.textContent).toContain('Step 2 of 3');
  });
});
