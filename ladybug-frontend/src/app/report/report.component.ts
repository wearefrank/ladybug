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
import { DebugComponent } from '../debug/debug.component';
import { HierarchicalReportData } from '../shared/interfaces/report-data';
import { TabService } from '../shared/services/tab.service';
import { NodeEventHandler } from 'rxjs/internal/observable/fromEvent';
import { ReportValueComponent } from './report-value/report-value.component';
import { CheckpointValueComponent } from './checkpoint-value/checkpoint-value.component';
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
import { HierarchicalCheckpoint, HierarchicalReport } from '../shared/interfaces/hierarchical-report';
import { DebugTreeNewComponent } from '../debug/debug-tree-new/debug-tree-new.component';

type ReportValueState = 'report' | 'checkpoint' | 'none';

const MIN_HEIGHT = 20;
const MARGIN_IF_NOT_NEW_TAB = 50;

export interface NodeValueState {
  isEdited: boolean;
  isReadOnly: boolean;
  storageId?: number;
  storageName?: string;
  checkpointsFromView?: string | null;
}

export interface UpdateNode {
  checkpointUidToRestore?: string;
  updateReport: UpdateReport;
}

const INDENT_TWO_SPACES = '  ';

@Component({
  selector: 'app-report',
  imports: [AngularSplitModule, DebugTreeNewComponent, ReportValueComponent, CheckpointValueComponent],
  templateUrl: './report.component.html',
  styleUrl: './report.component.css',
})
export class ReportComponent implements OnInit, AfterViewInit, OnDestroy {
  static readonly ROUTER_PATH: string = 'report';
  @Input() newTab = true;
  @ViewChild(SplitComponent) splitter!: SplitComponent;
  @ViewChild(DebugTreeNewComponent) debugTreeComponent!: DebugTreeNewComponent;

  protected monacoEditorHeight!: number;
  protected reportValueState: ReportValueState = 'none';
  // Not ordinary subjects, because the report or checkpoint value may
  // be posted before the receiving component is ready.
  // Also not ReplaySubject, because we do not want old report or checkpont
  // values to be reposted.
  protected reportValueSubject = new BehaviorSubject<HierarchicalReport | undefined>(undefined);
  protected checkpointValueSubject = new BehaviorSubject<HierarchicalCheckpoint | undefined>(undefined);
  protected saveDoneSubject = new Subject<void>();
  protected rerunResultSubject = new BehaviorSubject<TestResult | undefined>(undefined);
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
  private newTabReportData?: HierarchicalReportData;

  ngOnInit(): void {
    this.newTabReportData = this.tabService.activeReportTabs.get(this.getIdFromPath());
    if (!this.newTabReportData) {
      this.router.navigate([DebugComponent.ROUTER_PATH]);
    }
    this.listenToHeight();
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      if (this.newTabReportData) {
        // TODO: Take care here when working on issue https://github.com/wearefrank/ladybug-frontend/issues/1125.
        this.addReport(this.newTabReportData.report);
      }
    });
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  // Entry point when report is opened from debug table.
  // Also called when this component lives in a dedicated
  // tab after the HierarchicalReport has been fetched
  // via the URL.
  addReport(report: HierarchicalReport): void {
    this.debugTreeComponent.addReportToTree(report);
  }

  closeEntireTree(): void {
    this.debugTreeComponent.closeEntireTree();
    if (this.newTab && this.newTabReportData) {
      this.tabService.closeTab(this.newTabReportData);
    }
    this.changeReportValueState('none');
  }

  selectReport(node: HierarchicalReport | HierarchicalCheckpoint): void {
    // eslint-disable-next-line unicorn/no-useless-undefined
    this.rerunResultSubject.next(undefined);
    if (ReportUtility.isReport(node)) {
      console.log(`ReportComponent.selectReport(), report with storage id ${node.storageId}`);
      this.changeReportValueState('report');
      this.reportValueSubject.next(node as HierarchicalReport);
    } else if (ReportUtility.isCheckPoint(node)) {
      this.changeReportValueState('checkpoint');
      const checkpointNode = node as HierarchicalCheckpoint;
      this.checkpointValueSubject.next(checkpointNode);
    } else {
      throw new Error('State.newNode(): Node is neither a Report nor a Checkpoint');
    }
  }

  onButton(command: ButtonCommand): void {
    switch (command) {
      case 'close': {
        this.closeEntireTree();
        break;
      }
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
    this.showToastForCopyToTestTabIfApplicable(nodeValueState);
    this.nodeValueState = nodeValueState;
    // Suppress errors ExpressionChangedAfterItHasBeenCheckedError about button existence changes.
    this.cdr.detectChanges();
  }

  // eslint-disable-next-line @typescript-eslint/no-empty-function
  protected initEditor(): void {}

  protected save(update: UpdateNode): void {
    this.httpService
      .updateReport(`${this.nodeValueState!.storageId!}`, update.updateReport, this.nodeValueState!.storageName!)
      .pipe(catchError(this.handleErrorWithRethrowMessage('Caught error when trying to update report')))
      .subscribe({
        next: () => {
          this.toastService.showSuccessLong('Report updated!');
          this.updateUIAfterSave();
        },
      });
  }

  protected onDownload(downloadOptions: DownloadOptions): void {
    if (this.nodeValueState?.storageId === undefined) {
      throw new Error('ReportComponent.onDownload(): Expected that storageId was filled');
    }
    const queryString = `id=${this.nodeValueState!.storageId}`;
    this.helperService.download(
      `${queryString}&`,
      this.nodeValueState!.storageName!,
      downloadOptions.downloadReport,
      downloadOptions.downloadXmlSummary,
      false,
    );
    // TODO: Issue https://github.com/wearefrank/ladybug-frontend/issues/1128.
    this.toastService.showSuccess('Report Downloaded!');
  }

  private copyReport(): void {
    if (this.nodeValueState?.storageId === undefined) {
      throw new Error('Cannot copy report because ReportComponent does not have the storageId');
    }
    const data: Record<string, number[]> = {
      [this.nodeValueState!.storageName!]: [this.nodeValueState!.storageId],
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
    if (this.nodeValueState?.storageId === undefined) {
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
      .runReport(this.nodeValueState!.storageName!, this.nodeValueState!.storageId)
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
    if (this.nodeValueState?.storageId === undefined) {
      this.toastService.showDanger('Could not find report to apply custom action');
    } else {
      this.httpService
        .processCustomReportAction(this.nodeValueState!.storageName!, [this.nodeValueState!.storageId])
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

  private updateUIAfterSave(): void {
    console.log('ReportComponent.updateUIAfterSave()');
    this.saveDoneSubject.next();
    this.getReportFromServer().then((updatedReport) => {
      console.log('Got updated report from server');
      this.ngZone.run(() => {
        if (this.newTab) {
          console.log(`Restoring report with storage id [${updatedReport.storageId}]`);
          this.addReport(updatedReport);
          // TODO: Issue https://github.com/wearefrank/ladybug-frontend/issues/1129.
          // Add logic to restore the checkpoint that was selected before. Take
          // care not to create an infinite loop - logic reacting to selected node
          // event coming from tree trying to manipulate the debug tree again.
          this.testRefreshService.refreshAll();
        } else {
          this.debugTab.refreshAll({
            reportIds: [this.nodeValueState!.storageId!],
            displayToast: false,
          });
        }
      });
    });
  }

  private getReportFromServer(): Promise<HierarchicalReport> {
    if (this.nodeValueState?.checkpointsFromView === undefined) {
      throw new Error('Cannot get report from server because it is not clear what checkpoints to ask');
    }
    const checkpointsFromView: string | null = this.nodeValueState!.checkpointsFromView;
    return new Promise((resolve, reject) => {
      if (this.nodeValueState?.storageId === undefined) {
        this.toastService.showDanger(
          'Programming error detected, please view console log (F12) and refresh your browser',
        );
        reject();
      }
      this.httpService
        .getHierarchicalReports(
          [this.nodeValueState!.storageId!],
          this.nodeValueState!.storageName!,
          checkpointsFromView,
        )
        .pipe(catchError(this.handleErrorWithRethrowMessage('Failed to fetch report from the server')))
        .subscribe({
          next: (reports: HierarchicalReport[]): void => resolve(reports[0]),
          error: () => {
            this.toastService.showDanger(
              'Failed to get updated report from the server, please see browser console (F12) and refresh your browser',
            );
            reject();
          },
        });
    });
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
    this.reportValueSubject.next(undefined);
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
