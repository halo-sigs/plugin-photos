import type { Photo } from "@/api/generated";
import { computed, shallowRef, watch, type Ref } from "vue";

/**
 * Set-based batch selection for photos. Returns readonly state plus explicit
 * action functions; consumers should not mutate the underlying Set directly.
 */
export function usePhotoSelection(photos: Ref<Photo[] | undefined>) {
  // Replaced wholesale on clear/refetch — shallowRef avoids deep proxying of
  // the Set entries (Photo objects).
  const selectedPhotos = shallowRef<Set<Photo>>(new Set<Photo>());
  const checkedAll = shallowRef(false);

  const selectedCount = computed(() => selectedPhotos.value.size);

  const isSelected = (photo: Photo) => {
    for (const item of selectedPhotos.value) {
      if (item.metadata.name === photo.metadata.name) return true;
    }
    return false;
  };

  const toggle = (photo: Photo, checked?: boolean) => {
    const next = new Set(selectedPhotos.value);
    const shouldSelect = checked !== undefined ? checked : !isSelected(photo);
    if (shouldSelect) {
      next.add(photo);
    } else {
      // Remove by metadata.name to be resilient against object identity.
      for (const item of next) {
        if (item.metadata.name === photo.metadata.name) {
          next.delete(item);
          break;
        }
      }
    }
    selectedPhotos.value = next;
  };

  const setAll = (checkAll: boolean) => {
    if (checkAll) {
      selectedPhotos.value = new Set(photos.value || []);
    } else {
      clear();
    }
  };

  const clear = () => {
    selectedPhotos.value = new Set<Photo>();
  };

  // Keep checkedAll in sync with the current page selection state.
  watch([selectedCount, () => photos.value?.length], ([count, length]) => {
    checkedAll.value = !!length && count === length;
  });

  return {
    selectedPhotos,
    selectedCount,
    checkedAll,
    isSelected,
    toggle,
    setAll,
    clear,
  };
}
