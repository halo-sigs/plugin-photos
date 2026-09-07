import { createSharedComposable, useLocalStorage } from "@vueuse/core";

export const usePhotoPreferences = createSharedComposable(() => {
  // Read the saved preference before image components can mount.
  const informationOnly = useLocalStorage("plugin:photos:informationOnly", true);

  return { informationOnly };
});
