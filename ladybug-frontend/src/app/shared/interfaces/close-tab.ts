export const tabTypeConst = ['compare', 'report'] as const;
export type TabType = (typeof tabTypeConst)[number];

export interface CloseTab {
  type: TabType;
  id: string;
}
