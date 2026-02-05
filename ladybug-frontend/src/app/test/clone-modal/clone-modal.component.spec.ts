import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CloneModalComponent } from './clone-modal.component';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('CloneModalComponent', () => {
  let component: CloneModalComponent;
  let fixture: ComponentFixture<CloneModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CloneModalComponent],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CloneModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
