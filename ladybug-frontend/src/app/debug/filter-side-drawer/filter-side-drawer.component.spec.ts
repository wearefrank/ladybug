import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FilterSideDrawerComponent } from './filter-side-drawer.component';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { FormsModule } from '@angular/forms';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { View } from '../../shared/interfaces/view';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('FilterSideDrawerComponent', () => {
  let component: FilterSideDrawerComponent;
  let fixture: ComponentFixture<FilterSideDrawerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MatAutocompleteModule, FormsModule, FilterSideDrawerComponent],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(FilterSideDrawerComponent);
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
