import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ToastComponent } from './toast.component';
import { ToastService } from '../../services/toast.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { of } from 'rxjs';
import { DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { Toast } from '../../interfaces/toast';
import { By } from '@angular/platform-browser';

describe('ToastComponent', () => {
  let component: ToastComponent;
  let fixture: ComponentFixture<ToastComponent>;
  let mockToastService: jasmine.SpyObj<ToastService>;
  let mockNgbModal: jasmine.SpyObj<NgbModal>;

  beforeEach(async () => {
    mockToastService = jasmine.createSpyObj('ToastService', ['toastObservable']);
    mockToastService.toastObservable = of({
      title: 'Test Toast',
      message: 'Detailed error message',
      detailed: 'Detailed error message',
      type: 'danger',
    } as Toast);

    mockNgbModal = jasmine.createSpyObj('NgbModal', ['open']);

    await TestBed.configureTestingModule({
      imports: [ToastComponent],
      providers: [
        { provide: ToastService, useValue: mockToastService },
        { provide: NgbModal, useValue: mockNgbModal },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ToastComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should open modal when a toast with detailed message is clicked', () => {
    const toastDebugElement: DebugElement = fixture.debugElement.query(By.css('ngb-toast'));

    // Simulate click on the toast item
    toastDebugElement.triggerEventHandler('click', null);

    // Assert that the modal opens and the selectedAlert is set correctly
    expect(mockNgbModal.open).toHaveBeenCalledWith(component.modal, {
      size: 'lg',
    });
  });
});
