import { photosConsoleApiClient } from "@/api";
import { useQuery } from "@tanstack/vue-query";
import { computed } from "vue";

export const QK_PHOTO_TAGS = "plugin:photos:tags";

export function usePhotoTags() {
  const { data: existingTags } = useQuery<string[]>({
    queryKey: [QK_PHOTO_TAGS],
    queryFn: async () => {
      const { data } = await photosConsoleApiClient.photo.listPhotoTags();
      return data;
    },
  });

  const tagOptions = computed(() => {
    return (existingTags.value || []).map((tag) => ({
      label: tag,
      value: tag,
    }));
  });

  return { existingTags, tagOptions };
}
