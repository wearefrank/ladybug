import { TestBed } from '@angular/core/testing';

import { FilterFromUrl, TabService } from './tab.service';
import { KEY_DEBUG } from '../interfaces/tab';
import { ActivatedRouteSnapshot, UrlSegment } from '@angular/router';

describe('TabService', () => {
  let service: TabService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TabService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('When a word has special characters then it can be encoded for URLs and decoded again', () => {
    const word = 'beautiful world?&';
    const encoded = encodeURIComponent(word);
    expect(encoded).not.toContain('?');
    expect(encoded).not.toContain(' ');
    expect(encoded).not.toContain('&');
    const decoded = decodeURIComponent(encoded);
    expect(decoded).toEqual(word);
  });

  it('When activated route has no filters then no filters and key is just debug', () => {
    let route = getDefaultActivatedRouteSnapshotWithPath(KEY_DEBUG);
    route.queryParams = {};
    const filters: FilterFromUrl[] = service.routeGetFilters(route);
    expect(filters.length).toEqual(0);
    expect(service.getKey(route)).toEqual(`${KEY_DEBUG}`);
  });

  it('When activated route has filters then parsed and added to key', () => {
    let route = getDefaultActivatedRouteSnapshotWithPath(KEY_DEBUG);
    route.queryParams = {
      'filter-host': 'Host A',
      'filter-application': 'Application X',
    };
    const filters: FilterFromUrl[] = service.routeGetFilters(route);
    expect(filters.length).toEqual(2);
    expect(filters[0].metadataName).toEqual('application');
    expect(filters[0].value).toEqual('Application X');
    expect(filters[1].metadataName).toEqual('host');
    expect(filters[1].value).toEqual('Host A');
    expect(service.getKey(route)).toEqual(`${KEY_DEBUG}?application=Application%20X&host=Host%20A`);
  });

  it('When activated route has filters in different order then same filters and same key', () => {
    let route = getDefaultActivatedRouteSnapshotWithPath(KEY_DEBUG);
    route.queryParams = {
      'filter-application': 'Application X',
      'filter-host': 'Host A',
    };
    const filters: FilterFromUrl[] = service.routeGetFilters(route);
    expect(filters.length).toEqual(2);
    expect(filters[0].metadataName).toEqual('application');
    expect(filters[0].value).toEqual('Application X');
    expect(filters[1].metadataName).toEqual('host');
    expect(filters[1].value).toEqual('Host A');
    expect(service.getKey(route)).toEqual(`${KEY_DEBUG}?application=Application%20X&host=Host%20A`);
  });
});

function getDefaultActivatedRouteSnapshotWithPath(path: string): ActivatedRouteSnapshot {
  return {
    url: [new UrlSegment('products', {}), new UrlSegment('42', {})],
    params: { id: '42' },
    queryParams: { sort: 'asc' },
    fragment: 'details',
    data: {},
    outlet: 'primary',
    component: null,
    routeConfig: {
      path,
    },
    root: {} as any,
    parent: null,
    firstChild: null,
    children: [],
    pathFromRoot: [] as any,
    paramMap: new Map() as any,
    queryParamMap: new Map() as any,
    toString: () => '/products/42',
    title: '',
  };
}
