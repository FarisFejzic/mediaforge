import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AssetPreviewModal } from './asset-preview-modal';

describe('AssetPreviewModal', () => {
  let component: AssetPreviewModal;
  let fixture: ComponentFixture<AssetPreviewModal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AssetPreviewModal],
    }).compileComponents();

    fixture = TestBed.createComponent(AssetPreviewModal);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
