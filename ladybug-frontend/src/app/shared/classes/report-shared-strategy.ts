import { BehaviorSubject, catchError, debounceTime, fromEventPattern, Observable, Subject, Subscription } from 'rxjs';
import { HierarchicalCheckpoint, HierarchicalReport } from '../interfaces/hierarchical-report';
import { ReportUtil } from '../util/report-util';
import { HttpService } from '../services/http.service';
import { ChangeDetectorRef, ElementRef, inject, NgZone } from '@angular/core';
import { TestResult } from '../interfaces/test-result';
import { UpdateReport } from '../interfaces/update-report';
import { ButtonCommand, DownloadOptions } from '../../report/report-buttons/report-buttons';
import { ToastService } from '../services/toast.service';
import { HelperService } from '../services/helper.service';
import { TestReportsService } from '../../test/test-reports.service';
import { DebugTabService } from '../../debug/debug-tab.service';
import { HttpErrorResponse } from '@angular/common/http';
import { ErrorHandling } from './error-handling.service';
import { NodeEventHandler } from 'rxjs/internal/observable/fromEvent';

type ReportValueState = 'report' | 'checkpoint' | 'none';

const INDENT_TWO_SPACES = '  ';

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

export interface ReportComponentCallback {
  closeEntireTree(): void;
  navigateToTestTab(): void;
  onRefreshReport(report: HierarchicalReport): void;
  handleHeightChanges(clientHeight: number): void;
}

export class ReportSharedStrategy {
  private callback!: ReportComponentCallback;

  reportValueState: ReportValueState = 'none';
  // Not ordinary subjects, because the report or checkpoint value may
  // be posted before the receiving component is ready.
  // Also not ReplaySubject, because we do not want old report or checkpont
  // values to be reposted.
  reportValueSubject = new BehaviorSubject<HierarchicalReport | undefined>(undefined);
  checkpointValueSubject = new BehaviorSubject<HierarchicalCheckpoint | undefined>(undefined);
  saveDoneSubject = new Subject<void>();
  rerunResultSubject = new BehaviorSubject<TestResult | undefined>(undefined);
  nodeValueState?: NodeValueState;
  toastService = inject(ToastService);

  private helperService = inject(HelperService);
  private httpService = inject(HttpService);
  private debugTab = inject(DebugTabService);
  private testReportsService = inject(TestReportsService);
  private ngZone = inject(NgZone);
  private errorHandler = inject(ErrorHandling);
  private host = inject(ElementRef);
  private cdr = inject(ChangeDetectorRef);
  private subscriptions: Subscription = new Subscription();

  setCallback(callback: ReportComponentCallback): void {
    this.callback = callback;
  }

  unsubscribe(): void {
    this.subscriptions.unsubscribe();
  }

  selectReport(node: HierarchicalReport | HierarchicalCheckpoint): void {
    // eslint-disable-next-line unicorn/no-useless-undefined
    this.rerunResultSubject.next(undefined);
    if (ReportUtil.isReport(node)) {
      this.changeReportValueState('report');
      this.reportValueSubject.next(node as HierarchicalReport);
    } else if (ReportUtil.isCheckPoint(node)) {
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
        this.callback.closeEntireTree();
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

  save(update: UpdateNode): void {
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

  onDownload(downloadOptions: DownloadOptions): void {
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

  changeReportValueState(state: ReportValueState): void {
    this.reportValueState = state;
    // Make sure no old report or old checkpoint is processed when related components are recreated.
    /* eslint-disable-next-line unicorn/no-useless-undefined */
    this.reportValueSubject.next(undefined);
    /* eslint-disable-next-line unicorn/no-useless-undefined */
    this.checkpointValueSubject.next(undefined);
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
            callback: () => this.callback.navigateToTestTab(),
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
      .subscribe((response: TestResult) => {
        this.toastService.showSuccess('Report rerun successful');
        this.rerunResultSubject.next(response);
        this.debugTab.refreshTable({ displayToast: false });
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
    this.saveDoneSubject.next();
    this.getReportFromServer().then((updatedReport) => {
      this.ngZone.run(() => this.callback.onRefreshReport(updatedReport));
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

  listenToHeight(): void {
    const resizeObserver$ = fromEventPattern<ResizeObserverEntry[]>((handler: NodeEventHandler) => {
      const resizeObserver = new ResizeObserver(handler);
      resizeObserver.observe(this.host.nativeElement);
      return (): void => resizeObserver.disconnect();
    });

    const resizeSubscription = resizeObserver$.pipe(debounceTime(50)).subscribe((entries: ResizeObserverEntry[]) => {
      const entry = (entries[0] as unknown as ResizeObserverEntry[])[0];
      this.callback.handleHeightChanges(entry.target.clientHeight);
    });
    this.subscriptions.add(resizeSubscription);
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
