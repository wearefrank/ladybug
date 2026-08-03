import { ChangeDetectorRef, Component, inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { NgOptimizedImage } from '@angular/common';
import { Title } from '@angular/platform-browser';
import { CompareComponent } from './compare/compare.component';
import { TestComponent } from './test/test.component';
import { TabService } from './shared/services/tab.service';
import { AppVariablesService } from './shared/services/app.variables.service';
import { catchError, Subscription } from 'rxjs';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { KEY_COMPARE, KEY_REPORT, Tab } from './shared/interfaces/tab';
import { ToastComponent } from './shared/components/toast/toast.component';
import { HttpService } from './shared/services/http.service';
import { StubStrategy } from './shared/enums/stub-strategy';
import { ErrorHandling } from './shared/classes/error-handling.service';
import { VersionService } from './shared/services/version.service';

interface OpenReportEventData {
  storageName: string;
  storageId: number;
}

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  standalone: true,
  imports: [RouterLinkActive, RouterLink, RouterOutlet, ToastComponent, NgOptimizedImage],
})
export class AppComponent implements OnInit, OnDestroy {
  @ViewChild(CompareComponent) compareComponent!: CompareComponent;
  @ViewChild(TestComponent) testComponent!: TestComponent;
  version?: string;
  title = 'ladybug';
  subscriptions: Subscription = new Subscription();

  protected tabService = inject(TabService);
  // Having this local copy lets us control when to refresh
  protected tabs: Tab[] = [];
  private router = inject(Router);
  private titleService = inject(Title);
  private httpService = inject(HttpService);
  private errorHandler = inject(ErrorHandling);
  private versionService = inject(VersionService);
  private appVariablesService = inject(AppVariablesService);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.fetchAndSetVersion();
    this.subscribeToServices();
    this.getStubStrategies();
    this.appVariablesService.fetchCustomReportActionButtonText();
    this.setupPostMessageBridge();
    this.tabs = this.tabService.getTabs();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  async fetchAndSetVersion(): Promise<void> {
    this.version = await this.versionService.getVersion();
    this.titleService.setTitle(`Ladybug - v${this.version}`);
  }

  subscribeToServices(): void {
    const refreshSubscription: Subscription = this.tabService.refresh$.subscribe((navigation: string | null) => {
      this.tabs = this.tabService.getTabs();
      this.doNavigation(navigation);
      this.cdr.detectChanges();
    });
    this.subscriptions.add(refreshSubscription);
  }

  hasClose(tab: Tab): boolean {
    return tab.kind === KEY_REPORT || tab.kind === KEY_COMPARE;
  }

  closeTabEvent(tab: Tab, event: MouseEvent): void {
    event.stopPropagation();
    event.preventDefault();
    this.tabService.removeTab(tab.key);
    this.cdr.detectChanges();
  }

  protected doNavigation(navigation: string | null): void {
    if (navigation === null) {
      return;
    }
    const parsedNavigation = this.tabService.keyToNavigation(navigation);
    if (parsedNavigation.queryParameters === undefined) {
      this.router.navigate(parsedNavigation.path);
    } else {
      this.router.navigate(parsedNavigation.path, {
        queryParams: parsedNavigation.queryParameters,
      });
    }
  }

  private setupPostMessageBridge(): void {
    // Signal opener that Angular is ready
    if (window.opener) {
      window.opener.postMessage({ action: 'ladybug-ready' }, location.origin);
    }

    window.addEventListener('message', (event: MessageEvent) => {
      if (event.origin !== location.origin) return;
      if (typeof event.data?.action !== 'string' || !event.data.action.startsWith('ladybug-')) return;

      if (event.data?.action === 'ladybug-ping') {
        (event.source as Window)?.postMessage({ action: 'ladybug-ready' }, location.origin);
        return;
      }

      if (event.data?.action === 'ladybug-openReport') {
        const eventData = event.data as OpenReportEventData;
        this.tabService.openReportTab(eventData.storageName, eventData.storageId, 'Loading...');
      }
    });
  }

  getStubStrategies(): void {
    this.httpService
      .getStubStrategies()
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe((response: string[]) => (StubStrategy.report = response));
  }
}
