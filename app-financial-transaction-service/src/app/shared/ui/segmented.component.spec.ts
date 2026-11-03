import { TestBed } from '@angular/core/testing';
import { SegmentedComponent } from './segmented.component';

describe('SegmentedComponent', () => {
  const options = [
    { value: 'cpf', label: 'CPF' },
    { value: 'email', label: 'E-mail' },
    { value: 'phone', label: 'Mobile' },
  ];

  async function render(value = 'cpf') {
    const fixture = TestBed.createComponent(SegmentedComponent);
    fixture.componentRef.setInput('label', 'Key type');
    fixture.componentRef.setInput('options', options);
    fixture.componentRef.setInput('value', value);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    const radios = () => Array.from(el.querySelectorAll<HTMLButtonElement>('[role="radio"]'));
    return { fixture, el, radios };
  }

  it('is a labelled radio group with one checked option', async () => {
    const { el, radios } = await render('email');
    expect(el.querySelector('[role="radiogroup"]')?.getAttribute('aria-label')).toBe('Key type');
    expect(radios().map((r) => r.getAttribute('aria-checked'))).toEqual(['false', 'true', 'false']);
  });

  it('only the checked option is in the tab order (roving tabindex)', async () => {
    const { radios } = await render('cpf');
    expect(radios().map((r) => r.tabIndex)).toEqual([0, -1, -1]);
  });

  it('selects an option on click', async () => {
    const { fixture, radios } = await render('cpf');
    radios()[2].click();
    await fixture.whenStable();
    expect(fixture.componentInstance.value()).toBe('phone');
    expect(radios()[2].getAttribute('aria-checked')).toBe('true');
  });

  it('moves with the arrow keys and wraps around', async () => {
    const { fixture, radios } = await render('phone');
    radios()[2].dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }));
    await fixture.whenStable();
    expect(fixture.componentInstance.value()).toBe('cpf');
    radios()[0].dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft', bubbles: true }));
    await fixture.whenStable();
    expect(fixture.componentInstance.value()).toBe('phone');
  });
});
