@file:Suppress("UNCHECKED_CAST")

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.Timestamp
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.io.FileInputStream

/**
 * Firestore migration upload utility.
 * Reads dump.json and uploads to new Firestore database with transformed structure.
 *
 * CLI Args:
 *   --input <path> (required) input dump.json file
 *   --credentials <path.json> (required) service account key
 *   --dry-run (optional) simulate without writing to Firestore
 */
fun main(args: Array<String>) {
    val config = parseUploadArgs(args.toList())

    println("[firestore-upload] Starting upload${if (config.dryRun) " (DRY RUN)" else ""}")
    println("[firestore-upload] Input: ${config.input}")
    println("[firestore-upload] Credentials: ${config.credentialsPath}")

    val firestore = if (!config.dryRun) buildFirestoreClient(config) else null

    println("[firestore-upload] Reading dump file...")
    val dumpContent = config.input.readText()
    val rootJson = Json.parseToJsonElement(dumpContent).jsonObject

    println("[firestore-upload] Processing collections...")

    var stats = UploadStats()

    // Process WarehouseSlots -> Hranolky and Sparovky
    rootJson["WarehouseSlots"]?.jsonObject?.let { warehouseSlots ->
        stats += processWarehouseSlots(firestore, warehouseSlots, config.dryRun)
    }

    // Process WeeklyBeamReports -> WeeklyReports/Hranolky/WeeklyData
    rootJson["WeeklyBeamReports"]?.jsonObject?.let { weeklyBeamReports ->
        stats += processWeeklyBeamReports(firestore, weeklyBeamReports, config.dryRun)
    }

    // Process WeeklyJointerReports -> WeeklyReports/Sparovky/WeeklyData
    rootJson["WeeklyJointerReports"]?.jsonObject?.let { weeklyJointerReports ->
        stats += processWeeklyJointerReports(firestore, weeklyJointerReports, config.dryRun)
    }

    // Process AppConfig (as-is)
    rootJson["AppConfig"]?.jsonObject?.let { appConfig ->
        stats += processAppConfig(firestore, appConfig, config.dryRun)
    }

    // Process Devices (as-is)
    rootJson["Devices"]?.jsonObject?.let { devices ->
        stats += processDevices(firestore, devices, config.dryRun)
    }

    println("\n[firestore-upload] Upload complete${if (config.dryRun) " (DRY RUN - NO DATA WRITTEN)" else ""}!")
    println("[firestore-upload] Statistics:")
    println("  - Hranolky documents: ${stats.hranolkyDocs}")
    println("  - Sparovky documents: ${stats.sparovkyDocs}")
    println("  - SlotActions: ${stats.slotActions}")
    println("  - SlotWeeklyReports: ${stats.slotWeeklyReports}")
    println("  - Weekly Hranolky reports: ${stats.weeklyHranolkyReports}")
    println("  - Weekly Sparovky reports: ${stats.weeklySparovkyReports}")
    println("  - AppConfig documents: ${stats.appConfigDocs}")
    println("  - Devices: ${stats.devices}")
    println("  - Total documents written: ${stats.totalDocs}")
}

data class UploadConfig(
    val input: File,
    val credentialsPath: File,
    val dryRun: Boolean
)

/**
 * Represents a single write operation to be batched
 */
private data class WriteOperation(
    val collection: String,
    val documentPath: String,
    val data: Map<String, Any>,
    val description: String
)

/**
 * Batch writer that accumulates write operations and commits in batches of 500
 */
private class BatchWriter(
    private val firestore: Firestore?,
    private val dryRun: Boolean
) {
    private val operations = mutableListOf<WriteOperation>()
    private var totalWritten = 0

    companion object {
        const val BATCH_SIZE = 500
    }

    fun add(operation: WriteOperation) {
        operations.add(operation)

        if (operations.size >= BATCH_SIZE) {
            flush()
        }
    }

    fun flush() {
        if (operations.isEmpty()) return

        if (dryRun) {
            operations.forEach { op ->
                println("  [DRY RUN] ${op.description}")
            }
        } else {
            val batch = firestore!!.batch()
            operations.forEach { op ->
                // Handle both root collection and subcollection paths
                val docRef = if (op.documentPath.contains("/")) {
                    // Subcollection path like "DUB-A-20-50/SlotActions/abc123"
                    firestore.collection(op.collection).document(op.documentPath)
                } else {
                    // Simple document path
                    firestore.collection(op.collection).document(op.documentPath)
                }
                batch.set(docRef, op.data)
            }
            batch.commit().get()
            totalWritten += operations.size
            println("  [Batch committed: ${operations.size} operations, total: $totalWritten]")
        }

        operations.clear()
    }

    fun getTotalWritten(): Int = totalWritten
}

data class UploadStats(
    var hranolkyDocs: Int = 0,
    var sparovkyDocs: Int = 0,
    var slotActions: Int = 0,
    var slotWeeklyReports: Int = 0,
    var weeklyHranolkyReports: Int = 0,
    var weeklySparovkyReports: Int = 0,
    var appConfigDocs: Int = 0,
    var devices: Int = 0
) {
    val totalDocs: Int
        get() = hranolkyDocs + sparovkyDocs + slotActions + slotWeeklyReports +
                weeklyHranolkyReports + weeklySparovkyReports + appConfigDocs + devices

    operator fun plus(other: UploadStats): UploadStats {
        return UploadStats(
            hranolkyDocs = this.hranolkyDocs + other.hranolkyDocs,
            sparovkyDocs = this.sparovkyDocs + other.sparovkyDocs,
            slotActions = this.slotActions + other.slotActions,
            slotWeeklyReports = this.slotWeeklyReports + other.slotWeeklyReports,
            weeklyHranolkyReports = this.weeklyHranolkyReports + other.weeklyHranolkyReports,
            weeklySparovkyReports = this.weeklySparovkyReports + other.weeklySparovkyReports,
            appConfigDocs = this.appConfigDocs + other.appConfigDocs,
            devices = this.devices + other.devices
        )
    }
}

private fun parseUploadArgs(args: List<String>): UploadConfig {
    if (args.isEmpty()) uploadUsageAndExit()
    var input: File? = null
    var credentials: File? = null
    var dryRun = false
    var i = 0
    while (i < args.size) {
        when (val a = args[i]) {
            "--input" -> input = File(args.getOrNull(++i) ?: uploadErr("Missing value for --input"))
            "--credentials" -> credentials = File(args.getOrNull(++i) ?: uploadErr("Missing value for --credentials"))
            "--dry-run" -> dryRun = true
            "-h", "--help" -> uploadUsageAndExit()
            else -> uploadErr("Unknown arg: $a")
        }
        i++
    }
    if (input == null) uploadErr("--input is required")
    if (credentials == null) uploadErr("--credentials is required")
    if (!input.exists()) uploadErr("Input file does not exist: ${input.absolutePath}")
    if (!credentials.exists()) uploadErr("Credentials file does not exist: ${credentials.absolutePath}")
    return UploadConfig(input, credentials, dryRun)
}

private fun uploadUsageAndExit(): Nothing {
    println(
        """
        Firestore Upload/Migration Utility

        Required:
          --input <dump.json>
          --credentials <serviceAccount.json>

        Optional:
          --dry-run (simulate without writing to Firestore)

        Examples:
          ./gradlew :firestore-up:run --args="--input dump.json --credentials private_secret.json --dry-run"
          ./gradlew :firestore-up:run --args="--input dump.json --credentials private_secret.json"
        """.trimIndent()
    )
    kotlin.system.exitProcess(0)
}

private fun uploadErr(msg: String): Nothing {
    System.err.println("Error: $msg")
    kotlin.system.exitProcess(1)
}

private fun buildFirestoreClient(cfg: UploadConfig): Firestore {
    val creds = FileInputStream(cfg.credentialsPath).use { GoogleCredentials.fromStream(it) }
    val options = FirestoreOptions.getDefaultInstance().toBuilder()
        .setCredentials(creds)
        .build()
    return options.service
}

fun getFullQualityName(shortQuality: String): String {
    return when (shortQuality) {
        "DUB-A|A" -> "DUB A/A"
        "DUB-A|B" -> "DUB A/B"
        "DUB-B|B" -> "DUB B/B"
        "DUB-B|A" -> "DUB B/A"
        "DUB-ABP" -> "DUB A/B-P"
        "DUB-RST" -> "DUB RUSTIK"
        "DUB-CNK" -> "DUB CINK"
        "DUB-RSC" -> "DUB RUSTIK CINK"
        "ZIR-ZIR" -> "ZIRBE"
        "ZIR-BMS" -> "ZIRBE MS"
        "ZIR-CNK" -> "ZIRBE CINK"
        "ZBD-BDC" -> "ZIRBE+BUK/DUB/BUK CINK/DUB CINK"
        "ZBD-CNK" -> "ZIRBE CINK+BUK/DUB/BUK CINK/DUB CINK"
        "BUK-BUK" -> "BUK"
        "BUK-CNK" -> "BUK CINK"
        "JSN-JSN" -> "JASAN"
        "KŠT-KŠT" -> "KAŠTAN"
        else -> shortQuality
    }
}

/**
 * Parse slot ID to extract indexed fields for filtering
 * Example: "DUB-A-20-50-1530" -> quality="DUB-A", thickness=20, width=50, length=1530
 */
private fun parseSlotIdToIndexedFields(slotId: String): Map<String, Any> {
    val parts = slotId.split("-")
    val fields = mutableMapOf<String, Any>()

    // Quality: first two segments (e.g., "DUB-A")
    if (parts.size >= 2) {
        fields["quality"] = getFullQualityName("${parts[0]}-${parts[1]}")
    }

    // Thickness: third segment
    if (parts.size >= 3) {
        parts[2].toDoubleOrNull()?.let { rawThickness ->
            val thickness = when (rawThickness) {
                20.0 -> 20.0
                27.0 -> 27.4
                42.0 -> 42.4
                else -> rawThickness
            }
            fields["thickness"] = thickness
        }
    }

    // Width: fourth segment
    if (parts.size >= 4) {
        parts[3].toDoubleOrNull()?.let { rawWidth ->
            val width = when (rawWidth) {
                42.0 -> 42.4
                else -> rawWidth
            }
            fields["width"] = width
        }
    }

    // Length: fifth segment
    if (parts.size >= 5) {
        parts[4].toIntOrNull()?.let { fields["length"] = it.toLong() }
    }

    return fields
}

private fun processWarehouseSlots(firestore: Firestore?, warehouseSlots: JsonObject, dryRun: Boolean): UploadStats {
    val stats = UploadStats()
    val batchWriter = BatchWriter(firestore, dryRun)

    println("\n[firestore-upload] Processing WarehouseSlots (${warehouseSlots.size} documents)...")

    for ((docId, docData) in warehouseSlots) {
        val docObj = docData.jsonObject

        // Determine if this is Sparovky or Hranolky based on prefix
        val (targetCollection, newDocId) = when {
            docId.startsWith("S-") -> "Sparovky" to docId.removePrefix("S-")
            docId.startsWith("H-") -> "Hranolky" to docId.removePrefix("H-")
            else -> "Hranolky" to docId // DUB- and others go to Hranolky as-is
        }

        if (targetCollection == "Sparovky") {
            stats.sparovkyDocs++
        } else {
            stats.hranolkyDocs++
        }

        // Extract main document fields (excluding _subcollections)
        val mainFields = docObj.filterKeys { it != "_subcollections" }
        val mainData = convertJsonToFirestoreMap(JsonObject(mainFields)).toMutableMap()

        // Add indexed fields for filtering
        val indexedFields = parseSlotIdToIndexedFields(newDocId)
        mainData.putAll(indexedFields)

        val indexedFieldsStr = indexedFields.entries.joinToString(", ") { (key, value) -> "$key = $value" }
        batchWriter.add(WriteOperation(
            collection = targetCollection,
            documentPath = newDocId,
            data = mainData,
            description = "Would write $targetCollection/$newDocId (indexed: $indexedFieldsStr)"
        ))

        // Process subcollections
        val subcollections = docObj["_subcollections"]?.jsonObject
        if (subcollections != null) {
            // Process SlotActions
            subcollections["SlotActions"]?.jsonObject?.let { slotActions ->
                for ((actionId, actionData) in slotActions) {
                    val actionFields = convertJsonToFirestoreMap(actionData.jsonObject)
                    batchWriter.add(WriteOperation(
                        collection = targetCollection,
                        documentPath = "$newDocId/SlotActions/$actionId",
                        data = actionFields,
                        description = "Would write $targetCollection/$newDocId/SlotActions/$actionId"
                    ))
                    stats.slotActions++
                }
            }

            // Process SlotWeeklyReport with ID transformation (2025_27 -> 25_27)
            // Skip reports with duplicate quantities from the previous week
            subcollections["SlotWeeklyReport"]?.jsonObject?.let { slotWeeklyReports ->
                // Sort by week number to process in chronological order
                val sortedReports = slotWeeklyReports.entries.sortedBy { (reportId, _) ->
                    // Extract week number from ID like "2025_27" or "25_27"
                    reportId.substringAfter("_").toIntOrNull() ?: 0
                }

                var previousQuantity: Long? = null

                for ((reportId, reportData) in sortedReports) {
                    val newReportId = if (reportId.startsWith("20")) {
                        reportId.substring(2) // Remove "20" prefix
                    } else {
                        reportId
                    }

                    // Get current quantity
                    val currentQuantity = reportData.jsonObject["quantity"]?.jsonPrimitive?.longOrNull

                    // Skip if quantity is the same as previous week
                    if (currentQuantity != null && currentQuantity == previousQuantity) {
                        if (dryRun) {
                            println("    [DRY RUN] Skipping $targetCollection/$newDocId/SlotWeeklyReport/$newReportId (quantity unchanged: $currentQuantity)")
                        }
                        continue
                    }

                    val reportFields = convertJsonToFirestoreMap(reportData.jsonObject)
                    batchWriter.add(WriteOperation(
                        collection = targetCollection,
                        documentPath = "$newDocId/SlotWeeklyReport/$newReportId",
                        data = reportFields,
                        description = "Would write $targetCollection/$newDocId/SlotWeeklyReport/$newReportId (quantity: $currentQuantity)"
                    ))
                    stats.slotWeeklyReports++

                    // Update previous quantity for next iteration
                    previousQuantity = currentQuantity
                }
            }
        }
    }

    // Flush remaining operations
    batchWriter.flush()

    return stats
}


private fun processWeeklyBeamReports(firestore: Firestore?, weeklyBeamReports: JsonObject, dryRun: Boolean): UploadStats {
    val stats = UploadStats()
    val batchWriter = BatchWriter(firestore, dryRun)

    println("\n[firestore-upload] Processing WeeklyBeamReports (${weeklyBeamReports.size} documents)...")

    for ((reportId, reportData) in weeklyBeamReports) {
        // Transform 2025_27 -> 25_27
        val newReportId = if (reportId.startsWith("20")) {
            reportId.substring(2)
        } else {
            reportId
        }

        val reportFields = convertJsonToFirestoreMap(reportData.jsonObject)

        // New path: WeeklyReports/Hranolky/WeeklyData/{reportId}
        batchWriter.add(WriteOperation(
            collection = "WeeklyReports",
            documentPath = "Hranolky/WeeklyData/$newReportId",
            data = reportFields,
            description = "Would write WeeklyReports/Hranolky/WeeklyData/$newReportId"
        ))
        stats.weeklyHranolkyReports++
    }

    batchWriter.flush()
    return stats
}


private fun processWeeklyJointerReports(firestore: Firestore?, weeklyJointerReports: JsonObject, dryRun: Boolean): UploadStats {
    val stats = UploadStats()
    val batchWriter = BatchWriter(firestore, dryRun)

    println("\n[firestore-upload] Processing WeeklyJointerReports (${weeklyJointerReports.size} documents)...")

    for ((reportId, reportData) in weeklyJointerReports) {
        // Transform 2025_27 -> 25_27
        val newReportId = if (reportId.startsWith("20")) {
            reportId.substring(2)
        } else {
            reportId
        }

        val reportFields = convertJsonToFirestoreMap(reportData.jsonObject)

        // New path: WeeklyReports/Sparovky/WeeklyData/{reportId}
        batchWriter.add(WriteOperation(
            collection = "WeeklyReports",
            documentPath = "Sparovky/WeeklyData/$newReportId",
            data = reportFields,
            description = "Would write WeeklyReports/Sparovky/WeeklyData/$newReportId"
        ))
        stats.weeklySparovkyReports++
    }

    batchWriter.flush()
    return stats
}

private fun processAppConfig(firestore: Firestore?, appConfig: JsonObject, dryRun: Boolean): UploadStats {
    val stats = UploadStats()
    val batchWriter = BatchWriter(firestore, dryRun)

    println("\n[firestore-upload] Processing AppConfig (${appConfig.size} documents)...")

    for ((docId, docData) in appConfig) {
        val docFields = convertJsonToFirestoreMap(docData.jsonObject)

        batchWriter.add(WriteOperation(
            collection = "AppConfig",
            documentPath = docId,
            data = docFields,
            description = "Would write AppConfig/$docId"
        ))
        stats.appConfigDocs++
    }

    batchWriter.flush()
    return stats
}

private fun processDevices(firestore: Firestore?, devices: JsonObject, dryRun: Boolean): UploadStats {
    val stats = UploadStats()
    val batchWriter = BatchWriter(firestore, dryRun)

    println("\n[firestore-upload] Processing Devices (${devices.size} documents)...")

    for ((docId, docData) in devices) {
        val docFields = convertJsonToFirestoreMap(docData.jsonObject)

        batchWriter.add(WriteOperation(
            collection = "Devices",
            documentPath = docId,
            data = docFields,
            description = "Would write Devices/$docId"
        ))
        stats.devices++
    }

    batchWriter.flush()
    return stats
}

/**
 * Convert JsonObject to Map<String, Any> for Firestore document
 */
private fun convertJsonToFirestoreMap(jsonObject: JsonObject): Map<String, Any> {
    return jsonObject.entries.mapNotNull { (key, value) ->
        val converted = convertJsonToFirestoreData(value)
        if (converted != null) key to converted else null
    }.toMap()
}

/**
 * Convert JsonElement to Firestore-compatible data types
 */
private fun convertJsonToFirestoreData(element: JsonElement): Any? {
    return when (element) {
        is JsonNull -> null
        is JsonPrimitive -> {
            element.booleanOrNull ?: element.longOrNull ?: element.doubleOrNull ?: element.content
        }
        is JsonObject -> {
            // Check for special types
            val typeField = element["__type"]?.jsonPrimitive?.content
            when (typeField) {
                "timestamp" -> {
                    val seconds = element["seconds"]?.jsonPrimitive?.long ?: 0L
                    val nanos = element["nanos"]?.jsonPrimitive?.int ?: 0
                    Timestamp.ofTimeSecondsAndNanos(seconds, nanos)
                }
                "geopoint" -> {
                    val lat = element["lat"]?.jsonPrimitive?.double ?: 0.0
                    val lon = element["lon"]?.jsonPrimitive?.double ?: 0.0
                    com.google.cloud.firestore.GeoPoint(lat, lon)
                }
                "blob" -> {
                    val base64 = element["base64"]?.jsonPrimitive?.content ?: ""
                    val bytes = java.util.Base64.getDecoder().decode(base64)
                    com.google.cloud.firestore.Blob.fromBytes(bytes)
                }
                // Note: We don't handle docref as they need special handling with firestore instance
                else -> {
                    // Regular object - convert recursively and filter out nulls
                    element.entries.mapNotNull { (key, value) ->
                        val converted = convertJsonToFirestoreData(value)
                        if (converted != null) key to converted else null
                    }.toMap()
                }
            }
        }
        is kotlinx.serialization.json.JsonArray -> {
            element.mapNotNull { convertJsonToFirestoreData(it) }
        }
    }
}









