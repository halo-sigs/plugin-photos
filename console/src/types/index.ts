import type { PhotoExif } from "@/api/generated";

export interface PhotoGroupFormState {
  name?: string;
  displayName: string;
  priority: number;
  annotations?: Record<string, unknown>;
}

export interface PhotoFormState {
  url: string;
  displayName: string;
  groupName?: string;
  description?: string;
  tags?: string[];
  annotations?: Record<string, unknown>;
  exif?: PhotoExif;
}
