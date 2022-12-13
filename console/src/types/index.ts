export interface Metadata {
  name: string;
  generateName?: string;
  labels?: {
    [key: string]: string;
  } | null;
  annotations?: {
    [key: string]: string;
  } | null;
  version?: number | null;
  creationTimestamp?: string | null;
  deletionTimestamp?: string | null;
}

export interface PhotoGroupSpec {
  displayName: string;
  priority?: number;
  photos: string[];
}

export interface PhotoSpec {
  displayName: string;
  description?: string;
  url: string;
  cover?: string;
  priority?: number;
}

export interface Photo {
  spec: PhotoSpec;
  apiVersion: string;
  kind: string;
  metadata: Metadata;
}

export interface PhotoGroup {
  spec: PhotoGroupSpec;
  apiVersion: string;
  kind: string;
  metadata: Metadata;
}

export interface PhotoList {
  page: number;
  size: number;
  total: number;
  items: Array<Photo>;
  first: boolean;
  last: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface PhotoGroupList {
  page: number;
  size: number;
  total: number;
  items: Array<PhotoGroup>;
  first: boolean;
  last: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
}
