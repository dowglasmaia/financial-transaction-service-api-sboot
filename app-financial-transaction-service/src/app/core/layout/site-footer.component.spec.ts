import { TestBed } from '@angular/core/testing';
import { DEVELOPER, PROFILE_LINKS, STACK, SiteFooterComponent } from './site-footer.component';

describe('SiteFooterComponent', () => {
  async function render() {
    const fixture = TestBed.createComponent(SiteFooterComponent);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    return { el, text: el.textContent ?? '' };
  }

  it('credits the developer', async () => {
    const { text } = await render();
    expect(text).toContain('Dowglas Maia');
    expect(text).toContain('Senior Java Engineer');
    expect(DEVELOPER.initials).toBe('DM');
  });

  it.each([
    ['LinkedIn', 'https://www.linkedin.com/in/dowglasmaia/'],
    ['GitHub', 'https://github.com/dowglasmaia'],
    ['Malt', 'https://www.malt.com/profile/dowglasmaia'],
    ['Website', 'https://www.dowglasmaia.com'],
  ])('links to the %s profile', async (label, href) => {
    const { el } = await render();
    const link = Array.from(el.querySelectorAll('a')).find((a) => a.textContent?.includes(label));
    expect(link?.getAttribute('href')).toBe(href);
  });

  it('opens every external link in a new tab without leaking the opener', async () => {
    const { el } = await render();
    const links = Array.from(el.querySelectorAll('a'));
    expect(links).toHaveLength(PROFILE_LINKS.length);
    for (const link of links) {
      expect(link.getAttribute('target')).toBe('_blank');
      expect(link.getAttribute('rel')).toBe('noopener noreferrer');
      expect(link.textContent).toContain('(opens in a new tab)');
    }
  });

  it('lists the stack this prototype is built with', async () => {
    const { text } = await render();
    for (const item of STACK) expect(text).toContain(item);
    expect(STACK).toContain('Spring Boot');
    expect(STACK).toContain('Angular 21');
  });

  it('shows the current year, the demo notice and the fictitious-data notice', async () => {
    const { text } = await render();
    expect(text).toContain(`© ${new Date().getFullYear()} Dowglas Maia`);
    expect(text).toContain('demo product, not a real bank');
    expect(text).toContain('Demo environment · test data only');
    expect(text).toContain('fictitious');
    expect(text).toContain("device's time zone");
  });

  it('labels its sections for assistive technology', async () => {
    const { el } = await render();
    expect(el.querySelector('nav')?.getAttribute('aria-labelledby')).toBe('footer-profiles');
    expect(el.querySelectorAll('h2')).toHaveLength(3);
  });

  it("shows the author's photo with a text alternative", async () => {
    const { el } = await render();
    const photo = el.querySelector('img');
    expect(photo?.getAttribute('src')).toBe('img/dowglas.png');
    expect(photo?.getAttribute('alt')).toBe('Photo of Dowglas Maia');
    expect(photo?.getAttribute('width')).toBe('48');
    expect(photo?.getAttribute('height')).toBe('48');
  });

  it('falls back to the initials when the photo cannot be loaded', async () => {
    const fixture = TestBed.createComponent(SiteFooterComponent);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    el.querySelector('img')!.dispatchEvent(new Event('error'));
    await fixture.whenStable();

    expect(el.querySelector('img')).toBeNull();
    expect(el.textContent).toContain(DEVELOPER.initials);
  });

  it('is hidden when printing', async () => {
    const { el } = await render();
    expect(el.classList.contains('print:hidden')).toBe(true);
  });
});
