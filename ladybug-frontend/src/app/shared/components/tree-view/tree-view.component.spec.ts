import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { TreeViewCommand, TreeViewComponent, TreeViewInputNode, TreeViewNodeType } from './tree-view.component';

// The Bootstrap icon that is shown for each type of node.
const EXPECTED_ICONS: Record<TreeViewNodeType, string> = {
  report: 'bi-folder',
  startpoint: 'bi-arrow-bar-right',
  endpoint: 'bi-arrow-bar-left',
  abortpoint: 'bi-x-lg',
  inputpoint: 'bi-arrow-right',
  outputpoint: 'bi-arrow-left',
  infopoint: 'bi-info-square',
  threadCreatepoint: 'bi-fast-forward',
  threadStartpoint: 'bi-chevron-double-right',
  threadEndpoint: 'bi-chevron-double-left',
};

function node(
  key: string,
  children: TreeViewInputNode[] = [],
  extra: Partial<TreeViewInputNode> = {},
): TreeViewInputNode {
  return {
    key,
    type: 'startpoint',
    text: `text of ${key}`,
    valueIsError: false,
    level: 0,
    highlighted: false,
    children,
    ...extra,
  };
}

function report(children: TreeViewInputNode[]): TreeViewInputNode {
  return node('r', children, { type: 'report', level: null });
}

// r
// +- a
// |  +- a1
// |  +- a2
// +- b
//    +- b1
function exampleTree(): TreeViewInputNode {
  return report([node('a', [node('a1'), node('a2')]), node('b', [node('b1')])]);
}

describe('TreeViewComponent', () => {
  let fixture: ComponentFixture<TreeViewComponent>;
  let commands: Subject<TreeViewCommand>;
  let emitted: string[];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TreeViewComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(TreeViewComponent);
    commands = new Subject<TreeViewCommand>();
    fixture.componentRef.setInput('commands', commands);
    emitted = [];
    fixture.componentInstance.nodeSelected.subscribe((key: string) => emitted.push(key));
    fixture.detectChanges();
  });

  // Every change of the view is followed by a check that the data-cy
  // attributes agree with what is shown.
  function render(): void {
    fixture.detectChanges();
    expectDataCyAgreesWithView();
  }

  function send(command: TreeViewCommand): void {
    commands.next(command);
    render();
  }

  // The helpers below only use the data-cy-tree-view-* attributes, which
  // are explained in tree-view.component.html. Except for the rendering
  // tests at the end, the tests only use these helpers.

  function allNodes(): HTMLElement[] {
    return [...fixture.nativeElement.querySelectorAll('[data-cy-tree-view-node]')];
  }

  function findItem(key: string): HTMLElement | null {
    return fixture.nativeElement.querySelector(`[data-cy-tree-view-node="${CSS.escape(key)}"]`);
  }

  function item(key: string): HTMLElement {
    const result = findItem(key);
    if (result === null) {
      throw new Error(`No tree node with key ${key}`);
    }
    return result;
  }

  function hasItem(key: string): boolean {
    return findItem(key) !== null;
  }

  function selectedKeys(): string[] {
    return allNodes()
      .filter((element) => 'cyTreeViewSelected' in element.dataset)
      .map((element) => element.dataset['cyTreeViewNode']!);
  }

  function isExpanded(key: string): boolean {
    const element = item(key);
    if ('cyTreeViewExpanded' in element.dataset) {
      return true;
    }
    if ('cyTreeViewCollapsed' in element.dataset) {
      return false;
    }
    throw new Error(`Tree node ${key} has no children, so it is neither expanded nor collapsed`);
  }

  // A node is shown when all its ancestors are expanded.
  function isShown(key: string): boolean {
    for (let parent = item(key).dataset['cyTreeViewParent']; parent !== undefined; ) {
      const parentElement = item(parent);
      if (!('cyTreeViewExpanded' in parentElement.dataset)) {
        return false;
      }
      parent = parentElement.dataset['cyTreeViewParent'];
    }
    return true;
  }

  function textOf(key: string): string {
    return item(key).querySelector('[data-cy-tree-view-text]')!.textContent!.trim();
  }

  function clickExpansionToggle(key: string): void {
    (item(key).querySelector('[data-cy-tree-view-expansion-toggle]') as HTMLElement).click();
    render();
  }

  function clickSelect(key: string): void {
    (item(key).querySelector('[data-cy-tree-view-select]') as HTMLElement).click();
    render();
  }

  // Checks that the data-cy-tree-view-* attributes agree with what is shown.
  function expectDataCyAgreesWithView(): void {
    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelectorAll('.sft-item').length).withContext('number of nodes').toBe(allNodes().length);
    for (const element of allNodes()) {
      const key = element.dataset['cyTreeViewNode']!;
      const context = `node ${key}`;
      expect(element.classList).withContext(context).toContain('sft-item');

      // The parent is the node whose child container holds this node.
      // Each node is wrapped in its own <app-tree-view-node> host element.
      const childWrapper = element.parentElement!.parentElement!;
      const structuralParent = childWrapper.classList.contains('sft-item-child')
        ? (childWrapper.parentElement!.previousElementSibling as HTMLElement)
        : undefined;
      expect(element.dataset['cyTreeViewParent'])
        .withContext(`${context} parent`)
        .toBe(structuralParent?.dataset['cyTreeViewNode']);

      const expanded = 'cyTreeViewExpanded' in element.dataset;
      const collapsed = 'cyTreeViewCollapsed' in element.dataset;
      const childContainer = element.nextElementSibling as HTMLElement | null;
      const hasChildren = childContainer?.classList.contains('sft-child-container') ?? false;
      const toggle = element.querySelector('[data-cy-tree-view-expansion-toggle]');
      if (hasChildren) {
        expect(expanded !== collapsed)
          .withContext(`${context} expanded xor collapsed`)
          .toBeTrue();
        expect(childContainer!.hidden).withContext(`${context} children hidden`).toBe(collapsed);
        const chevron = toggle!.querySelector('.sft-chevron')!.classList;
        expect(chevron.contains('bi-chevron-down')).withContext(`${context} chevron down`).toBe(expanded);
        expect(chevron.contains('bi-chevron-right')).withContext(`${context} chevron right`).toBe(collapsed);
      } else {
        expect(expanded || collapsed)
          .withContext(`${context} expanded or collapsed`)
          .toBeFalse();
        expect(toggle).withContext(`${context} toggle`).toBeNull();
      }

      expect(isShown(key)).withContext(`${context} shown`).toBe(element.checkVisibility());

      expect('cyTreeViewSelected' in element.dataset)
        .withContext(`${context} selected`)
        .toBe(element.classList.contains('sft-item-selected'));

      const text = element.querySelector('[data-cy-tree-view-text]') as HTMLElement;
      expect('cyTreeViewHighlighted' in element.dataset)
        .withContext(`${context} highlighted`)
        .toBe(text.classList.contains('red'));

      const level = element.dataset['cyTreeViewLevel'];
      const icon = element.querySelector('[data-cy-tree-view-type]') as HTMLElement;
      const type = icon.dataset['cyTreeViewType'] as TreeViewNodeType;
      expect(icon.classList).withContext(`${context} icon`).toContain(EXPECTED_ICONS[type]);
      expect(icon.classList.contains('red'))
        .withContext(`${context} red icon`)
        .toBe('cyTreeViewError' in element.dataset || type === 'abortpoint');
      if (level === undefined) {
        expect(icon.classList.contains('even') || icon.classList.contains('odd'))
          .withContext(`${context} colored without level`)
          .toBeFalse();
      } else {
        const isClosing = (['endpoint', 'abortpoint', 'threadEndpoint'] as TreeViewNodeType[]).includes(type);
        const colorLevel = Number(level) - (isClosing ? 1 : 0);
        expect(icon.classList)
          .withContext(`${context} color`)
          .toContain(colorLevel % 2 === 0 ? 'even' : 'odd');
      }

      expect(element.querySelector('[data-cy-tree-view-select]')).withContext(`${context} select`).not.toBeNull();
    }
  }

  // For the rendering tests, which check how the tree looks.
  function iconClasses(key: string): DOMTokenList {
    return (item(key).querySelector('[data-cy-tree-view-type]') as HTMLElement).classList;
  }

  it('shows nothing before the first command', () => {
    expect(allNodes().length).toBe(0);
    expect(emitted).toEqual([]);
  });

  describe('command new', () => {
    it('shows all nodes expanded and selects the requested node', () => {
      send({ command: 'new', node: exampleTree(), select: 'a2' });
      for (const key of ['r', 'a', 'a1', 'a2', 'b', 'b1']) {
        expect(hasItem(key)).withContext(key).toBeTrue();
      }
      for (const key of ['r', 'a', 'b']) {
        expect(isExpanded(key)).withContext(key).toBeTrue();
      }
      expect(selectedKeys()).toEqual(['a2']);
      expect(emitted).toEqual(['a2']);
    });

    it('exposes the parent of each node and leaves leaves neither expanded nor collapsed', () => {
      send({ command: 'new', node: exampleTree() });
      const parents = allNodes().map((element) => [
        element.dataset['cyTreeViewNode'],
        element.dataset['cyTreeViewParent'],
      ]);
      expect(parents).toEqual([
        ['r', undefined],
        ['a', 'r'],
        ['a1', 'a'],
        ['a2', 'a'],
        ['b', 'r'],
        ['b1', 'b'],
      ]);
      expect(() => isExpanded('a1')).toThrowError(/neither expanded nor collapsed/);
    });

    it('selects the root when select is absent', () => {
      send({ command: 'new', node: exampleTree() });
      expect(selectedKeys()).toEqual(['r']);
      expect(emitted).toEqual(['r']);
    });

    it('selects the root when no node has the requested key', () => {
      send({ command: 'new', node: exampleTree(), select: 'unknown' });
      expect(selectedKeys()).toEqual(['r']);
      expect(emitted).toEqual(['r']);
    });

    it('discards the selection and expanded state of the previous tree', () => {
      send({ command: 'new', node: exampleTree(), select: 'a1' });
      clickExpansionToggle('b');
      expect(isExpanded('b')).toBeFalse();
      send({ command: 'new', node: exampleTree(), select: 'b1' });
      expect(isExpanded('b')).toBeTrue();
      expect(selectedKeys()).toEqual(['b1']);
      expect(emitted).toEqual(['a1', 'b1']);
    });

    it('removes nodes that are not in the new tree', () => {
      send({ command: 'new', node: exampleTree() });
      send({ command: 'new', node: report([node('c')]) });
      expect(hasItem('a')).toBeFalse();
      expect(hasItem('c')).toBeTrue();
    });
  });

  describe('command update', () => {
    it('keeps the selected node when it is still present', () => {
      send({ command: 'new', node: exampleTree(), select: 'a1' });
      send({ command: 'update', node: exampleTree(), select: 'b1' });
      expect(selectedKeys()).toEqual(['a1']);
      expect(emitted).toEqual(['a1', 'a1']);
    });

    it('keeps the expanded state of nodes that are still present', () => {
      send({ command: 'new', node: exampleTree(), select: 'a1' });
      clickExpansionToggle('b');
      send({ command: 'update', node: exampleTree() });
      expect(isExpanded('b')).toBeFalse();
      expect(isExpanded('a')).toBeTrue();
    });

    it('expands nodes that are new', () => {
      send({ command: 'new', node: exampleTree(), select: 'a1' });
      send({ command: 'update', node: report([node('a', [node('a1')]), node('c', [node('c1')])]) });
      expect(isExpanded('c')).toBeTrue();
    });

    it('shows the new text and attributes of nodes', () => {
      send({ command: 'new', node: exampleTree() });
      send({
        command: 'update',
        node: report([node('a', [], { text: 'changed', type: 'infopoint', valueIsError: true, highlighted: true })]),
      });
      expect(textOf('a')).toBe('changed');
      expect((item('a').querySelector('[data-cy-tree-view-type]') as HTMLElement).dataset['cyTreeViewType']).toBe(
        'infopoint',
      );
      expect('cyTreeViewError' in item('a').dataset).toBeTrue();
      expect('cyTreeViewHighlighted' in item('a').dataset).toBeTrue();
      expect(hasItem('b')).toBeFalse();
    });

    it('selects the requested node when the selected node is gone, expanding its ancestors', () => {
      send({ command: 'new', node: exampleTree(), select: 'a1' });
      clickExpansionToggle('b');
      send({ command: 'update', node: report([node('a', [node('a2')]), node('b', [node('b1')])]), select: 'b1' });
      expect(selectedKeys()).toEqual(['b1']);
      expect(isExpanded('b')).toBeTrue();
      expect(emitted).toEqual(['a1', 'b1']);
    });

    it('selects the root when the selected node is gone and select is absent or unknown', () => {
      send({ command: 'new', node: exampleTree(), select: 'a1' });
      send({ command: 'update', node: report([node('b')]) });
      expect(selectedKeys()).toEqual(['r']);
      send({ command: 'new', node: exampleTree(), select: 'a1' });
      send({ command: 'update', node: report([node('b')]), select: 'unknown' });
      expect(selectedKeys()).toEqual(['r']);
    });

    it('acts like new when there is no previous tree', () => {
      send({ command: 'update', node: exampleTree(), select: 'b1' });
      expect(selectedKeys()).toEqual(['b1']);
      expect(isExpanded('a')).toBeTrue();
    });
  });

  describe('commands expandAll and collapseAll', () => {
    it('expandAll expands all nodes and keeps the selection without emitting', () => {
      send({ command: 'new', node: exampleTree(), select: 'a1' });
      // Collapsing 'a' moves the selection from 'a1' to 'a'.
      clickExpansionToggle('a');
      clickExpansionToggle('b');
      expect(emitted).toEqual(['a1', 'a']);
      emitted = [];
      send({ command: 'expandAll' });
      for (const key of ['r', 'a', 'b']) {
        expect(isExpanded(key)).withContext(key).toBeTrue();
      }
      expect(selectedKeys()).toEqual(['a']);
      expect(emitted).toEqual([]);
    });

    it('collapseAll keeps only the ancestors of the selected node expanded', () => {
      send({ command: 'new', node: exampleTree(), select: 'a1' });
      emitted = [];
      send({ command: 'collapseAll' });
      expect(isExpanded('r')).toBeTrue();
      expect(isExpanded('a')).toBeTrue();
      expect(isExpanded('b')).toBeFalse();
      expect(isShown('a1')).toBeTrue();
      expect(isShown('b')).toBeTrue();
      expect(isShown('b1')).toBeFalse();
      expect(selectedKeys()).toEqual(['a1']);
      expect(emitted).toEqual([]);
    });

    it('collapseAll collapses a selected node that has children', () => {
      send({ command: 'new', node: exampleTree(), select: 'a' });
      expect(emitted).toEqual(['a']);
      emitted = [];
      send({ command: 'collapseAll' });
      expect(isExpanded('r')).toBeTrue();
      expect(isExpanded('a')).toBeFalse();
      expect(isExpanded('b')).toBeFalse();
      expect(selectedKeys()).toEqual(['a']);
      expect(emitted).toEqual([]);
    });

    it('collapseAll collapses everything when the root is selected', () => {
      send({ command: 'new', node: exampleTree() });
      send({ command: 'collapseAll' });
      for (const key of ['r', 'a', 'b']) {
        expect(isExpanded(key)).withContext(key).toBeFalse();
      }
      expect(selectedKeys()).toEqual(['r']);
    });

    it('update after collapseAll keeps the collapsed state', () => {
      send({ command: 'new', node: exampleTree(), select: 'a1' });
      send({ command: 'collapseAll' });
      send({ command: 'update', node: exampleTree() });
      expect(isExpanded('a')).toBeTrue();
      expect(isExpanded('b')).toBeFalse();
    });

    it('ignores expandAll and collapseAll before the first tree', () => {
      send({ command: 'expandAll' });
      send({ command: 'collapseAll' });
      expect(allNodes().length).toBe(0);
      expect(emitted).toEqual([]);
      send({ command: 'new', node: exampleTree(), select: 'b1' });
      expect(selectedKeys()).toEqual(['b1']);
    });
  });

  describe('command select', () => {
    it('selects the node, expands its ancestors and does not emit', () => {
      send({ command: 'new', node: exampleTree(), select: 'a1' });
      send({ command: 'collapseAll' });
      expect(isExpanded('b')).toBeFalse();
      emitted = [];
      send({ command: 'select', key: 'b1' });
      expect(selectedKeys()).toEqual(['b1']);
      expect(isExpanded('r')).toBeTrue();
      expect(isExpanded('b')).toBeTrue();
      expect(emitted).toEqual([]);
    });

    it('ignores an unknown key', () => {
      send({ command: 'new', node: exampleTree(), select: 'a1' });
      emitted = [];
      send({ command: 'select', key: 'unknown' });
      expect(selectedKeys()).toEqual(['a1']);
      expect(emitted).toEqual([]);
    });

    it('is ignored before the first tree', () => {
      send({ command: 'select', key: 'b1' });
      expect(allNodes().length).toBe(0);
      expect(emitted).toEqual([]);
      send({ command: 'new', node: exampleTree(), select: 'a1' });
      expect(selectedKeys()).toEqual(['a1']);
    });

    it('is kept by a following update', () => {
      send({ command: 'new', node: exampleTree(), select: 'a1' });
      send({ command: 'select', key: 'b1' });
      send({ command: 'update', node: exampleTree(), select: 'a2' });
      expect(selectedKeys()).toEqual(['b1']);
    });
  });

  describe('command clear', () => {
    it('removes the tree without emitting', () => {
      send({ command: 'new', node: exampleTree(), select: 'a1' });
      emitted = [];
      send({ command: 'clear' });
      expect(allNodes().length).toBe(0);
      expect(emitted).toEqual([]);
    });

    it('makes a following update act like new', () => {
      send({ command: 'new', node: exampleTree(), select: 'a1' });
      clickExpansionToggle('b');
      send({ command: 'clear' });
      send({ command: 'update', node: exampleTree(), select: 'b1' });
      expect(selectedKeys()).toEqual(['b1']);
      expect(isExpanded('b')).toBeTrue();
    });

    it('makes following expandAll, collapseAll and select do nothing', () => {
      send({ command: 'new', node: exampleTree(), select: 'a1' });
      send({ command: 'clear' });
      send({ command: 'expandAll' });
      send({ command: 'collapseAll' });
      send({ command: 'select', key: 'b1' });
      expect(allNodes().length).toBe(0);
    });
  });

  describe('user interaction', () => {
    beforeEach(() => {
      send({ command: 'new', node: exampleTree(), select: 'a1' });
      emitted = [];
    });

    it('selects a clicked node and emits its key', () => {
      clickSelect('b1');
      expect(selectedKeys()).toEqual(['b1']);
      expect(emitted).toEqual(['b1']);
    });

    it('toggles expanded when the expansion toggle is clicked, without selecting', () => {
      clickExpansionToggle('b');
      expect(isExpanded('b')).toBeFalse();
      clickExpansionToggle('b');
      expect(isExpanded('b')).toBeTrue();
      expect(selectedKeys()).toEqual(['a1']);
      expect(emitted).toEqual([]);
    });

    it('selects a collapsed node when it is an ancestor of the selected node', () => {
      clickExpansionToggle('r');
      expect(isExpanded('r')).toBeFalse();
      expect(selectedKeys()).toEqual(['r']);
      expect(emitted).toEqual(['r']);
    });
  });

  describe('rendering', () => {
    it('chooses icon colors from type, valueIsError and level', () => {
      send({
        command: 'new',
        node: report([
          node('even', [], { type: 'infopoint', level: 2 }),
          node('odd', [], { type: 'infopoint', level: 1 }),
          node('abort', [], { type: 'abortpoint', level: 1 }),
          node('error', [], { type: 'outputpoint', level: 0, valueIsError: true }),
        ]),
      });
      expect(iconClasses('even')).toContain('even');
      expect(iconClasses('even')).not.toContain('red');
      expect(iconClasses('odd')).toContain('odd');
      expect(iconClasses('abort')).toContain('red');
      expect(iconClasses('error')).toContain('red');
      expect(iconClasses('r')).toContain('bi-folder');
      expect(iconClasses('r')).not.toContain('even');
      expect(iconClasses('r')).not.toContain('odd');
    });

    it('colors a closing checkpoint like its startpoint, although its level is one higher', () => {
      send({
        command: 'new',
        node: report([
          node(
            'start',
            [node('child', [], { type: 'infopoint', level: 1 }), node('end', [], { type: 'endpoint', level: 1 })],
            { type: 'startpoint', level: 0 },
          ),
          node('threadStart', [node('threadEnd', [], { type: 'threadEndpoint', level: 2 })], {
            type: 'threadStartpoint',
            level: 1,
          }),
          node('abort', [], { type: 'abortpoint', level: 1 }),
        ]),
      });
      expect(iconClasses('start')).toContain('even');
      expect(iconClasses('child')).toContain('odd');
      expect(iconClasses('end')).toContain('even');
      expect(iconClasses('threadStart')).toContain('odd');
      expect(iconClasses('threadEnd')).toContain('odd');
      expect(iconClasses('abort')).toContain('even');
    });

    it('shows the icon that belongs to the type', () => {
      const types = Object.keys(EXPECTED_ICONS).filter((type) => type !== 'report') as TreeViewNodeType[];
      send({ command: 'new', node: report(types.map((type) => node(type, [], { type }))) });
      expect(iconClasses('r')).toContain(EXPECTED_ICONS.report);
      for (const type of types) {
        expect(iconClasses(type)).withContext(type).toContain(EXPECTED_ICONS[type]);
      }
    });

    it('shows a chevron only for nodes with children', () => {
      send({ command: 'new', node: exampleTree() });
      expect(item('a').querySelector('.sft-chevron')).not.toBeNull();
      expect(item('a1').querySelector('.sft-chevron')).toBeNull();
    });
  });
});
