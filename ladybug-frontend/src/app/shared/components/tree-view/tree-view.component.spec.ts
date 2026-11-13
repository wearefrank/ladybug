import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Subject } from 'rxjs';
import { TreeViewCommand, TreeViewComponent, TreeViewInputNode } from './tree-view.component';

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

  function send(command: TreeViewCommand): void {
    commands.next(command);
    fixture.detectChanges();
  }

  function item(key: string): HTMLElement {
    const result = fixture.nativeElement.querySelector(
      `[data-cy-tree-view-key="${CSS.escape(key)}"]`,
    ) as HTMLElement | null;
    if (result === null) {
      throw new Error(`No tree node with key ${key}`);
    }
    return result;
  }

  function hasItem(key: string): boolean {
    return fixture.nativeElement.querySelector(`[data-cy-tree-view-key="${CSS.escape(key)}"]`) !== null;
  }

  function selectedKeys(): string[] {
    return fixture.debugElement
      .queryAll(By.css('.sft-item-selected'))
      .map((element) => (element.nativeElement as HTMLElement).dataset['cyTreeViewKey']!);
  }

  // The children container is the next sibling of the item.
  function isExpanded(key: string): boolean {
    const children = item(key).nextElementSibling as HTMLElement;
    return !children.hidden;
  }

  function clickChevron(key: string): void {
    (item(key).querySelector('[data-cy-tree-view="chevron"]') as HTMLElement).click();
    fixture.detectChanges();
  }

  function clickItem(key: string): void {
    item(key).click();
    fixture.detectChanges();
  }

  function iconClasses(key: string): DOMTokenList {
    return (item(key).querySelector('[data-cy-tree-view="icon"]') as HTMLElement).classList;
  }

  it('shows nothing before the first command', () => {
    expect(fixture.nativeElement.querySelectorAll('.sft-item').length).toBe(0);
    expect(emitted).toEqual([]);
  });

  describe('command new', () => {
    it('shows all nodes expanded and selects the requested node', () => {
      send({ command: 'new', nodes: exampleTree(), select: 'a2' });
      for (const key of ['r', 'a', 'a1', 'a2', 'b', 'b1']) {
        expect(hasItem(key)).withContext(key).toBeTrue();
      }
      for (const key of ['r', 'a', 'b']) {
        expect(isExpanded(key)).withContext(key).toBeTrue();
      }
      expect(selectedKeys()).toEqual(['a2']);
      expect(emitted).toEqual(['a2']);
    });

    it('selects the root when select is absent', () => {
      send({ command: 'new', nodes: exampleTree() });
      expect(selectedKeys()).toEqual(['r']);
      expect(emitted).toEqual(['r']);
    });

    it('selects the root when no node has the requested key', () => {
      send({ command: 'new', nodes: exampleTree(), select: 'unknown' });
      expect(selectedKeys()).toEqual(['r']);
      expect(emitted).toEqual(['r']);
    });

    it('discards the selection and expanded state of the previous tree', () => {
      send({ command: 'new', nodes: exampleTree(), select: 'a1' });
      clickChevron('b');
      expect(isExpanded('b')).toBeFalse();
      send({ command: 'new', nodes: exampleTree(), select: 'b1' });
      expect(isExpanded('b')).toBeTrue();
      expect(selectedKeys()).toEqual(['b1']);
      expect(emitted).toEqual(['a1', 'b1']);
    });

    it('removes nodes that are not in the new tree', () => {
      send({ command: 'new', nodes: exampleTree() });
      send({ command: 'new', nodes: report([node('c')]) });
      expect(hasItem('a')).toBeFalse();
      expect(hasItem('c')).toBeTrue();
    });
  });

  describe('command update', () => {
    it('keeps the selected node when it is still present', () => {
      send({ command: 'new', nodes: exampleTree(), select: 'a1' });
      send({ command: 'update', nodes: exampleTree(), select: 'b1' });
      expect(selectedKeys()).toEqual(['a1']);
      expect(emitted).toEqual(['a1', 'a1']);
    });

    it('keeps the expanded state of nodes that are still present', () => {
      send({ command: 'new', nodes: exampleTree(), select: 'a1' });
      clickChevron('b');
      send({ command: 'update', nodes: exampleTree() });
      expect(isExpanded('b')).toBeFalse();
      expect(isExpanded('a')).toBeTrue();
    });

    it('expands nodes that are new', () => {
      send({ command: 'new', nodes: exampleTree(), select: 'a1' });
      send({ command: 'update', nodes: report([node('a', [node('a1')]), node('c', [node('c1')])]) });
      expect(isExpanded('c')).toBeTrue();
    });

    it('shows the new text and attributes of nodes', () => {
      send({ command: 'new', nodes: exampleTree() });
      send({ command: 'update', nodes: report([node('a', [], { text: 'changed', highlighted: true })]) });
      const text = item('a').querySelector('[data-cy-tree-view="text"]') as HTMLElement;
      expect(text.textContent!.trim()).toBe('changed');
      expect(text.classList).toContain('red');
      expect(hasItem('b')).toBeFalse();
    });

    it('selects the requested node when the selected node is gone, expanding its ancestors', () => {
      send({ command: 'new', nodes: exampleTree(), select: 'a1' });
      clickChevron('b');
      send({ command: 'update', nodes: report([node('a', [node('a2')]), node('b', [node('b1')])]), select: 'b1' });
      expect(selectedKeys()).toEqual(['b1']);
      expect(isExpanded('b')).toBeTrue();
      expect(emitted).toEqual(['a1', 'b1']);
    });

    it('selects the root when the selected node is gone and select is absent or unknown', () => {
      send({ command: 'new', nodes: exampleTree(), select: 'a1' });
      send({ command: 'update', nodes: report([node('b')]) });
      expect(selectedKeys()).toEqual(['r']);
      send({ command: 'new', nodes: exampleTree(), select: 'a1' });
      send({ command: 'update', nodes: report([node('b')]), select: 'unknown' });
      expect(selectedKeys()).toEqual(['r']);
    });

    it('acts like new when there is no previous tree', () => {
      send({ command: 'update', nodes: exampleTree(), select: 'b1' });
      expect(selectedKeys()).toEqual(['b1']);
      expect(isExpanded('a')).toBeTrue();
    });
  });

  describe('user interaction', () => {
    beforeEach(() => {
      send({ command: 'new', nodes: exampleTree(), select: 'a1' });
      emitted = [];
    });

    it('selects a clicked node and emits its key', () => {
      clickItem('b1');
      expect(selectedKeys()).toEqual(['b1']);
      expect(emitted).toEqual(['b1']);
    });

    it('toggles expanded when the chevron is clicked, without selecting', () => {
      clickChevron('b');
      expect(isExpanded('b')).toBeFalse();
      clickChevron('b');
      expect(isExpanded('b')).toBeTrue();
      expect(selectedKeys()).toEqual(['a1']);
      expect(emitted).toEqual([]);
    });

    it('selects a collapsed node when it is an ancestor of the selected node', () => {
      clickChevron('r');
      expect(isExpanded('r')).toBeFalse();
      expect(selectedKeys()).toEqual(['r']);
      expect(emitted).toEqual(['r']);
    });
  });

  describe('rendering', () => {
    it('chooses icon colors from type, valueIsError and level', () => {
      send({
        command: 'new',
        nodes: report([
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

    it('shows a chevron only for nodes with children', () => {
      send({ command: 'new', nodes: exampleTree() });
      expect(item('a').querySelector('[data-cy-tree-view="chevron"]')).not.toBeNull();
      expect(item('a1').querySelector('[data-cy-tree-view="chevron"]')).toBeNull();
    });
  });
});
