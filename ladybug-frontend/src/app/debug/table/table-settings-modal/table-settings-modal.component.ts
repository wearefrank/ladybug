import { Component, inject, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ServerSettings, SettingsService } from '../../../shared/services/settings.service';
import { ToastService } from '../../../shared/services/toast.service';
import { ClientSettingsService } from 'src/app/shared/services/client.settings.service';

@Component({
  selector: 'app-table-settings-modal',
  templateUrl: './table-settings-modal.component.html',
  styleUrls: ['./table-settings-modal.component.css'],
  standalone: true,
  imports: [ReactiveFormsModule],
})
export class TableSettingsModalComponent implements OnInit {
  @ViewChild('modal') protected settingsModalElement!: TemplateRef<HTMLElement>;
  @ViewChild('unsavedChangesModal')
  protected unsavedChangesModalElement!: TemplateRef<HTMLElement>;

  // Cannot be defined after protected members because they
  // are used to initialize the protected members.
  private modalService = inject(NgbModal);
  public clientSettingsService = inject(ClientSettingsService);
  public serverSettingsService = inject(SettingsService);
  private toastService = inject(ToastService);

  protected unsavedChanges = false;

  //Form Control Name keys
  protected readonly showMultipleFilesKey: string = 'showMultipleFilesAtATime';
  protected readonly tableSpacingKey: string = 'tableSpacing';
  protected readonly amountOfRecordsShownKey: string = 'amountOfRecordsShown';
  protected readonly generatorEnabledKey: string = 'generatorEnabled';
  protected readonly regexFilterKey: string = 'regexFilter';
  protected readonly transformationKey: string = 'transformation';
  protected readonly transformationEnabledKey: string = 'transformationEnabled';

  protected readonly spacingOptions: number[] = [0, 1, 2, 3, 4, 5, 6, 7, 8];

  protected readonly GLOBAL = 'Everyone';
  protected readonly CLIENT = 'Personal';

  protected settingsForm: FormGroup = new FormGroup({
    [this.showMultipleFilesKey]: new FormControl(false),
    [this.tableSpacingKey]: new FormControl(0),
    [this.amountOfRecordsShownKey]: new FormControl(0),
    [this.transformationEnabledKey]: new FormControl(true),
    [this.generatorEnabledKey]: new FormControl(false),
    [this.regexFilterKey]: new FormControl(''),
    [this.transformationKey]: new FormControl(''),
  });

  private activeSettingsModal?: NgbActiveModal;
  private activeUnsavedChangesModal?: NgbActiveModal;

  protected activeTab: string = this.GLOBAL;

  ngOnInit(): void {
    this.serverSettingsService.init().then(() => this.loadSettings());
  }

  async open(): Promise<void> {
    await this.loadSettings();
    this.activeSettingsModal = this.modalService.open(this.settingsModalElement);
  }

  closeSettingsModal(): void {
    if (this.unsavedChanges) {
      this.activeUnsavedChangesModal = this.modalService.open(this.unsavedChangesModalElement, { backdrop: 'static' });
    } else {
      this.activeSettingsModal?.close();
    }
  }

  async loadSettings(): Promise<void> {
    await this.serverSettingsService.refresh();
    this.settingsForm
      .get(this.showMultipleFilesKey)
      ?.setValue(this.clientSettingsService.isShowMultipleReportsAtATime());
    this.settingsForm.get(this.tableSpacingKey)?.setValue(this.clientSettingsService.getTableSpacing());
    this.settingsForm
      .get(this.amountOfRecordsShownKey)
      ?.setValue(this.clientSettingsService.getAmountOfRecordsInTable());
    this.settingsForm
      .get(this.transformationEnabledKey)
      ?.setValue(this.clientSettingsService.isTransformationEnabled());
    this.disableForObserver(this.settingsForm.get(this.generatorEnabledKey)!);
    this.settingsForm.get(this.generatorEnabledKey)?.setValue(this.serverSettingsService.isGeneratorEnabled());
    this.disableForObserver(this.settingsForm.get(this.regexFilterKey)!);
    this.settingsForm.get(this.regexFilterKey)?.setValue(this.serverSettingsService.getRegexFilter());
    this.settingsForm.get(this.transformationKey)?.setValue(this.serverSettingsService.getTransformation());
    this.unsavedChanges = false;
  }

  async onClickSave(): Promise<void> {
    this.saveClientSettings();
    this.toastService.showSuccess('Client settings saved');
    // eslint-disable-next-line unicorn/prefer-ternary
    if (this.serverSettingsService.isDataAdmin()) {
      await this.saveSettingsAsDataAdmin();
    } else {
      await this.saveSettingsAsObserver();
    }
    try {
      this.loadSettings();
    } catch {
      this.toastService.showDanger('Failer to reload settings after saving change');
    }
    this.closeSettingsModal();
  }

  async saveSettingsAsDataAdmin(): Promise<void> {
    if (this.formServerSettingsChanged()) {
      const body: ServerSettings = {
        isGeneratorEnabled: this.getFormGeneratorEnabled(),
        regexFilter: this.settingsForm.value[this.regexFilterKey],
        transformation: this.settingsForm.value[this.transformationKey],
      };
      try {
        await this.serverSettingsService.saveAsDataAdmin(body);
        this.toastService.showSuccess('Server settings saved!');
      } catch {
        this.toastService.showDanger('Failed to save settings');
      }
    }
  }

  private async saveSettingsAsObserver(): Promise<void> {
    if (this.formServerSettingsChanged()) {
      try {
        const transformation = this.settingsForm.value[this.transformationKey];
        await this.serverSettingsService.saveAsObserver(transformation);
        this.toastService.showSuccess('Server settings saved!');
      } catch {
        this.toastService.showDanger('Failed to save report transformation with role observer');
      }
    }
  }

  private saveClientSettings(): void {
    this.clientSettingsService.setShowMultipleReportsatATime(this.settingsForm.value[this.showMultipleFilesKey]);
    this.clientSettingsService.setAmountOfRecordsInTable(this.settingsForm.value[this.amountOfRecordsShownKey]);
    this.clientSettingsService.setTableSpacing(this.settingsForm.value[this.tableSpacingKey]);
    this.clientSettingsService.setTransformationEnabled(this.settingsForm.value[this.transformationEnabledKey]);
  }

  async factoryReset(): Promise<void> {
    await this.serverSettingsService.backToFactory();
    await this.clientSettingsService.backToFactory();
    await this.loadSettings();
    this.closeSettingsModal();
  }

  protected formHasChanged(): void {
    const formMultipleFilesEnabled: boolean | null = this.settingsForm.value[this.showMultipleFilesKey];
    const formTableSpacing: number | null = this.settingsForm.value[this.tableSpacingKey];
    const formAmountOfRecordsShown: number | null = this.settingsForm.value[this.amountOfRecordsShownKey];
    const formTransformationEnabled: boolean | null = this.settingsForm.value[this.transformationEnabledKey];
    this.unsavedChanges =
      this.formServerSettingsChanged() ||
      formMultipleFilesEnabled !== this.clientSettingsService.isShowMultipleReportsAtATime() ||
      formTableSpacing !== this.clientSettingsService.getTableSpacing() ||
      formAmountOfRecordsShown !== this.clientSettingsService.getAmountOfRecordsInTable() ||
      formTransformationEnabled !== this.clientSettingsService.isTransformationEnabled();
  }

  protected formServerSettingsChanged(): boolean {
    const formRegexFilter: string | null = this.settingsForm.value[this.regexFilterKey];
    const formTransformation: string | null = this.settingsForm.value[this.transformationKey];
    const result: boolean =
      this.getFormGeneratorEnabled() !== this.serverSettingsService.isGeneratorEnabled() ||
      formRegexFilter !== this.serverSettingsService.getRegexFilter() ||
      formTransformation !== this.serverSettingsService.getTransformation();
    return result;
  }

  private getFormGeneratorEnabled(): boolean {
    const formReportGeneratorEnabled: boolean | null = this.settingsForm.value[this.generatorEnabledKey];
    const result: boolean = formReportGeneratorEnabled === true;
    return result;
  }

  protected async saveAndClose(): Promise<void> {
    await this.saveSettingsAsDataAdmin();
    this.activeUnsavedChangesModal?.close();
    this.closeSettingsModal();
  }

  protected async closeWithoutSaving(): Promise<void> {
    await this.loadSettings();
    this.activeUnsavedChangesModal?.close();
    this.closeSettingsModal();
  }

  getNavClasses(tab: string): string[] {
    const result = ['nav-link'];
    if (tab === this.activeTab) {
      result.push('active');
    }
    return result;
  }

  selectNav(tab: string): void {
    this.activeTab = tab;
  }

  private disableForObserver(f: AbstractControl): void {
    if (this.serverSettingsService.isDataAdmin()) {
      f.enable();
    } else {
      f.disable();
    }
  }

  protected optionalNotAuthorized(): string {
    return this.serverSettingsService.isDataAdmin() ? '' : ' (not authorized to edit)';
  }
}
