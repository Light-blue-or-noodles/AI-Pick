import { ref, shallowRef, onMounted, onActivated } from 'vue';
import { getPageCache, setPageCache } from '@/utils/pageCache';

function resolveCacheKey(options) {
  if (typeof options.cacheKey === 'function') {
    return options.cacheKey();
  }
  return options.cacheKey || null;
}

/**
 * @template T
 * @param {() => Promise<T>} fetcher
 * @param {object} [options]
 * @param {string|(() => string|null)} [options.cacheKey]
 * @param {number} [options.cacheTtlMs]
 * @param {T} [options.empty]
 * @param {boolean} [options.immediate]
 * @param {boolean} [options.reloadOnActivated] 有缓存时静默刷新
 */
export function usePageLoad(fetcher, options = {}) {
  const {
    cacheTtlMs = 5 * 60 * 1000,
    empty = null,
    immediate = true,
    reloadOnActivated = false
  } = options;

  const initialKey = resolveCacheKey(options);
  const initialCached = initialKey ? getPageCache(initialKey, cacheTtlMs) : null;
  const hasInitialCache = initialCached !== null && initialCached !== undefined;

  const data = shallowRef(hasInitialCache ? initialCached : empty);
  const pending = ref(!hasInitialCache);
  const error = ref(null);

  async function load(loadOpts = {}) {
    const key = resolveCacheKey(options);
    const cached = key ? getPageCache(key, cacheTtlMs) : null;
    const hasCache = cached !== null && cached !== undefined;
    const silent = loadOpts.silent ?? hasCache;

    if (hasCache) {
      data.value = cached;
      pending.value = false;
    } else if (!silent) {
      pending.value = true;
    }

    try {
      const result = await fetcher();
      data.value = result;
      error.value = null;
      if (key) {
        setPageCache(key, result);
      }
      return result;
    } catch (e) {
      error.value = e;
      if (!silent && !hasCache) {
        data.value = empty;
      }
      throw e;
    } finally {
      pending.value = false;
    }
  }

  /** 下拉刷新 / 手动更新：不闪空 */
  function refresh() {
    return load({ silent: true });
  }

  if (immediate) {
    onMounted(() => {
      load({ silent: hasInitialCache });
    });
  }

  if (reloadOnActivated) {
    onActivated(() => {
      load({ silent: true });
    });
  }

  return {
    data,
    pending,
    error,
    load,
    refresh,
    hasInitialCache
  };
}
