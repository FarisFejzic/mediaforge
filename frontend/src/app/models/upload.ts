export type MediaType = 'IMAGE' | 'VIDEO' | 'AUDIO';
export type UploadStatus = 'RECEIVED' | 'PROCESSING' | 'DONE' | 'FAILED';

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
