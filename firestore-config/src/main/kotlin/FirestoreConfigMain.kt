import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import java.io.File
import java.io.FileInputStream

/**
 * Firestore AppConfig update utility.
 * Updates the /AppConfig/latest document with new version info.
 * Archives the old "latest" document by renaming it to its version name.
 *
 * CLI Args:
 *   --version <name> (required) version name e.g. "3.0.4"
 *   --version-code <code> (required) version code e.g. 18
 *   --release-notes <notes> (required) release notes text
 *   --credentials <path.json> (required) service account key
 *   --dry-run (optional) simulate without writing to Firestore
 */
fun main(args: Array<String>) {
    val config = parseArgs(args.toList())

    println("[firestore-config] Starting AppConfig update${if (config.dryRun) " (DRY RUN)" else ""}")
    println("[firestore-config] Version: ${config.version}")
    println("[firestore-config] Version Code: ${config.versionCode}")
    println("[firestore-config] Release Notes: ${config.releaseNotes}")
    println("[firestore-config] Credentials: ${config.credentialsPath}")

    val firestore = if (!config.dryRun) buildFirestoreClient(config.credentialsPath) else null

    // Step 1: Read the current "latest" document
    println("\n[firestore-config] Reading current 'latest' document...")
    val currentLatest = if (!config.dryRun) {
        val docRef = firestore!!.collection("AppConfig").document("latest")
        val snapshot = docRef.get().get()
        if (snapshot.exists()) {
            snapshot.data
        } else {
            println("[firestore-config] WARNING: No existing 'latest' document found")
            null
        }
    } else {
        println("  [DRY RUN] Would read AppConfig/latest")
        null
    }

    // Step 2: Archive the old "latest" document with its version name
    if (currentLatest != null) {
        val oldVersion = currentLatest["version"]?.toString() ?: "unknown"
        println("[firestore-config] Archiving old version '$oldVersion' to AppConfig/$oldVersion...")

        if (!config.dryRun) {
            val archiveRef = firestore!!.collection("AppConfig").document(oldVersion)
            archiveRef.set(currentLatest).get()
            println("[firestore-config] Archived successfully")
        } else {
            println("  [DRY RUN] Would archive to AppConfig/$oldVersion")
        }
    }

    // Step 3: Update the "latest" document with new version info
    println("[firestore-config] Updating 'latest' document with new version...")
    val newLatestData = mapOf(
        "downloadUrl" to "https://jelinekp.cz/app/update.apk",
        "releaseNotes" to config.releaseNotes,
        "version" to config.version,
        "versionCode" to config.versionCode
    )

    if (!config.dryRun) {
        val latestRef = firestore!!.collection("AppConfig").document("latest")
        latestRef.set(newLatestData).get()
        println("[firestore-config] Updated 'latest' document successfully")
    } else {
        println("  [DRY RUN] Would update AppConfig/latest with:")
        newLatestData.forEach { (key, value) ->
            println("    $key: $value")
        }
    }

    println("\n[firestore-config] Done${if (config.dryRun) " (DRY RUN - NO DATA WRITTEN)" else ""}!")
}

data class ConfigUpdateConfig(
    val version: String,
    val versionCode: Long,
    val releaseNotes: String,
    val credentialsPath: File,
    val dryRun: Boolean
)

fun parseArgs(args: List<String>): ConfigUpdateConfig {
    var version: String? = null
    var versionCode: Long? = null
    var releaseNotes: String? = null
    var credentialsPath: File? = null
    var dryRun = false

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--version" -> {
                version = args.getOrNull(i + 1)
                    ?: error("--version requires a value")
                i += 2
            }
            "--version-code" -> {
                versionCode = args.getOrNull(i + 1)?.toLongOrNull()
                    ?: error("--version-code requires a numeric value")
                i += 2
            }
            "--release-notes" -> {
                releaseNotes = args.getOrNull(i + 1)
                    ?: error("--release-notes requires a value")
                i += 2
            }
            "--credentials" -> {
                credentialsPath = File(args.getOrNull(i + 1)
                    ?: error("--credentials requires a path"))
                i += 2
            }
            "--dry-run" -> {
                dryRun = true
                i++
            }
            else -> {
                error("Unknown argument: ${args[i]}")
            }
        }
    }

    require(version != null) { "--version is required" }
    require(versionCode != null) { "--version-code is required" }
    require(releaseNotes != null) { "--release-notes is required" }
    require(credentialsPath != null) { "--credentials is required" }
    require(credentialsPath.exists()) { "Credentials file not found: $credentialsPath" }

    return ConfigUpdateConfig(
        version = version,
        versionCode = versionCode,
        releaseNotes = releaseNotes,
        credentialsPath = credentialsPath,
        dryRun = dryRun
    )
}

fun buildFirestoreClient(credentialsPath: File): Firestore {
    val credentials = GoogleCredentials.fromStream(FileInputStream(credentialsPath))
    val options = FirestoreOptions.newBuilder()
        .setCredentials(credentials)
        .build()
    return options.service
}

