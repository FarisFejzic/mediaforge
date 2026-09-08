import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Upload, Page, UploadDetail } from '../models/upload';

@Injectable({ providedIn: 'root' })
export class UploadService {
  private http = inject(HttpClient);

  list(): Observable<Page<Upload>> {
    return this.http.get<Page<Upload>>('/api/uploads');
  }

  upload(file: File): Observable<Upload> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<Upload>('/api/uploads', form);
  }

  getById(id: string): Observable<UploadDetail> {
    return this.http.get<UploadDetail>(`/api/uploads/${id}`);
  }

  getDownloadUrl(assetId: string): Observable<{ url: string; expiresIn: number }> {
    return this.http.get<{ url: string; expiresIn: number }>(`/api/assets/${assetId}/download`);
  }
}
