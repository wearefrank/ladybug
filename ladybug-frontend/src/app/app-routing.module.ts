import { inject, Injectable, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, DetachedRouteHandle, RouteReuseStrategy, RouterModule, Routes } from '@angular/router';
import { DebugComponent } from './debug/debug.component';
import { TestComponent } from './test/test.component';
import { CompareComponent } from './compare/compare.component';
import { TabService } from './shared/services/tab.service';
import { ReportComponent } from './report/report.component';
import { KEY_COMPARE, KEY_DEBUG, KEY_REPORT, KEY_TEST } from './shared/interfaces/tab';
export const routes: Routes = [
  {
    component: DebugComponent,
    path: KEY_DEBUG,
    pathMatch: 'full',
  },
  {
    component: TestComponent,
    path: KEY_TEST,
  },
  {
    component: ReportComponent,
    path: `${KEY_REPORT}/:storageName/:storageId`,
  },
  {
    component: CompareComponent,
    path: `${KEY_COMPARE}/:leftStorageName/:leftStorageId/:rightStorageName/:rightStorageId`,
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

  shouldDetach(route: ActivatedRouteSnapshot): boolean {
    return this.tabService.getKey(route).length > 0;
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
