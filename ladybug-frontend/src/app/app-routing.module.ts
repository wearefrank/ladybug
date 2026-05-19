import { inject, Injectable, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, DetachedRouteHandle, RouteReuseStrategy, RouterModule, Routes } from '@angular/router';
import { DebugComponent } from './debug/debug.component';
import { TestComponent } from './test/test.component';
import { CompareComponent } from './compare/compare.component';
import { TabService } from './shared/services/tab.service';
import { ReportComponent } from './report/report.component';

export const routes: Routes = [
  {
    component: DebugComponent,
    path: DebugComponent.ROUTER_PATH,
    pathMatch: 'full',
  },
  {
    component: TestComponent,
    path: TestComponent.ROUTER_PATH,
  },
  {
    component: ReportComponent,
    path: `${ReportComponent.ROUTER_PATH}/:id`,
  },
  {
    component: CompareComponent,
    path: `${CompareComponent.ROUTER_PATH}/:id`,
  },
  {
    path: '',
    redirectTo: 'debug',
    pathMatch: 'full',
  },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}

@Injectable({
  providedIn: 'root',
})
export class AppRouteReuseStrategy implements RouteReuseStrategy {
  storedRoutes: Record<string, DetachedRouteHandle> = {};
  private tabService = inject(TabService);

  constructor() {
    this.tabService.closeTab$.subscribe((closeTab) => {
      const pathPrefix = closeTab.type === 'report' ? 'report' : 'compare';
      const routePath = `${pathPrefix}/${closeTab.id}`;

      delete this.storedRoutes[routePath];
    });
  }

  shouldDetach(_route: ActivatedRouteSnapshot): boolean {
    return true;
  }

  store(route: ActivatedRouteSnapshot, handle: DetachedRouteHandle | null): void {
    if (route.routeConfig && handle) {
      const key = this.pathOf(route);
      this.storedRoutes[key] = handle;
    }
  }

  shouldAttach(route: ActivatedRouteSnapshot): boolean {
    const key: string = this.pathOf(route);
    return this.storedRoutes[key] !== undefined;
  }

  retrieve(route: ActivatedRouteSnapshot): DetachedRouteHandle | null {
    const key: string = this.pathOf(route);
    return this.storedRoutes[key];
  }

  shouldReuseRoute(future: ActivatedRouteSnapshot, current: ActivatedRouteSnapshot): boolean {
    return future.routeConfig === current.routeConfig && future.params['id'] === current.params['id'];
  }

  private pathOf(route: ActivatedRouteSnapshot): string {
    let result: string;
    const path = route.routeConfig?.path || '';
    if (this.isReportOrCompare(path)) {
      const firstComponent = path.split('/')[0];
      result = `${firstComponent}/${this.getRouteId(route)}`;
    } else {
      result = path;
    }
    return result;
  }

  private isReportOrCompare(path: string): boolean {
    return path.startsWith('report') || path.startsWith('compare');
  }

  private getRouteId(route: ActivatedRouteSnapshot): string {
    const id: string = route.params['id'] || '';
    return id;
  }
}
