export const StubStrategy = {
  report: [] as string[],
  checkpoints: ['Use report level stub strategy', 'Always stub this checkpoint', 'Never stub this checkpoint'] as const,
  // See backend StubType.toInt()
  checkpointStubToIndex: (stub: number): number => stub + 1,
  checkpointIndex2Stub: (index: number): number => index - 1,
};
