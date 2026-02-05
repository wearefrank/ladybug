import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DifferenceModalComponent } from './difference-modal.component';

describe('DifferenceModalComponent', () => {
  let component: DifferenceModalComponent;
  let fixture: ComponentFixture<DifferenceModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DifferenceModalComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(DifferenceModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
