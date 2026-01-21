package eu.jelinek.hranolky.config

/**
 * Product quality code mappings following Data Version Transparency (DVT) principle.
 *
 * Maps internal quality codes to human-readable display names.
 * These mappings are business data that may need updates as new products are added.
 */
object QualityConfig {

    /**
     * Maps internal quality codes to full display names
     * Used in detail views and reports
     */
    val fullQualityNames: Map<String, String> = mapOf(
        // Oak (DUB) grades
        "DUB-A|A" to "DUB A/A",
        "DUB-A|B" to "DUB A/B",
        "DUB-B|B" to "DUB B/B",
        "DUB-B|A" to "DUB B/A",
        "DUB-ABP" to "DUB A/B-P",
        "DUB-RST" to "DUB RUSTIK",
        "DUB-CNK" to "DUB CINK",
        "DUB-RSC" to "DUB RUSTIK CINK",

        // Stone pine (ZIRBE) grades
        "ZIR-ZIR" to "ZIRBE",
        "ZIR-BMS" to "ZIRBE MS",
        "ZIR-CNK" to "ZIRBE CINK",

        // Mixed grades
        "ZBD-BDC" to "ZIRBE+BUK/DUB/BUK CINK/DUB CINK",
        "ZBD-CNK" to "ZIRBE CINK+BUK/DUB/BUK CINK/DUB CINK",

        // Beech (BUK) grades
        "BUK-BUK" to "BUK",
        "BUK-CNK" to "BUK CINK",

        // Other wood types
        "JSN-JSN" to "JASAN",  // Ash
        "KŠT-KŠT" to "KAŠTAN" // Chestnut
    )

    /**
     * Gets the full quality name for a given code, or returns the code if not found.
     */
    fun getFullQualityName(code: String?): String {
        if (code == null) return ""
        return fullQualityNames[code] ?: code
    }
}
