import { DifferencesBuilder } from './differences-builder';

describe('DifferencesBuilder', () => {
  it('When a non-nullable variable is added then one difference is created', () => {
    const instance = new DifferencesBuilder().nonNullableVariable('oldValue', 'newValue', 'Name of difference');
    expect(instance.data.length).toEqual(1);
    expect(instance.data[0].originalValue).toEqual('oldValue');
    expect(instance.data[0].editedValue).toEqual('newValue');
    expect(instance.data[0].name).toEqual('Name of difference');
    expect(instance.data[0].colorDifferences).toEqual(false);
  });

  it('When equal non-nullable values are compared then no difference is created', () => {
    const instance = new DifferencesBuilder().nonNullableVariable('value', 'value', 'Name of difference');
    expect(instance.data.length).toEqual(0);
  });

  it('When a nullable variable is added and old and new are non-null, then one difference is created', () => {
    const instance = new DifferencesBuilder().nullableVariable('oldValue', 'newValue', 'Name of difference');
    expect(instance.data.length).toEqual(1);
    expect(instance.data[0].originalValue).toEqual('oldValue');
    expect(instance.data[0].editedValue).toEqual('newValue');
    expect(instance.data[0].name).toEqual('Name of difference');
    expect(instance.data[0].colorDifferences).toEqual(false);
  });

  it('When two equal nullable values are compared, no difference is created', () => {
    const instance = new DifferencesBuilder().nullableVariable('value', 'value', 'Name of difference');
    expect(instance.data.length).toEqual(0);
  });

  it('When a null variable is given a value, two difference are created', () => {
    const instance = new DifferencesBuilder().nullableVariable(null, 'newValue', 'Name of difference', true);
    expect(instance.data.length).toEqual(2);
    expect(instance.data[0].name).toEqual('Name of difference - null status');
    expect(instance.data[0].colorDifferences).toEqual(false);
    expect(instance.data[0].originalValue).toEqual('null');
    expect(instance.data[0].editedValue).toEqual('not null');
    expect(instance.data[1].name).toEqual('Name of difference - text');
    expect(instance.data[1].colorDifferences).toEqual(true);
    expect(instance.data[1].originalValue).toEqual('');
    expect(instance.data[1].editedValue).toEqual('newValue');
  });

  it('When a non-null variable is given a null value, two difference are created', () => {
    const instance = new DifferencesBuilder().nullableVariable('oldValue', null, 'Name of difference', true);
    expect(instance.data.length).toEqual(2);
    expect(instance.data[0].name).toEqual('Name of difference - null status');
    expect(instance.data[0].colorDifferences).toEqual(false);
    expect(instance.data[0].originalValue).toEqual('not null');
    expect(instance.data[0].editedValue).toEqual('null');
    expect(instance.data[1].name).toEqual('Name of difference - text');
    expect(instance.data[1].colorDifferences).toEqual(true);
    expect(instance.data[1].originalValue).toEqual('oldValue');
    expect(instance.data[1].editedValue).toEqual('');
  });

  it('When two null values are compared, no difference is created', () => {
    const instance = new DifferencesBuilder().nullableVariable(null, null, 'A name');
    expect(instance.data.length).toEqual(0);
  });

  it('When null is changed to the empty string, it is expressed clearly in one difference', () => {
    const instance = new DifferencesBuilder().nullableVariable(null, '', 'A name', true);
    expect(instance.data.length).toEqual(1);
    expect(instance.data[0].name).toEqual('A name - null status');
    expect(instance.data[0].colorDifferences).toEqual(false);
    expect(instance.data[0].originalValue).toEqual('null');
    expect(instance.data[0].editedValue).toEqual('blank');
  });

  it('When empty string is changed to null, it is expressed clearly in one difference', () => {
    const instance = new DifferencesBuilder().nullableVariable('', null, 'A name', true);
    expect(instance.data.length).toEqual(1);
    expect(instance.data[0].name).toEqual('A name - null status');
    expect(instance.data[0].colorDifferences).toEqual(false);
    expect(instance.data[0].originalValue).toEqual('blank');
    expect(instance.data[0].editedValue).toEqual('null');
  });
});
