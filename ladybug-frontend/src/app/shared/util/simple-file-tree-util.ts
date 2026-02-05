import { CreateTreeItem, TreeItemComponent } from 'ng-simple-file-tree';

export const SimpleFileTreeUtil = {
  conditionalCssClass(item: CreateTreeItem): string {
    if (!item.uid || !item.iconClass) {
      return 'bi bi-folder icon-size';
    }
    return item.iconClass;
  },

  hideOrShowCheckpoints(unmatched: string[], items: TreeItemComponent[]): void {
    for (let item of items) {
      if (unmatched.length === 0 || !unmatched) {
        item.setVisible(true);
      } else if (unmatched.includes(item.item.originalValue.uid)) {
        item.setVisible(false);
      }
      if (item.item.children) {
        this.hideOrShowCheckpoints(unmatched, [item.childElement]);
      }
    }
  },
};
