import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UploadDetail } from './upload-detail';

describe('UploadDetail', () => {
  let component: UploadDetail;
  let fixture: ComponentFixture<UploadDetail>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UploadDetail],
    }).compileComponents();

    fixture = TestBed.createComponent(UploadDetail);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
