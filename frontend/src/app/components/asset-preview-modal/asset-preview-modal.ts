import { Component, input, output } from '@angular/core';
import { Asset } from '../../models/upload';

@Component({
  selector: 'app-asset-preview-modal',
  imports: [],
  templateUrl: './asset-preview-modal.html',
  styleUrl: './asset-preview-modal.css'
})
export class AssetPreviewModal {
  readonly asset = input.required<Asset>();
  readonly close = output<void>();

  protected mediaType(): 'image' | 'video' | 'audio' {
    const kind = this.asset().kind;
    if (kind === 'TRANSCODE' || kind === 'PREVIEW') return 'video';
    if (kind === 'AUDIO_TRANSCODE') return 'audio';
    return 'image'; // THUMBNAIL, POSTER, WAVEFORM
  }

  onClose() {
    this.close.emit();
  }
}
