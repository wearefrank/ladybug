import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TestFolderTreeComponent } from './test-folder-tree.component';
import { NgSimpleFileTree } from 'ng-simple-file-tree';

describe('TestFileTreeComponent', () => {
  let component: TestFolderTreeComponent;
  let fixture: ComponentFixture<TestFolderTreeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestFolderTreeComponent, NgSimpleFileTree],
    }).compileComponents();

    fixture = TestBed.createComponent(TestFolderTreeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
