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
  private tabService = inject(TabService);

  shouldDetach(_: ActivatedRouteSnapshot): boolean {
    return true;
  }

  store(route: ActivatedRouteSnapshot, handle: DetachedRouteHandle | null): void {
    if (route.routeConfig && handle) {
      this.tabService.storeHandle(route, handle);
    }
  }

  shouldAttach(route: ActivatedRouteSnapshot): boolean {
    return this.tabService.getHandle(route) !== undefined;
  }

  retrieve(route: ActivatedRouteSnapshot): DetachedRouteHandle | null {
    return this.tabService.getHandle(route)!;
  }

  shouldReuseRoute(future: ActivatedRouteSnapshot, current: ActivatedRouteSnapshot): boolean {
    return this.tabService.getKey(future) === this.tabService.getKey(current);
  }
}
