import { Component, inject, signal, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { UploadService } from '../../services/upload';
import { Upload } from '../../models/upload';
import { Uploader } from '../../components/uploader/uploader';

@Component({
  selector: 'app-dashboard',
  imports: [DatePipe, Uploader],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {
  private uploadService = inject(UploadService);

  protected uploads = signal<Upload[]>([]);
  protected loading = signal(true);
  protected error = signal<string | null>(null);

  ngOnInit() {
    this.loadUploads();
  }

  private loadUploads() {
    this.uploadService.list().subscribe({
      next: (page) => {
        this.uploads.set(page.content);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load uploads.');
        this.loading.set(false);
      }
    });
  }

  onUploaded(upload: Upload) {
    // prepend the new upload to the list immediately
    this.uploads.update(list => [upload, ...list]);
  }
}
