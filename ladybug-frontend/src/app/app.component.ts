import { ChangeDetectorRef, Component, inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Location, NgOptimizedImage } from '@angular/common';
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
  private titleService = inject(Title);
  private router = inject(Router);
  private location = inject(Location);
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
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  async fetchAndSetVersion(): Promise<void> {
    this.version = await this.versionService.getVersion();
    this.titleService.setTitle(`Ladybug - v${this.version}`);
  }

  subscribeToServices(): void {
    const refreshSubscription: Subscription = this.tabService.refresh$.subscribe(() => this.cdr.detectChanges());
    this.subscriptions.add(refreshSubscription);
  }

  hasClose(tab: Tab): boolean {
    return tab.kind === KEY_REPORT || tab.kind === KEY_COMPARE;
  }

  closeTabEvent(tab: Tab, event: MouseEvent): void {
    event.stopPropagation();
    event.preventDefault();
    this.tabService.removeTab(tab);
  }

  getStubStrategies(): void {
    this.httpService
      .getStubStrategies()
      .pipe(catchError(this.errorHandler.handleError()))
      .subscribe((response: string[]) => (StubStrategy.report = response));
  }
}
