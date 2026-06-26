import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FixedFilterInfoComponent } from './fixed-filter-info.component';

describe('FixedFilterInfoComponent', () => {
  let component: FixedFilterInfoComponent;
  let fixture: ComponentFixture<FixedFilterInfoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FixedFilterInfoComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(FixedFilterInfoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
