import { BooleanToStringPipe } from './boolean-to-string.pipe';

describe('BooleanToStringPipe', () => {
  it('create an instance', () => {
    const pipe = new BooleanToStringPipe();
    expect(pipe).toBeTruthy();
  });
});
