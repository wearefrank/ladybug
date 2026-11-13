import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DebugTreeComponent } from './debug-tree.component';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HierarchicalCheckpoint, HierarchicalReport } from '../../shared/interfaces/hierarchical-report';

function checkpoint(id: number, name: string, extra: Partial<HierarchicalCheckpoint> = {}): HierarchicalCheckpoint {
  return {
    id,
    name,
    uid: `5#${id}`,
    type: 1,
    level: 0,
    encoding: null,
    children: null,
    ...extra,
  } as HierarchicalCheckpoint;
}

// report
// +- start (0)
//    +- info (1)
//    +- end (2)
function exampleReport(): HierarchicalReport {
  const start = checkpoint(0, 'start', {
    children: [checkpoint(1, 'info', { type: 6, level: 1 }), checkpoint(2, 'end', { type: 2, level: 0 })],
  });
  return { name: 'report', storageId: 5, children: [start] } as HierarchicalReport;
}

describe('DebugTreeComponent', () => {
  let component: DebugTreeComponent;
  let fixture: ComponentFixture<DebugTreeComponent>;
  let selected: (HierarchicalReport | HierarchicalCheckpoint)[];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DebugTreeComponent],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DebugTreeComponent);
    component = fixture.componentInstance;
    selected = [];
    component.selectReportEvent.subscribe((value) => selected.push(value));
    fixture.detectChanges();
  });

  function element(selector: string): HTMLElement | null {
    return fixture.nativeElement.querySelector(selector) as HTMLElement | null;
  }

  function texts(): string[] {
    const result: string[] = [];
    for (const item of fixture.nativeElement.querySelectorAll('[data-cy-tree-view="text"]')) {
      result.push((item as HTMLElement).textContent!.trim());
    }
    return result;
  }

  function selectedText(): string {
    return element('.sft-item-selected [data-cy-tree-view="text"]')!.textContent!.trim();
  }

  function click(selector: string): void {
    element(selector)!.click();
    fixture.detectChanges();
  }

  it('shows no buttons before a report is added', () => {
    expect(element('[data-cy-debug-tree="buttons"]')).toBeNull();
  });

  it('shows an added report and selects its first checkpoint', () => {
    const report = exampleReport();
    component.addReportToTree(report);
    fixture.detectChanges();
    expect(texts()).toEqual(['report', 'start', 'info', 'end']);
    expect(selectedText()).toBe('start');
    expect(selected).toEqual([report.children![0]]);
    expect(element('[data-cy-debug-tree="buttons"]')).not.toBeNull();
  });

  it('emits the report or checkpoint of a clicked node', () => {
    const report = exampleReport();
    component.addReportToTree(report);
    fixture.detectChanges();
    click('[data-cy-tree-view-key="report"]');
    click('[data-cy-tree-view-key="2"]');
    expect(selected).toEqual([report.children![0], report, report.children![0].children![1]]);
  });

  it('keeps the selection when ids are shown', () => {
    component.addReportToTree(exampleReport());
    fixture.detectChanges();
    click('[data-cy-tree-view-key="1"]');
    click('[data-cy-debug-tree="toggleShowCheckpointId"]');
    expect(texts()).toEqual(['report (5)', 'start (0)', 'info (1)', 'end (2)']);
    expect(selectedText()).toBe('info (1)');
  });

  it('collapseAll keeps the selected node visible and expandAll expands everything', () => {
    component.addReportToTree(exampleReport());
    fixture.detectChanges();
    // 'start' is selected. Its ancestor stays expanded, 'start' itself collapses.
    click('[data-cy-debug-tree="collapseAll"]');
    expect(element('[data-cy-tree-view-key="report"]')!.nextElementSibling!.hasAttribute('hidden')).toBeFalse();
    expect(element('[data-cy-tree-view-key="0"]')!.nextElementSibling!.hasAttribute('hidden')).toBeTrue();
    click('[data-cy-tree-view-key="report"]');
    click('[data-cy-debug-tree="collapseAll"]');
    expect(element('[data-cy-tree-view-key="report"]')!.nextElementSibling!.hasAttribute('hidden')).toBeTrue();
    click('[data-cy-debug-tree="expandAll"]');
    expect(element('[data-cy-tree-view-key="report"]')!.nextElementSibling!.hasAttribute('hidden')).toBeFalse();
    expect(element('[data-cy-tree-view-key="0"]')!.nextElementSibling!.hasAttribute('hidden')).toBeFalse();
  });

  it('highlights nodes whose text contains the search term', () => {
    component.addReportToTree(exampleReport());
    fixture.detectChanges();
    const input = element('[data-cy-debug-tree="search"]') as HTMLInputElement;
    input.value = 'nd';
    input.dispatchEvent(new KeyboardEvent('keyup'));
    fixture.detectChanges();
    const highlighted: string[] = [];
    for (const item of fixture.nativeElement.querySelectorAll('[data-cy-tree-view="text"].red')) {
      highlighted.push((item as HTMLElement).textContent!.trim());
    }
    expect(highlighted).toEqual(['end']);
  });

  it('removes the tree and the buttons when closed', () => {
    component.addReportToTree(exampleReport());
    fixture.detectChanges();
    component.closeEntireTree();
    fixture.detectChanges();
    expect(element('[data-cy-debug-tree="buttons"]')).toBeNull();
    expect(texts()).toEqual([]);
  });

  it('shows a report that is added after closing', () => {
    component.addReportToTree(exampleReport());
    fixture.detectChanges();
    component.closeEntireTree();
    fixture.detectChanges();
    component.addReportToTree(exampleReport());
    fixture.detectChanges();
    expect(texts()).toEqual(['report', 'start', 'info', 'end']);
    expect(selectedText()).toBe('start');
  });
});
