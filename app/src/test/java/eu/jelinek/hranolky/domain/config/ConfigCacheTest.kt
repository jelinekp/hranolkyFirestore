package eu.jelinek.hranolky.domain.config

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConfigCacheTest {

    private lateinit var cache: ConfigCache
    private var currentTime = 0L

    @Before
    fun setup() {
        currentTime = 0L
        cache = ConfigCache(
            expirationMs = 1000, // 1 second expiration for testing
            timeProvider = { currentTime }
        )
    }

    @Test
    fun `getOrFetch returns fetched value on first call`() = runBlocking {
        val result = cache.getOrFetch("key") { "value" }

        assertEquals("value", result)
    }

    @Test
    fun `getOrFetch returns cached value on subsequent calls`() = runBlocking {
        var fetchCount = 0

        cache.getOrFetch("key") {
            fetchCount++
            "value"
        }
        cache.getOrFetch("key") {
            fetchCount++
            "new_value"
        }

        assertEquals(1, fetchCount)
    }

    @Test
    fun `getOrFetch fetches again after expiration`() = runBlocking {
        var fetchCount = 0

        cache.getOrFetch("key") {
            fetchCount++
            "value_$fetchCount"
        }

        // Advance time past expiration
        currentTime = 1500

        val result = cache.getOrFetch("key") {
            fetchCount++
            "value_$fetchCount"
        }

        assertEquals(2, fetchCount)
        assertEquals("value_2", result)
    }

    @Test
    fun `invalidate clears all cached values`() = runBlocking {
        cache.getOrFetch("key1") { "value1" }
        cache.getOrFetch("key2") { "value2" }

        cache.invalidate()

        assertEquals(0, cache.size())
    }

    @Test
    fun `invalidate specific key only removes that key`() = runBlocking {
        cache.getOrFetch("key1") { "value1" }
        cache.getOrFetch("key2") { "value2" }

        cache.invalidate("key1")

        assertEquals(1, cache.size())
        assertFalse(cache.isCached("key1"))
        assertTrue(cache.isCached("key2"))
    }

    @Test
    fun `getOrFetch returns null when fetch returns null`() = runBlocking {
        val result = cache.getOrFetch<String>("key") { null }

        assertNull(result)
    }

    @Test
    fun `getOrFetch returns expired cached value when fetch fails`() = runBlocking {
        // First fetch and cache
        val first: String? = cache.getOrFetch("key") { "cached_value" }
        assertEquals("cached_value", first)

        // Advance time past expiration
        currentTime = 1500

        // Fetch that throws exception should return the expired cached value
        var exceptionThrown = false
        val result: String? = cache.getOrFetch("key") {
            exceptionThrown = true
            throw RuntimeException("Fetch failed")
        }

        assertTrue("Fetch should have been attempted", exceptionThrown)
        assertEquals("cached_value", result)
    }

    @Test
    fun `isCached returns false for non-existent key`() {
        assertFalse(cache.isCached("nonexistent"))
    }

    @Test
    fun `isCached returns true for cached key`() = runBlocking {
        cache.getOrFetch("key") { "value" }

        assertTrue(cache.isCached("key"))
    }

    @Test
    fun `isCached returns false for expired key`() = runBlocking {
        cache.getOrFetch("key") { "value" }

        currentTime = 1500

        assertFalse(cache.isCached("key"))
    }

    // New tests for enhanced features

    @Test
    fun `default expiration is 24 hours`() {
        val defaultCache = ConfigCache()
        assertEquals(24 * 60 * 60 * 1000L, ConfigCache.DEFAULT_EXPIRATION_MS)
    }

    @Test
    fun `put directly adds value to cache`() {
        cache.put("key", "direct_value")

        assertTrue(cache.isCached("key"))
        assertEquals("direct_value", cache.getIfCached<String>("key"))
    }

    @Test
    fun `getIfCached returns null for non-existent key`() {
        assertNull(cache.getIfCached<String>("nonexistent"))
    }

    @Test
    fun `getIfCached returns null for expired key`() = runBlocking {
        cache.getOrFetch("key") { "value" }
        currentTime = 1500

        assertNull(cache.getIfCached<String>("key"))
    }

    @Test
    fun `version increments on put`() {
        val initialVersion = cache.getVersion()

        cache.put("key", "value")

        assertEquals(initialVersion + 1, cache.getVersion())
    }

    @Test
    fun `version increments on invalidate`() {
        val initialVersion = cache.getVersion()

        cache.invalidate()

        assertEquals(initialVersion + 1, cache.getVersion())
    }

    @Test
    fun `stats track hits and misses`() = runBlocking {
        // Miss
        cache.getOrFetch("key1") { "value1" }

        // Hit
        cache.getOrFetch("key1") { "value1" }

        // Another miss
        cache.getOrFetch("key2") { "value2" }

        val stats = cache.getStats()
        assertEquals(1, stats.hitCount)
        assertEquals(2, stats.missCount)
        assertEquals(0.333f, stats.hitRate, 0.01f)
    }

    @Test
    fun `resetStats clears hit and miss counters`() = runBlocking {
        cache.getOrFetch("key") { "value" }
        cache.getOrFetch("key") { "value" }

        cache.resetStats()

        val stats = cache.getStats()
        assertEquals(0, stats.hitCount)
        assertEquals(0, stats.missCount)
    }

    @Test
    fun `update listener is called when value changes`() {
        var notifiedKey: String? = null
        cache.addUpdateListener { key -> notifiedKey = key }

        cache.put("test_key", "value")

        assertEquals("test_key", notifiedKey)
    }

    @Test
    fun `update listener is not called when same value is put`() = runBlocking {
        cache.put("key", "same_value")

        var listenerCalled = false
        cache.addUpdateListener { listenerCalled = true }

        cache.put("key", "same_value")

        assertFalse(listenerCalled)
    }

    @Test
    fun `update listener can be removed`() {
        var callCount = 0
        val listener: (String) -> Unit = { callCount++ }

        cache.addUpdateListener(listener)
        cache.put("key1", "value1")
        assertEquals(1, callCount)

        cache.removeUpdateListener(listener)
        cache.put("key2", "value2")
        assertEquals(1, callCount) // Should not have been called again
    }

    @Test
    fun `stats size reflects cache size`() = runBlocking {
        cache.getOrFetch("key1") { "value1" }
        cache.getOrFetch("key2") { "value2" }

        assertEquals(2, cache.getStats().size)
    }
}
