import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ViewDropdownComponent } from './view-dropdown.component';

describe('ViewDropdownComponent', () => {
  let component: ViewDropdownComponent;
  let fixture: ComponentFixture<ViewDropdownComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ViewDropdownComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ViewDropdownComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
