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
    children: [checkpoint(1, 'info', { type: 6, level: 1 }), checkpoint(2, 'end', { type: 2, level: 1 })],
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

    fixture = TestBed.createComponent(DebugTreeComponent);
    component = fixture.componentInstance;
    selected = [];
    component.selectReportEvent.subscribe((value) => selected.push(value));
    fixture.detectChanges();
  });

  function element(selector: string): HTMLElement | null {
    return fixture.nativeElement.querySelector(selector) as HTMLElement | null;
  }

  // The helpers below access the tree through the data-cy-tree-view-*
  // attributes, see tree-view.component.html.

  function textsOf(selector: string): string[] {
    const result: string[] = [];
    for (const item of fixture.nativeElement.querySelectorAll(`${selector} [data-cy-tree-view-text]`)) {
      result.push((item as HTMLElement).textContent!.trim());
    }
    return result;
  }

  function texts(): string[] {
    return textsOf('[data-cy-tree-view-node]');
  }

  function selectedText(): string {
    return element('[data-cy-tree-view-selected] [data-cy-tree-view-text]')!.textContent!.trim();
  }

  function node(key: string): HTMLElement {
    return element(`[data-cy-tree-view-node="${CSS.escape(key)}"]`)!;
  }

  function isExpanded(key: string): boolean {
    return 'cyTreeViewExpanded' in node(key).dataset;
  }

  function typeOf(key: string): string | undefined {
    return (node(key).querySelector('[data-cy-tree-view-type]') as HTMLElement).dataset['cyTreeViewType'];
  }

  function click(selector: string): void {
    element(selector)!.click();
    fixture.detectChanges();
  }

  function selectNode(key: string): void {
    (node(key).querySelector('[data-cy-tree-view-select]') as HTMLElement).click();
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
    selectNode('report');
    selectNode('2');
    expect(selected).toEqual([report.children![0], report, report.children![0].children![1]]);
  });

  it('keeps the selection when ids are shown', () => {
    component.addReportToTree(exampleReport());
    fixture.detectChanges();
    selectNode('1');
    click('[data-cy-debug-tree="toggleShowCheckpointId"]');
    expect(texts()).toEqual(['report (5)', 'start (0)', 'info (1)', 'end (2)']);
    expect(selectedText()).toBe('info (1)');
  });

  it('collapseAll keeps the selected node visible and expandAll expands everything', () => {
    component.addReportToTree(exampleReport());
    fixture.detectChanges();
    // 'start' is selected. Its ancestor stays expanded, 'start' itself collapses.
    click('[data-cy-debug-tree="collapseAll"]');
    expect(isExpanded('report')).toBeTrue();
    expect(isExpanded('0')).toBeFalse();
    selectNode('report');
    click('[data-cy-debug-tree="collapseAll"]');
    expect(isExpanded('report')).toBeFalse();
    click('[data-cy-debug-tree="expandAll"]');
    expect(isExpanded('report')).toBeTrue();
    expect(isExpanded('0')).toBeTrue();
  });

  it('highlights nodes whose text contains the search term', () => {
    component.addReportToTree(exampleReport());
    fixture.detectChanges();
    const input = element('[data-cy-debug-tree="search"]') as HTMLInputElement;
    input.value = 'nd';
    input.dispatchEvent(new KeyboardEvent('keyup'));
    fixture.detectChanges();
    expect(textsOf('[data-cy-tree-view-highlighted]')).toEqual(['end']);
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

  describe('node types', () => {
    it('gives each node the tree view type that belongs to the checkpoint type', () => {
      // The keys are the values of field `type` of HierarchicalCheckpoint, which
      // is how the backend sends the type of a checkpoint. The backend computes
      // them with org.wearefrank.ladybug.CheckpointType.toInt(), which is the
      // position in that enum plus one. The frontend enum CheckpointType in
      // shared/enums/checkpoint-type.ts has the same values. This test uses the
      // numbers instead of that enum to check the mapping from what the backend
      // actually sends. Which icon belongs to a tree view type is tested in
      // tree-view.component.spec.ts.
      const expectedTypes: Record<number, string> = {
        1: 'startpoint',
        2: 'endpoint',
        3: 'abortpoint',
        4: 'inputpoint',
        5: 'outputpoint',
        6: 'infopoint',
        7: 'threadCreatepoint',
        8: 'threadStartpoint',
        9: 'threadEndpoint',
      };
      const types = Object.keys(expectedTypes).map(Number);
      const children = types.map((type) => checkpoint(type, `type ${type}`, { type }));
      component.addReportToTree({ name: 'report', storageId: 5, children } as HierarchicalReport);
      fixture.detectChanges();
      expect(typeOf('report')).toBe('report');
      for (const type of types) {
        expect(typeOf(`${type}`))
          .withContext(`type ${type}`)
          .toBe(expectedTypes[type]);
      }
    });

    it('marks a node as error when the message is a thrown exception', () => {
      const children = [
        checkpoint(0, 'exception', { type: 6, level: 0, encoding: 'printStackTrace()' }),
        checkpoint(1, 'normal', { type: 6, level: 0 }),
      ];
      component.addReportToTree({ name: 'report', storageId: 5, children } as HierarchicalReport);
      fixture.detectChanges();
      expect('cyTreeViewError' in node('0').dataset).toBeTrue();
      expect('cyTreeViewError' in node('1').dataset).toBeFalse();
    });

    it('passes the level of each checkpoint, and no level for the report', () => {
      const children = [checkpoint(0, 'start', { level: 0, children: [checkpoint(1, 'info', { type: 6, level: 1 })] })];
      component.addReportToTree({ name: 'report', storageId: 5, children } as HierarchicalReport);
      fixture.detectChanges();
      expect(node('report').dataset['cyTreeViewLevel']).toBeUndefined();
      expect(node('0').dataset['cyTreeViewLevel']).toBe('0');
      expect(node('1').dataset['cyTreeViewLevel']).toBe('1');
    });
  });
});
