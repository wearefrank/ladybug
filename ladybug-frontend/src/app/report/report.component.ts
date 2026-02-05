import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  inject,
  Input,
  ViewChild,
  OnInit,
  AfterViewInit,
  OnDestroy,
  NgZone,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AngularSplitModule, SplitComponent } from 'angular-split';
import { BehaviorSubject, catchError, debounceTime, fromEventPattern, Observable, Subject, Subscription } from 'rxjs';
import { DebugTreeComponent } from '../debug/debug-tree/debug-tree.component';
import { DebugComponent } from '../debug/debug.component';
import { ReportData } from '../shared/interfaces/report-data';
import { Report } from '../shared/interfaces/report';
import { Checkpoint } from '../shared/interfaces/checkpoint';
import { View } from '../shared/interfaces/view';
import { TabService } from '../shared/services/tab.service';
import { NodeEventHandler } from 'rxjs/internal/observable/fromEvent';
import { ReportValueComponent } from './report-value/report-value.component';
import { CheckpointValueComponent, PartialCheckpoint } from './checkpoint-value/checkpoint-value.component';
import { ReportUtil as ReportUtility } from '../shared/util/report-util';
import { ButtonCommand, DownloadOptions } from './report-buttons/report-buttons';
import { ErrorHandling } from '../shared/classes/error-handling.service';
import { HttpService } from '../shared/services/http.service';
import { ToastService } from '../shared/services/toast.service';
import { TestReportsService } from '../test/test-reports.service';
import { HttpErrorResponse } from '@angular/common/http';
import { TestResult } from '../shared/interfaces/test-result';
import { DebugTabService } from '../debug/debug-tab.service';
import { UpdateReport } from '../shared/interfaces/update-report';
import { HelperService } from '../shared/services/helper.service';
import { TestRefreshService } from '../test/test-refresh.service';

type ReportValueState = 'report' | 'checkpoint' | 'none';

const MIN_HEIGHT = 20;
const MARGIN_IF_NOT_NEW_TAB = 50;

export interface PartialReport {
  name: string;
  description: string | null;
  path: string | null;
  // TODO: Issue https://github.com/wearefrank/ladybug-frontend/issues/1127
  transformation: string | null;
  // TODO: Issue https://github.com/wearefrank/ladybug-frontend/issues/1127
  variables: string;
  xml: string;
  crudStorage: boolean;
  // undefined is allowed to support testing
  storageId?: number;
  stubStrategy: string;
  correlationId: string;
  estimatedMemoryUsage: number;
  storageName: string;
}

export interface NodeValueState {
  isEdited: boolean;
  isReadOnly: boolean;
  storageId?: number;
}

export interface UpdateNode {
  checkpointUidToRestore?: string;
  updateReport: UpdateReport;
}

const INDENT_TWO_SPACES = '  ';

@Component({
  selector: 'app-report',
  imports: [AngularSplitModule, DebugTreeComponent, ReportValueComponent, CheckpointValueComponent],
  templateUrl: './report.component.html',
  styleUrl: './report.component.css',
})
export class ReportComponent implements OnInit, AfterViewInit, OnDestroy {
  static readonly ROUTER_PATH: string = 'report';
  @Input() newTab = true;
  @Input({ required: true }) currentView!: View;
  @ViewChild(SplitComponent) splitter!: SplitComponent;
  @ViewChild(DebugTreeComponent) debugTreeComponent!: DebugTreeComponent;

  protected treeWidth: Subject<void> = new Subject<void>();
  protected monacoEditorHeight!: number;
  protected reportValueState: ReportValueState = 'none';
  // Not ordinary subjects, because the report or checkpoint value may
  // be posted before the receiving component is ready.
  // Also not ReplaySubject, because we do not want old report or checkpont
  // values to be reposted.
  protected reportSubject = new BehaviorSubject<PartialReport | undefined>(undefined);
  protected checkpointValueSubject = new BehaviorSubject<PartialCheckpoint | undefined>(undefined);
  protected saveDoneSubject = new Subject<void>();
  protected rerunResultSubject = new BehaviorSubject<TestResult | undefined>(undefined);
  private storageId?: number;
  private nodeValueState?: NodeValueState;
  private host = inject(ElementRef);
  private tabService = inject(TabService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  private httpService = inject(HttpService);
  private errorHandler = inject(ErrorHandling);
  private toastService = inject(ToastService);
  private testReportsService = inject(TestReportsService);
  private helperService = inject(HelperService);
  private debugTab = inject(DebugTabService);
  private testRefreshService = inject(TestRefreshService);
  private ngZone = inject(NgZone);
  private subscriptions: Subscription = new Subscription();
  private newTabReportData?: ReportData;

  ngOnInit(): void {
    this.newTabReportData = this.tabService.activeReportTabs.get(this.getIdFromPath());
    if (!this.newTabReportData) {
      this.router.navigate([DebugComponent.ROUTER_PATH]);
    }
    this.listenToHeight();
  }

  ngAfterViewInit(): void {
    if (this.splitter.dragProgress$) {
      this.splitter.dragProgress$.subscribe(() => {
        this.treeWidth.next();
      });
    }
    setTimeout(() => {
      if (this.newTabReportData) {
        this.currentView = this.newTabReportData.currentView;
        // TODO: Take care here when working on issue https://github.com/wearefrank/ladybug-frontend/issues/1125.
        this.addReportToTree(this.newTabReportData.report);
      }
    });
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  addReportToTree(report: Report): void {
    this.debugTreeComponent.addReportToTree(report);
  }

  closeEntireTree(): void {
    if (this.newTab && this.newTabReportData) {
      this.tabService.closeTab(this.newTabReportData);
    }
    this.changeReportValueState('none');
  }

  selectReport(node: Report | Checkpoint): void {
    // eslint-disable-next-line unicorn/no-useless-undefined
    this.rerunResultSubject.next(undefined);
    if (ReportUtility.isReport(node)) {
      this.changeReportValueState('report');
      this.reportSubject.next(node as Report);
    } else if (ReportUtility.isCheckPoint(node)) {
      this.changeReportValueState('checkpoint');
      const checkpointNode = node as Checkpoint;
      this.checkpointValueSubject.next(checkpointNode);
    } else {
      throw new Error('State.newNode(): Node is neither a Report nor a Checkpoint');
    }
  }

  onButton(command: ButtonCommand): void {
    switch (command) {
      case 'makeNull': {
        throw new Error(
          'ReportComponent.onButton() with command makeNull cannot happen - should be handled by CheckpointValue',
        );
      }
      case 'prettify': {
        throw new Error(
          'ReportComponent.onButton() with command prettify cannot happen - should be handled by CheckpointValue',
        );
      }
      case 'save': {
        throw new Error(
          'ReportComponent.onButton() with command save cannot happen - should be handled by CheckpointValue or ReportValue',
        );
      }
      case 'copyReport': {
        this.copyReport();
        break;
      }
      case 'rerun': {
        this.rerunReport();
        break;
      }
      case 'customReportAction': {
        this.processCustomReportAction();
        break;
      }
      case 'showMetadata': {
        throw new Error('Command showMetadata should have been handles by ReportValue or CheckpointValue');
      }
      case 'hideMetadata': {
        throw new Error('Command hideMetadata should have been handles by ReportValue or CheckpointValue');
      }
      case 'hideMessageContext': {
        throw new Error('Command hideMessageContext should have been handles by CheckpointValue');
      }
      case 'showMessageContext': {
        throw new Error('Command showMessageContext should have been handles by CheckpointValue');
      }
    }
  }

  onNodeValueState(nodeValueState: NodeValueState): void {
    this.storageId = nodeValueState.storageId;
    this.showToastForCopyToTestTabIfApplicable(nodeValueState);
    this.nodeValueState = nodeValueState;
    // Suppress errors ExpressionChangedAfterItHasBeenCheckedError about button existence changes.
    this.cdr.detectChanges();
  }

  // eslint-disable-next-line @typescript-eslint/no-empty-function
  protected initEditor(): void {}

  protected save(update: UpdateNode): void {
    this.httpService
      .updateReport(`${this.storageId!}`, update.updateReport, this.currentView.storageName)
      .pipe(catchError(this.handleErrorWithRethrowMessage('Caught error when trying to update report')))
      .subscribe({
        next: () => {
          this.toastService.showSuccessLong('Report updated!');
          this.updateUIAfterSave(update.checkpointUidToRestore);
        },
      });
  }

  protected onDownload(downloadOptions: DownloadOptions): void {
    if (this.storageId === undefined) {
      throw new Error('ReportComponent.onDownload(): Expected that storageId was filled');
    }
    const queryString = `id=${this.storageId}`;
    this.helperService.download(
      `${queryString}&`,
      this.currentView.storageName,
      downloadOptions.downloadReport,
      downloadOptions.downloadXmlSummary,
    );
    // TODO: Issue https://github.com/wearefrank/ladybug-frontend/issues/1128.
    this.toastService.showSuccess('Report Downloaded!');
  }

  private copyReport(): void {
    if (this.storageId === undefined) {
      throw new Error('Cannot copy report because ReportComponent does not have the storageId');
    }
    const data: Record<string, number[]> = {
      [this.currentView.storageName]: [this.storageId],
    };
    this.httpService
      .copyReport(data, 'Test')
      .pipe(catchError(this.handleErrorWithRethrowMessage('Copying report failed')))
      .subscribe({
        next: () => {
          this.testReportsService.getReports();
          this.toastService.showSuccess('Copied report to test tab', {
            buttonText: 'Go to test tab',
            callback: () => this.router.navigate(['/test']),
          });
        },
      }); // TODO: storage is hardcoded, fix issue https://github.com/wearefrank/ladybug-frontend/issues/196.
  }

  private rerunReport(): void {
    if (this.storageId === undefined) {
      throw new Error('Cannot rerun report because ReportComponent does not have the storageId');
    }
    if (this.nodeValueState !== undefined && this.nodeValueState.isEdited === true) {
      if (this.nodeValueState.isReadOnly) {
        this.toastService.showWarning(
          'This storage is readonly so reran original report. Copy to test tab to save changes and rerun updated report.',
          {
            buttonText: 'Copy to test tab',
            callback: () => {
              this.copyReport();
            },
          },
        );
      } else {
        this.toastService.showWarning('Changes were not saved so reran original report.');
      }
    }
    this.httpService
      .runReport(this.currentView.storageName, this.storageId)
      .pipe(catchError(this.handleErrorWithRethrowMessage('Rerunning report failed')))
      .subscribe({
        next: (response: TestResult): void => {
          this.toastService.showSuccess('Report rerun successful');
          this.rerunResultSubject.next(response);
          this.debugTab.refreshTable({ displayToast: false });
        },
      });
  }

  private processCustomReportAction(): void {
    if (this.storageId === undefined) {
      this.toastService.showDanger('Could not find report to apply custom action');
    } else {
      this.httpService
        .processCustomReportAction(this.currentView.storageName, [this.storageId])
        .pipe(catchError(this.handleErrorWithRethrowMessage('Could not start custom report action')))
        .subscribe({
          next: (data: Record<string, string>) => {
            if (data.success) {
              this.toastService.showSuccess(data.success);
            }
            if (data.error) {
              this.toastService.showDanger(data.error);
            }
          },
          error: () => {
            this.toastService.showDanger('Failed to process custom report action');
          },
        });
    }
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private handleErrorWithRethrowMessage(message: string): (error: HttpErrorResponse) => Observable<any> {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return (error: HttpErrorResponse): Observable<any> => {
      this.errorHandler.handleError()(error);
      throw new Error(message);
    };
  }

  private showToastForCopyToTestTabIfApplicable(newNodeValueState: NodeValueState): void {
    const isEditingStarted: boolean =
      this.nodeValueState !== undefined && !this.nodeValueState.isEdited && newNodeValueState.isEdited;
    if (isEditingStarted && newNodeValueState.isReadOnly) {
      this.toastService.showWarning(
        'This storage is readonly. Copy to test tab to save changes and rerun updated report.',
        {
          buttonText: 'Copy to test tab',
          callback: () => {
            this.copyReport();
          },
        },
      );
    }
  }

  private updateUIAfterSave(checkpointUidToRestore: string | undefined): void {
    this.saveDoneSubject.next();
    this.getReportFromServer().then((updatedReport) => {
      this.ngZone.run(() => {
        if (this.newTab) {
          this.addReportToTree(updatedReport);
          this.selectUpdatedReportOrCheckpoint(updatedReport, checkpointUidToRestore);
          this.testRefreshService.refreshAll();
        } else {
          this.debugTab.refreshAll({
            reportIds: [this.storageId!],
            displayToast: false,
          });
        }
      });
    });
  }

  private getReportFromServer(): Promise<Report> {
    return new Promise((resolve, reject) => {
      if (this.storageId === undefined) {
        console.log('ReportComponent.getReportFromServer(): Expected that there was a storageId');
        this.toastService.showDanger(
          'Programming error detected, please view console log (F12) and refresh your browser',
        );
        reject();
      }
      this.httpService
        .getReport(this.storageId!, this.currentView.storageName)
        .pipe(catchError(this.handleErrorWithRethrowMessage('Failed to fetch report from the server')))
        .subscribe({
          next: (report: Report): void => resolve(report),
          error: () => {
            this.toastService.showDanger(
              'Failed to get updated report from the server, please see browser console (F12) and refresh your browser',
            );
            reject();
          },
        });
    });
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  private selectUpdatedReportOrCheckpoint(updatedReport: Report, checkpointUid: string | undefined): void {
    // TODO: Issue https://github.com/wearefrank/ladybug-frontend/issues/1129.
    this.selectReport(updatedReport);
  }

  private getIdFromPath(): string {
    return this.route.snapshot.paramMap.get('id') as string;
  }

  private listenToHeight(): void {
    const resizeObserver$ = fromEventPattern<ResizeObserverEntry[]>((handler: NodeEventHandler) => {
      const resizeObserver = new ResizeObserver(handler);
      resizeObserver.observe(this.host.nativeElement);
      return (): void => resizeObserver.disconnect();
    });

    const resizeSubscription = resizeObserver$.pipe(debounceTime(50)).subscribe((entries: ResizeObserverEntry[]) => {
      const entry = (entries[0] as unknown as ResizeObserverEntry[])[0];
      this.handleHeightChanges(entry.target.clientHeight);
    });
    this.subscriptions.add(resizeSubscription);
  }

  private handleHeightChanges(clientHeight: number): void {
    this.monacoEditorHeight = clientHeight;
    if (!this.newTab) {
      this.monacoEditorHeight = this.monacoEditorHeight - MARGIN_IF_NOT_NEW_TAB;
    }
    if (this.monacoEditorHeight < MIN_HEIGHT) {
      this.monacoEditorHeight = MIN_HEIGHT;
    }
    this.cdr.detectChanges();
  }

  private changeReportValueState(state: ReportValueState): void {
    this.reportValueState = state;
    // Make sure no old report or old checkpoint is processed when related components are recreated.
    /* eslint-disable-next-line unicorn/no-useless-undefined */
    this.reportSubject.next(undefined);
    /* eslint-disable-next-line unicorn/no-useless-undefined */
    this.checkpointValueSubject.next(undefined);
  }
}

export function prettify(text: string | null): string | null {
  if (text === null) {
    return text;
  }
  try {
    if (JSON.parse(text!)) {
      return JSON.stringify(JSON.parse(text!), null, INDENT_TWO_SPACES);
    }
  } catch {
    // Not JSON, continue.
  }
  if (checkIfIsXml(text!)) {
    return prettifyXml(text);
  }
  return text;
}

function checkIfIsXml(text: string): boolean {
  if (text) {
    for (let index = 0; index < text.length; index++) {
      if (text.charAt(index) === ' ' || text.charAt(index) === '\t') {
        continue;
      }
      return text.charAt(index) === '<';
    }
  }
  return false;
}

function prettifyXml(sourceXml: string): string {
  var xmlDoc = new DOMParser().parseFromString(sourceXml, 'application/xml');
  var xsltDoc = new DOMParser().parseFromString(
    [
      // describes how we want to modify the XML - indent everything
      '<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform">',
      '  <xsl:strip-space elements="*"/>',
      '  <xsl:template match="para[content-style][not(text())]">', // change to just text() to strip space in text nodes
      '    <xsl:value-of select="normalize-space(.)"/>',
      '  </xsl:template>',
      '  <xsl:template match="node()|@*">',
      '    <xsl:copy><xsl:apply-templates select="node()|@*"/></xsl:copy>',
      '  </xsl:template>',
      '  <xsl:output indent="yes"/>',
      '</xsl:stylesheet>',
    ].join('\n'),
    'application/xml',
  );

  var xsltProcessor = new XSLTProcessor();
  xsltProcessor.importStylesheet(xsltDoc);
  var resultDoc = xsltProcessor.transformToDocument(xmlDoc);
  var resultXml = new XMLSerializer().serializeToString(resultDoc);
  return resultXml;
}
