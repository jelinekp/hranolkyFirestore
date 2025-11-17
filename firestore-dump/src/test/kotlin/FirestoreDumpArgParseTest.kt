import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FirestoreDumpArgParseTest {
    @Test
    fun parseBasicArgs() {
        val args = listOf("--output", "test.json", "--pretty", "--flat")
        val cfg = callParse(args)
        assertEquals("test.json", cfg.output.name)
        assertTrue(cfg.pretty)
        assertTrue(cfg.flat)
    }

    private fun callParse(a: List<String>): DumpConfig {
        val loader = this::class.java.classLoader
        val m = loader.loadClass("FirestoreDumpMainKt")
        val f = m.getDeclaredMethod("parseArgs", java.util.List::class.java)
        f.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return f.invoke(null, a) as DumpConfig
    }
}
