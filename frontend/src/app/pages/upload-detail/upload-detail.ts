import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { UploadService } from '../../services/upload';
import { WebSocketService } from '../../services/websocket';
import { UploadDetail, Job } from '../../models/upload';

@Component({
  selector: 'app-upload-detail',
  imports: [DatePipe, RouterLink],
  templateUrl: './upload-detail.html',
  styleUrl: './upload-detail.css'
})
export class UploadDetailPage implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private uploadService = inject(UploadService);
  private ws = inject(WebSocketService);

  protected detail = signal<UploadDetail | null>(null);
  protected loading = signal(true);
  protected error = signal<string | null>(null);

  private wsSub?: Subscription;
  private uploadId!: string;

  ngOnInit() {
    this.uploadId = this.route.snapshot.paramMap.get('id')!;

    this.uploadService.getById(this.uploadId).subscribe({
      next: (detail) => {
        this.detail.set(detail);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load upload.');
        this.loading.set(false);
      }
    });

    // subscribe to live status updates for this upload
    this.wsSub = this.ws.watchUpload(this.uploadId).subscribe(event => {
      this.applyEvent(event);
    });
  }

  ngOnDestroy() {
    this.wsSub?.unsubscribe();
  }

  private applyEvent(event: any) {
    this.detail.update(current => {
      if (!current) return current;

      if (event.eventType === 'UPLOAD') {
        // upload status changed → update the header status
        return { ...current, status: event.status };
      } else {
        // job status changed → update the matching job
        const jobs = current.jobs.map(job =>
          job.id === event.jobId ? { ...job, status: event.status as Job['status'] } : job
        );
        return { ...current, jobs };
      }
    });
  }

  protected downloadUrl(assetId: string): string {
    return `/api/assets/${assetId}/download`;
  }
}
