package eu.jelinek.hranolky.domain.config

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * In-memory cache for configuration values with expiration.
 * Thread-safe implementation using ConcurrentHashMap.
 *
 * Features:
 * - Configurable TTL (default: 24 hours)
 * - Stale-while-revalidate pattern: returns cached value while refreshing in background
 * - Version tracking for differential updates
 * - Cache statistics for monitoring
 */
class ConfigCache(
    private val expirationMs: Long = DEFAULT_EXPIRATION_MS,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val cache = ConcurrentHashMap<String, CachedValue<*>>()
    private val configVersion = AtomicLong(0)
    private val hitCount = AtomicLong(0)
    private val missCount = AtomicLong(0)

    /** Callbacks for cache update notifications */
    private val updateListeners = mutableListOf<(String) -> Unit>()

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
            hitCount.incrementAndGet()
            return cached.value
        }

        missCount.incrementAndGet()

        return try {
            fetch()?.also { value ->
                put(key, value)
            }
        } catch (@Suppress("UNUSED_PARAMETER") e: Exception) {
            // If fetch fails, return expired cached value if available
            cached?.value
        }
    }

    /**
     * Put a value directly into the cache.
     * Used for background updates and differential updates.
     */
    fun <T> put(key: String, value: T) {
        val oldValue = cache[key]
        cache[key] = CachedValue(value, timeProvider())
        configVersion.incrementAndGet()

        // Notify listeners only if value changed
        if (oldValue?.value != value) {
            updateListeners.forEach { it(key) }
        }
    }

    /**
     * Check if a cache entry is expired.
     */
    private fun isExpired(timestamp: Long): Boolean =
        timeProvider() - timestamp > expirationMs

    /**
     * Invalidate all cached values.
     */
    fun invalidate() {
        cache.clear()
        configVersion.incrementAndGet()
    }

    /**
     * Invalidate a specific cached value.
     */
    fun invalidate(key: String) {
        cache.remove(key)
        configVersion.incrementAndGet()
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
     * Get the current cache version.
     * Increments on every put or invalidate operation.
     */
    fun getVersion(): Long = configVersion.get()

    /**
     * Get cache statistics for monitoring.
     */
    fun getStats(): CacheStats = CacheStats(
        hitCount = hitCount.get(),
        missCount = missCount.get(),
        size = cache.size,
        version = configVersion.get()
    )

    /**
     * Reset cache statistics.
     */
    fun resetStats() {
        hitCount.set(0)
        missCount.set(0)
    }

    /**
     * Add a listener for cache updates.
     * Called when a cache entry is added or updated with a different value.
     */
    fun addUpdateListener(listener: (String) -> Unit) {
        updateListeners.add(listener)
    }

    /**
     * Remove an update listener.
     */
    fun removeUpdateListener(listener: (String) -> Unit) {
        updateListeners.remove(listener)
    }

    /**
     * Get cached value without fetching (for differential updates).
     * Returns null if not cached or expired.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getIfCached(key: String): T? {
        val cached = cache[key] as? CachedValue<T>
        return if (cached != null && !isExpired(cached.timestamp)) cached.value else null
    }

    /**
     * Cached value with timestamp.
     */
    private data class CachedValue<T>(
        val value: T,
        val timestamp: Long
    )

    /**
     * Cache statistics for monitoring.
     */
    data class CacheStats(
        val hitCount: Long,
        val missCount: Long,
        val size: Int,
        val version: Long
    ) {
        val hitRate: Float
            get() = if (hitCount + missCount > 0) {
                hitCount.toFloat() / (hitCount + missCount)
            } else 0f
    }

    companion object {
        /** Default cache expiration: 24 hours */
        const val DEFAULT_EXPIRATION_MS = 24 * 60 * 60 * 1000L // 24 hours

        /** Short cache expiration: 1 hour (for frequently changing data) */
        const val SHORT_EXPIRATION_MS = 60 * 60 * 1000L // 1 hour

        /** Cache keys */
        const val KEY_QUALITY_MAPPINGS = "qualityMappings"
        const val KEY_DIMENSION_ADJUSTMENTS = "dimensionAdjustments"
        const val KEY_INVENTORY_CHECK_PERIOD = "inventoryCheckPeriodDays"
        const val KEY_COLLECTION_CONFIG = "collectionConfig"
    }
}
