import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { UploadService } from '../../services/upload';
import { WebSocketService } from '../../services/websocket';
import { Upload } from '../../models/upload';
import { Uploader } from '../../components/uploader/uploader';

@Component({
  selector: 'app-dashboard',
  imports: [DatePipe, Uploader, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit, OnDestroy {
  private uploadService = inject(UploadService);
  private ws = inject(WebSocketService);

  protected uploads = signal<Upload[]>([]);
  protected loading = signal(true);
  protected error = signal<string | null>(null);

  private subs = new Map<string, Subscription>();

  ngOnInit() {
    this.loadUploads();
  }

  ngOnDestroy() {
    this.subs.forEach(sub => sub.unsubscribe());
    this.subs.clear();
  }

  private loadUploads() {
    this.uploadService.list().subscribe({
      next: (page) => {
        this.uploads.set(page.content);
        this.loading.set(false);
        // subscribe to live updates for each upload
        page.content.forEach(u => this.watch(u.id));
      },
      error: () => {
        this.error.set('Failed to load uploads.');
        this.loading.set(false);
      }
    });
  }

  private watch(uploadId: string) {
    if (this.subs.has(uploadId)) return; // already watching
    const sub = this.ws.watchUpload(uploadId).subscribe(event => {
      if (event.eventType === 'UPLOAD') {
        this.uploads.update(list =>
          list.map(u => u.id === uploadId ? { ...u, status: event.status } : u)
        );
      }
    });
    this.subs.set(uploadId, sub);
  }

  onUploaded(upload: Upload) {
    this.uploads.update(list => [upload, ...list]);
    this.watch(upload.id); // watch the new upload too
  }
}
