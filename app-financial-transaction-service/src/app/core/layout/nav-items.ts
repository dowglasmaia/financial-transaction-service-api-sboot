import { Routes } from '@angular/router';

/** Navigation metadata a route declares in `data: { nav: ... }`. A slice adds its menu entry just by declaring it on its route. */
export interface NavMeta {
  readonly label: string;
  /** Phosphor classes, e.g. `ph ph-house`. */
  readonly icon: string;
  readonly order: number;
}

export interface NavItem extends NavMeta {
  readonly path: string;
}

function isNavMeta(value: unknown): value is NavMeta {
  if (typeof value !== 'object' || value === null) return false;
  const meta = value as Record<string, unknown>;
  return typeof meta['label'] === 'string' && typeof meta['icon'] === 'string' && typeof meta['order'] === 'number';
}

/** The menu entries of the top-level routes that declare nav metadata, ordered. Malformed metadata is ignored. */
export function navItemsFrom(routes: Routes): NavItem[] {
  return routes
    .flatMap((route) => {
      const nav = route.data?.['nav'];
      return route.path !== undefined && isNavMeta(nav) ? [{ ...nav, path: route.path === '' ? '/' : `/${route.path}` }] : [];
    })
    .sort((a, b) => a.order - b.order);
}
