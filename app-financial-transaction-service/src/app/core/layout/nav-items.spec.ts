import { Routes } from '@angular/router';
import { navItemsFrom } from './nav-items';

describe('navItemsFrom', () => {
  const page = class {};

  it('lists only the routes that declare nav metadata, ordered', () => {
    const routes: Routes = [
      { path: 'pix', component: page, data: { nav: { label: 'Pix', icon: 'ph ph-diamonds-four', order: 3 } } },
      { path: '', component: page, data: { nav: { label: 'Home', icon: 'ph ph-house', order: 1 } } },
      { path: 'design', component: page },
      { path: 'transactions', component: page, data: { nav: { label: 'Transactions', icon: 'ph ph-arrows-left-right', order: 2 } } },
      { path: '**', redirectTo: '' },
    ];

    expect(navItemsFrom(routes).map((item) => [item.label, item.path])).toEqual([
      ['Home', '/'],
      ['Transactions', '/transactions'],
      ['Pix', '/pix'],
    ]);
  });

  it('ignores malformed metadata instead of throwing', () => {
    const routes: Routes = [
      { path: 'a', component: page, data: { nav: { label: 42 } } },
      { path: 'b', component: page, data: { nav: 'nope' } },
      { path: 'c', component: page, data: {} },
    ];
    expect(navItemsFrom(routes)).toEqual([]);
  });

  it('a slice adds its entry just by declaring it on its route', () => {
    const before = navItemsFrom([{ path: '', component: page, data: { nav: { label: 'Home', icon: 'i', order: 1 } } }]);
    const after = navItemsFrom([
      { path: '', component: page, data: { nav: { label: 'Home', icon: 'i', order: 1 } } },
      { path: 'withdraw', component: page, data: { nav: { label: 'Withdraw', icon: 'i', order: 6 } } },
    ]);
    expect(after.length - before.length).toBe(1);
  });
});
