import { ref, shallowRef } from "vue";

/**
 * Tracks an in-flight batch operation with bounded concurrency. Exposes
 * `isBatchOperating` and a progress object so the toolbar can show
 * "已处理 N / M" while the runner walks the queue.
 */
export function useBatchOperations() {
  const isBatchOperating = shallowRef(false);
  // Mutated in place (current++) so a deep ref keeps progress reactive.
  const batchProgress = ref({ current: 0, total: 0 });

  const runWithConcurrency = async <T, R>(
    items: T[],
    fn: (item: T) => Promise<R>,
    concurrency = 5,
  ): Promise<(R | undefined)[]> => {
    const results: (R | undefined)[] = [];
    const executing: Promise<unknown>[] = [];
    batchProgress.value = { current: 0, total: items.length };

    for (let i = 0; i < items.length; i++) {
      const p = fn(items[i]!)
        .then((r) => {
          results[i] = r;
          batchProgress.value.current++;
          return r;
        })
        .catch((e) => {
          console.error(e);
          results[i] = undefined;
          batchProgress.value.current++;
          throw e;
        });

      results.push(undefined as unknown as R);
      executing.push(p);

      if (executing.length >= concurrency) {
        await Promise.race(executing);
        const idx = executing.findIndex((ep) => ep === p);
        if (idx >= 0) executing.splice(idx, 1);
      }
    }

    await Promise.all(executing);
    return results;
  };

  return {
    isBatchOperating,
    batchProgress,
    runWithConcurrency,
  };
}
