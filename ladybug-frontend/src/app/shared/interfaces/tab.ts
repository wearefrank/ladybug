import { DetachedRouteHandle } from '@angular/router';

export const KEY_DEBUG = 'debug';
export const KEY_TEST = 'test';
export const KEY_REPORT = 'report';
export const KEY_COMPARE = 'compare';

export type routeKind = 'debug' | 'test' | 'report' | 'compare';

export interface Tab {
  kind: routeKind;
  key: string;
  title: string;
  handle?: DetachedRouteHandle;
}
