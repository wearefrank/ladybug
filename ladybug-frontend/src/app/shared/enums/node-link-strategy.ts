export const nodeLinkStrategyConst = ['PATH', 'CHECKPOINT_NUMBER', 'NONE'] as const;
export type NodeLinkStrategy = (typeof nodeLinkStrategyConst)[number];
