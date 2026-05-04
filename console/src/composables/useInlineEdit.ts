import { photosCoreApiClient } from "@/api";
import type { JsonPatchInner, Photo } from "@/api/generated";
import { Toast } from "@halo-dev/components";
import { nextTick, ref, shallowRef } from "vue";

export type InlineEditField = "displayName" | "groupName" | "tags";

export interface InlineEditCell {
  photoName: string;
  field: InlineEditField;
}

export interface UseInlineEditOptions {
  onSaved?: () => void;
  /** Debounce window in ms before persisting the edit. */
  debounceMs?: number;
}

/**
 * Inline cell editing state and actions. Tracks the active cell, the in-flight
 * value/tags buffer, and persists changes through the standard photo update
 * endpoint with a small debounce so chained `enter`/`blur` events do not race.
 */
export function useInlineEdit(options: UseInlineEditOptions = {}) {
  const { onSaved, debounceMs = 300 } = options;

  const editingCell = shallowRef<InlineEditCell | null>(null);
  const editingValue = shallowRef("");
  const editingTags = ref<string[]>([]);

  let saveTimeout: ReturnType<typeof setTimeout> | null = null;

  const start = (photo: Photo, field: InlineEditField) => {
    editingCell.value = { photoName: photo.metadata.name, field };
    if (field === "tags") {
      editingTags.value = [...(photo.spec.tags || [])];
    } else {
      editingValue.value = (photo.spec[field] as string) || "";
    }
    nextTick(() => {
      const input = document.querySelector(`[data-edit-field="${field}-${photo.metadata.name}"]`);
      if (input) {
        (input as HTMLElement).focus();
      }
    });
  };

  const commit = (photo: Photo, field: InlineEditField) => {
    if (saveTimeout) clearTimeout(saveTimeout);
    saveTimeout = setTimeout(async () => {
      const path = field === "tags" ? "/spec/tags" : `/spec/${field}`;
      const value = field === "tags" ? [...editingTags.value] : editingValue.value;
      const patch: Array<JsonPatchInner> = [{ op: "add", path, value }];
      try {
        await photosCoreApiClient.photo.patchPhoto({
          name: photo.metadata.name,
          jsonPatchInner: patch,
        });
        onSaved?.();
      } catch (e) {
        console.error(e);
        Toast.error("保存失败");
      } finally {
        editingCell.value = null;
      }
    }, debounceMs);
  };

  const cancel = () => {
    if (saveTimeout) clearTimeout(saveTimeout);
    editingCell.value = null;
  };

  return {
    editingCell,
    editingValue,
    editingTags,
    start,
    commit,
    cancel,
  };
}
