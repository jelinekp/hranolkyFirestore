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

    @Before
    fun setup() {
        cache = ConfigCache(expirationMs = 1000) // 1 second expiration for testing
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
        val shortCache = ConfigCache(expirationMs = 50)
        var fetchCount = 0

        shortCache.getOrFetch("key") {
            fetchCount++
            "value_$fetchCount"
        }

        delay(100) // Wait for expiration

        val result = shortCache.getOrFetch("key") {
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
        val shortCache = ConfigCache(expirationMs = 50)

        // First fetch and cache
        val first: String? = shortCache.getOrFetch("key") { "cached_value" }
        assertEquals("cached_value", first)

        delay(100) // Wait for expiration

        // Fetch that throws exception should return the expired cached value
        var exceptionThrown = false
        val result: String? = shortCache.getOrFetch("key") {
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
        val shortCache = ConfigCache(expirationMs = 50)

        shortCache.getOrFetch("key") { "value" }

        delay(100)

        assertFalse(shortCache.isCached("key"))
    }
}
