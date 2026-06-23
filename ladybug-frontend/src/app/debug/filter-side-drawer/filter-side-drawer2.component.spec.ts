import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FilterSideDrawer2Component } from './filter-side-drawer2.component';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { FormsModule } from '@angular/forms';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { View } from '../../shared/interfaces/view';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('FilterSideDrawerComponent', () => {
  let component: FilterSideDrawer2Component;
  let fixture: ComponentFixture<FilterSideDrawer2Component>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MatAutocompleteModule, FormsModule, FilterSideDrawer2Component],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(FilterSideDrawer2Component);
    component = fixture.componentInstance;
    component.currentView = {
      storageName: 'mockStorage',
      metadataNames: ['mockMetadata'],
    } as View;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
