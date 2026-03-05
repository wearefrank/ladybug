import { inject, Injectable } from '@angular/core';
import { BaseReport } from '../interfaces/base-report';
import { ClientSettingsService } from './client.settings.service';

@Injectable({
  providedIn: 'root',
})
export class HelperService {
  private clientSettingsService = inject(ClientSettingsService);

  download(queryString: string, storage: string, exportBinary: boolean, exportXML: boolean): void {
    let xmlChoice = 'omit';
    if (exportXML) {
      xmlChoice = this.clientSettingsService.isTransformationEnabled() ? 'with_default_xslt' : 'no_default_xslt';
    }
    window.open(
      `api/report/download/${storage}/${exportBinary}/${xmlChoice}/${this.clientSettingsService.isForMultipleOmitIfXmlEmpty()}?${queryString.slice(0, -1)}`,
    );
  }

  getSelectedIds(reports: BaseReport[]): number[] {
    let copiedIds: number[] = [];
    for (const report of this.getSelectedReports(reports)) {
      copiedIds.push(report.storageId);
    }
    return copiedIds;
  }

  getSelectedReports(reports: BaseReport[]): BaseReport[] {
    return reports.filter((report) => report.checked);
  }
}
