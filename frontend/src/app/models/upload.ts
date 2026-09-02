export type MediaType = 'IMAGE' | 'VIDEO' | 'AUDIO';
export type UploadStatus = 'RECEIVED' | 'PROCESSING' | 'DONE' | 'FAILED';
export type JobType = 'THUMBNAIL' | 'POSTER' | 'TRANSCODE' | 'PREVIEW' | 'METADATA' | 'WAVEFORM' | 'AUDIO_TRANSCODE';
export type JobStatus = 'QUEUED' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface Upload {
  id: string;
  originalName: string;
  mediaType: MediaType;
  sizeBytes: number;
  status: UploadStatus;
  createdAt: string;
}

// Spring Page wrapper — generic so we can reuse it for any paginated list
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface Job {
  id: string;
  uploadId: string;
  type: JobType;
  status: JobStatus;
  attempts: number;
  error: string | null;
  startedAt: string | null;
  finishedAt: string | null;
}

export interface Asset {
  id: string;
  jobId: string;
  kind: string;
  sizeBytes: number;
  createdAt: string;
}

export interface UploadDetail extends Upload {
  jobs: Job[];
  assets: Asset[];
}
