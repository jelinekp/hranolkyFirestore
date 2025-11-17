@file:Suppress("UNCHECKED_CAST")
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.Timestamp
import com.google.cloud.firestore.Blob
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import com.google.cloud.firestore.GeoPoint
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import java.io.FileInputStream
import java.time.Instant

/**
 * Firestore full dump utility.
 * CLI Args:
 *   --output <path> (required) output JSON file
 *   --projectId <projectId> (optional) override project id
 *   --credentials <path.json> (optional) service account key; else GOOGLE_APPLICATION_CREDENTIALS env used
 *   --pretty (optional) pretty print
 *   --flat (optional) output as flat map of path -> document fields instead of nested tree
 */
fun main(args: Array<String>) {
    val config = parseArgs(args.toList())
    val firestore = buildClient(config)

    println("[firestore-dump] Starting dump for project=${config.projectId ?: firestore.options.projectId}")

    val rootCollections = firestore.listCollections().toList()
    if (rootCollections.isEmpty()) {
        println("[firestore-dump] No root collections found; writing empty JSON.")
        writeJson(JsonObject(emptyMap()), config)
        return
    }

    val flatResult = mutableMapOf<String, JsonElement>()
    val nestedBuilder = mutableMapOf<String, JsonElement>()

    for (colIndex in rootCollections.indices) {
        val col = rootCollections[colIndex]
        val colName = col.id
        println("[firestore-dump] Starting collection '$colName' (${colIndex + 1}/${rootCollections.size})")
        val colJson = dumpCollectionRecursive(firestore, colName, col, config)
        if (config.flat) {
            flatResult.putAll(colJson.flat)
        } else {
            nestedBuilder[colName] = colJson.nested
        }
        println("[firestore-dump] Finished collection '$colName' documents=${colJson.documentCount} (${colIndex + 1}/${rootCollections.size})")
    }

    val finalJson = if (config.flat) JsonObject(flatResult) else JsonObject(nestedBuilder)
    writeJson(finalJson, config)
    println("[firestore-dump] Dump complete -> ${config.output}")
}

data class DumpConfig(
    val output: File,
    val projectId: String?,
    val credentialsPath: File?,
    val pretty: Boolean,
    val flat: Boolean,
    val progressEvery: Int, // log progress every N documents per collection (<=0 disables per-doc progress)
)

private fun parseArgs(args: List<String>): DumpConfig {
    if (args.isEmpty()) usageAndExit()
    var output: File? = null
    var projectId: String? = null
    var credentials: File? = null
    var pretty = false
    var flat = false
    var progressEvery = 100
    var i = 0
    while (i < args.size) {
        when (val a = args[i]) {
            "--output" -> output = File(args.getOrNull(++i) ?: err("Missing value for --output"))
            "--projectId" -> projectId = args.getOrNull(++i) ?: err("Missing value for --projectId")
            "--credentials" -> credentials = File(args.getOrNull(++i) ?: err("Missing value for --credentials"))
            "--pretty" -> pretty = true
            "--flat" -> flat = true
            "--progressEvery" -> progressEvery = (args.getOrNull(++i) ?: err("Missing value for --progressEvery")).toIntOrNull()?.takeIf { it >= 0 }
                ?: err("--progressEvery must be an integer >= 0")
            "-h", "--help" -> usageAndExit()
            else -> err("Unknown arg: $a")
        }
        i++
    }
    if (output == null) err("--output is required")
    return DumpConfig(output, projectId, credentials, pretty, flat, progressEvery)
}

private fun usageAndExit(): Nothing {
    println(
        """
        Firestore Dump Utility

        Required:
          --output <file>

        Optional:
          --projectId <id>
          --credentials <serviceAccount.json>
          --pretty (pretty print)
          --flat (flat path->docFields)
          --progressEvery <N> (log per-collection doc progress every N docs; 0 disables, default 100)

        Examples:
          ./gradlew :firestore-dump:run --args="--output dump.json --pretty"
          ./gradlew :firestore-dump:run --args="--output dump.json --progressEvery 50"
          java -jar firestore-dump-all.jar --output dump.json --flat --progressEvery 10
        """.trimIndent()
    )
    kotlin.system.exitProcess(0)
}

private fun err(msg: String): Nothing {
    System.err.println("Error: $msg")
    kotlin.system.exitProcess(1)
}

private fun buildClient(cfg: DumpConfig): Firestore {
    val builder = FirestoreOptions.getDefaultInstance().toBuilder()
    if (cfg.projectId != null) builder.setProjectId(cfg.projectId)
    val creds = when {
        cfg.credentialsPath != null -> FileInputStream(cfg.credentialsPath).use { GoogleCredentials.fromStream(it) }
        System.getenv("GOOGLE_APPLICATION_CREDENTIALS") != null -> GoogleCredentials.getApplicationDefault()
        else -> err("No credentials specified. Provide --credentials or set GOOGLE_APPLICATION_CREDENTIALS env var.")
    }
    builder.setCredentials(creds)
    return builder.build().service
}

private data class CollectionDump(val flat: Map<String, JsonElement>, val nested: JsonElement, val documentCount: Int)

private fun dumpCollectionRecursive(
    firestore: Firestore,
    collectionName: String,
    collection: com.google.cloud.firestore.CollectionReference,
    cfg: DumpConfig
): CollectionDump {
    val flat = mutableMapOf<String, JsonElement>()

    val docs = collection.listDocuments().toList()
    val total = docs.size
    if (total == 0) {
        println("[firestore-dump] Collection '$collectionName' empty")
    } else {
        println("[firestore-dump] Collection '$collectionName' has $total documents")
    }
    val nestedDocs = mutableMapOf<String, JsonElement>()

    for ((index, docRef) in docs.withIndex()) {
        val snap = docRef.get().get() // blocking
        if (!snap.exists()) continue
        val docPath = snap.reference.path
        val dataJson = convertMap(snap.data)
        val subCols = snap.reference.listCollections().toList()
        // Handle subcollections
        if (subCols.isNotEmpty()) {
            val subMap = mutableMapOf<String, JsonElement>()
            for (sub in subCols) {
                val subDump = dumpCollectionRecursive(firestore, sub.id, sub, cfg)
                // merge flat results
                flat.putAll(subDump.flat)
                subMap[sub.id] = subDump.nested
            }
            val withChildren = JsonObject(dataJson + ("_subcollections" to JsonObject(subMap)))
            nestedDocs[snap.id] = withChildren
            flat[docPath] = withChildren
        } else {
            nestedDocs[snap.id] = JsonObject(dataJson)
            flat[docPath] = JsonObject(dataJson)
        }
        if (cfg.progressEvery > 0 && total > 0) {
            val shouldLog = ((index + 1) % cfg.progressEvery == 0) || (index == total - 1)
            if (shouldLog) {
                val percent = ((index + 1) * 100.0 / total).let { String.format("%.1f", it) }
                println("[firestore-dump] Progress '$collectionName': ${index + 1}/$total docs ($percent%)")
            }
        }
    }

    return CollectionDump(flat, JsonObject(nestedDocs), nestedDocs.size)
}

private fun convertValue(v: Any?): JsonElement = when (v) {
    null -> JsonNull
    is String -> JsonPrimitive(v)
    is Number -> JsonPrimitive(v)
    is Boolean -> JsonPrimitive(v)
    is Timestamp -> JsonObject(mapOf(
        "__type" to JsonPrimitive("timestamp"),
        "seconds" to JsonPrimitive(v.seconds),
        "nanos" to JsonPrimitive(v.nanos),
        "iso" to JsonPrimitive(Instant.ofEpochSecond(v.seconds, v.nanos.toLong()).toString())
    ))
    is GeoPoint -> JsonObject(mapOf(
        "__type" to JsonPrimitive("geopoint"),
        "lat" to JsonPrimitive(v.latitude),
        "lon" to JsonPrimitive(v.longitude)
    ))
    is Blob -> JsonObject(mapOf(
        "__type" to JsonPrimitive("blob"),
        "base64" to JsonPrimitive(v.toBytes().encodeBase64())
    ))
    is DocumentReference -> JsonObject(mapOf(
        "__type" to JsonPrimitive("docref"),
        "path" to JsonPrimitive(v.path)
    ))
    is Map<*, *> -> JsonObject(convertMap(v as Map<String, Any?>))
    is List<*> -> JsonArray(v.map { convertValue(it) })
    else -> JsonObject(mapOf(
        "__type" to JsonPrimitive("unknown"),
        "class" to JsonPrimitive(v::class.qualifiedName ?: "?"),
        "string" to JsonPrimitive(v.toString())
    ))
}

private fun convertMap(data: Map<String, Any?>?): Map<String, JsonElement> =
    data?.mapValues { convertValue(it.value) } ?: emptyMap()

private fun ByteArray.encodeBase64(): String = java.util.Base64.getEncoder().encodeToString(this)

private val json = Json { prettyPrint = true }

private fun writeJson(root: JsonElement, cfg: DumpConfig) {
    val text = if (cfg.pretty) json.encodeToString(JsonElement.serializer(), root) else Json.encodeToString(JsonElement.serializer(), root)
    cfg.output.writeText(text)
}
