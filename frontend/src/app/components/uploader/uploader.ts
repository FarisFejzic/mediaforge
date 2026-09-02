import { Component, inject, signal, output } from '@angular/core';
import { UploadService } from '../../services/upload';
import { Upload } from '../../models/upload';

@Component({
  selector: 'app-uploader',
  imports: [],
  templateUrl: './uploader.html',
  styleUrl: './uploader.css'
})
export class Uploader {
  private uploadService = inject(UploadService);

  protected uploading = signal(false);
  protected error = signal<string | null>(null);
  protected dragOver = signal(false);

  // emits when an upload succeeds, so the parent (dashboard) can refresh
  readonly uploaded = output<Upload>();

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      this.uploadFile(file);
      input.value = ''; // reset so the same file can be picked again
    }
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.dragOver.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (file) {
      this.uploadFile(file);
    }
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    this.dragOver.set(true);
  }

  onDragLeave() {
    this.dragOver.set(false);
  }

  private uploadFile(file: File) {
    this.error.set(null);
    this.uploading.set(true);
    this.uploadService.upload(file).subscribe({
      next: (upload) => {
        this.uploading.set(false);
        this.uploaded.emit(upload);
      },
      error: () => {
        this.uploading.set(false);
        this.error.set('Upload failed.');
      }
    });
  }
}
