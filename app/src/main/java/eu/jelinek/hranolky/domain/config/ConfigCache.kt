package eu.jelinek.hranolky.domain.config

import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory cache for configuration values with expiration.
 * Thread-safe implementation using ConcurrentHashMap.
 */
class ConfigCache(
    private val expirationMs: Long = DEFAULT_EXPIRATION_MS
) {
    private val cache = ConcurrentHashMap<String, CachedValue<*>>()

    /**
     * Get a cached value or fetch it if not cached or expired.
     *
     * @param key Cache key
     * @param fetch Suspend function to fetch the value if not cached
     * @return The cached or freshly fetched value, or null if fetch fails
     */
    suspend fun <T> getOrFetch(
        key: String,
        fetch: suspend () -> T?
    ): T? {
        @Suppress("UNCHECKED_CAST")
        val cached = cache[key] as? CachedValue<T>

        if (cached != null && !isExpired(cached.timestamp)) {
            return cached.value
        }

        return try {
            fetch()?.also { value ->
                cache[key] = CachedValue(value, System.currentTimeMillis())
            }
        } catch (e: Exception) {
            // If fetch fails, return expired cached value if available
            cached?.value
        }
    }

    /**
     * Check if a cache entry is expired.
     */
    private fun isExpired(timestamp: Long): Boolean =
        System.currentTimeMillis() - timestamp > expirationMs

    /**
     * Invalidate all cached values.
     */
    fun invalidate() {
        cache.clear()
    }

    /**
     * Invalidate a specific cached value.
     */
    fun invalidate(key: String) {
        cache.remove(key)
    }

    /**
     * Get the number of cached entries (for testing).
     */
    fun size(): Int = cache.size

    /**
     * Check if a key is cached and not expired.
     */
    fun isCached(key: String): Boolean {
        @Suppress("UNCHECKED_CAST")
        val cached = cache[key] as? CachedValue<*>
        return cached != null && !isExpired(cached.timestamp)
    }

    /**
     * Cached value with timestamp.
     */
    private data class CachedValue<T>(
        val value: T,
        val timestamp: Long
    )

    companion object {
        /** Default cache expiration: 1 hour */
        const val DEFAULT_EXPIRATION_MS = 3600_000L

        /** Cache keys */
        const val KEY_QUALITY_MAPPINGS = "qualityMappings"
        const val KEY_DIMENSION_ADJUSTMENTS = "dimensionAdjustments"
        const val KEY_INVENTORY_CHECK_PERIOD = "inventoryCheckPeriodDays"
        const val KEY_COLLECTION_CONFIG = "collectionConfig"
    }
}
