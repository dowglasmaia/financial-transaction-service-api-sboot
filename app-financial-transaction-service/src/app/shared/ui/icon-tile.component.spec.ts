import { TestBed } from '@angular/core/testing';
import { IconTileComponent } from './icon-tile.component';
import { TRANSACTION_TYPES } from './transaction-types';

describe('IconTileComponent', () => {
  async function render(tone: string) {
    const fixture = TestBed.createComponent(IconTileComponent);
    fixture.componentRef.setInput('icon', 'ph-bold ph-bank');
    fixture.componentRef.setInput('tone', tone);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('is decorative: hidden from assistive technology', async () => {
    const el = await render('income');
    expect(el.querySelector('[aria-hidden="true"]')).not.toBeNull();
  });

  it.each([
    ['income', 'text-income'],
    ['accent', 'text-accent'],
    ['violet', 'text-violet'],
    ['warning', 'text-warning'],
  ])('tone %s uses %s', async (tone, cls) => {
    const el = await render(tone);
    expect(el.innerHTML).toContain(cls);
  });

  it('every ledger kind has its own tile tone (colour is never the only cue: label, sign and icon come with it)', () => {
    expect(TRANSACTION_TYPES.DEPOSIT.tile).toBe('income');
    expect(TRANSACTION_TYPES.PIX.tile).toBe('accent');
    expect(TRANSACTION_TYPES.PHONE_RECHARGE.tile).toBe('violet');
    expect(TRANSACTION_TYPES.WITHDRAW.tile).toBe('warning');
  });
});
