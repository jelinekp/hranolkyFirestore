import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import com.google.cloud.firestore.Query
import java.io.FileInputStream

/**
 * Script to find slots in Hranolky and Sparovky collections where the latest slot action
 * is inconsistent with the slot's current state (quantity or lastModified).
 */
fun main(args: Array<String>) {
    val credentialsPath = args.firstOrNull() ?: run {
        println("Usage: SlotConsistencyChecker <path-to-service-account-json>")
        return
    }

    val firestore = buildFirestoreClient(credentialsPath)
    println("Connected to project: ${firestore.options.projectId}")

    val collections = listOf("Hranolky", "Sparovky")

    // Attempt to list collections to verify overall access if direct access fails
    try {
        for (collectionName in collections) {
            println("\n--- Checking collection: $collectionName ---")
            val slots = try {
                firestore.collection(collectionName).get().get()
            } catch (e: Exception) {
                println("Error: Could not access collection '$collectionName'.")
                println("Message: ${e.message}")
                if (e.message?.contains("PERMISSION_DENIED") == true) {
                    printPermissionGuidance(firestore)
                }
                continue
            }
            
            var inconsistentCount = 0
            for (slotDoc in slots.documents) {
                val slotId = slotDoc.id
                val slotQuantity = slotDoc.getLong("quantity") ?: 0L
                val slotLastModified = slotDoc.getTimestamp("lastModified")

                // Get the latest SlotAction
                val latestActionQuery = slotDoc.reference.collection("SlotActions")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .get()

                if (latestActionQuery.isEmpty) continue

                val latestAction = latestActionQuery.documents[0]
                val actionNewQuantity = latestAction.getLong("newQuantity") ?: 0L
                val actionTimestamp = latestAction.getTimestamp("timestamp")

                val quantityMismatch = slotQuantity != actionNewQuantity
                val timestampMismatch = if (slotLastModified != null && actionTimestamp != null) {
                    slotLastModified.seconds != actionTimestamp.seconds || slotLastModified.nanos != actionTimestamp.nanos
                } else {
                    slotLastModified != actionTimestamp
                }

                if (quantityMismatch || timestampMismatch) {
                    inconsistentCount++
                    println("Inconsistent Slot: $collectionName/$slotId")
                    if (quantityMismatch) {
                        println("  Quantity mismatch: Slot=$slotQuantity, LatestAction=$actionNewQuantity")
                    }
                    if (timestampMismatch) {
                        println("  Timestamp mismatch: Slot=$slotLastModified, LatestAction=$actionTimestamp")
                    }
                }
            }
            println("Finished $collectionName. Found $inconsistentCount inconsistent slots.")
        }
    } catch (e: Exception) {
        println("\n[FATAL ERROR] ${e.message}")
        e.printStackTrace()
    }
}

private fun printPermissionGuidance(firestore: Firestore) {
    val creds = firestore.options.credentials as? ServiceAccountCredentials
    val email = creds?.clientEmail ?: "unknown"
    println("\n[GUIDANCE]")
    println("The Service Account '$email' does not have permission to read Firestore.")
    println("To fix this:")
    println("1. Go to https://console.cloud.google.com/iam-admin/iam?project=${firestore.options.projectId}")
    println("2. Find the principal: $email")
    println("3. Ensure it has one of these roles:")
    println("   - Firebase Firestore Admin (recommended for full access)")
    println("   - Cloud Datastore User (minimum for read/write)")
    println("   - Cloud Datastore Viewer (minimum for read-only)")
    println("--------------------------------------------------\n")
}

private fun buildFirestoreClient(credentialsPath: String): Firestore {
    val fis = FileInputStream(credentialsPath)
    val creds = GoogleCredentials.fromStream(fis)
    val projectId = (creds as? ServiceAccountCredentials)?.projectId

    val builder = FirestoreOptions.getDefaultInstance().toBuilder()
        .setCredentials(creds)
    
    if (projectId != null) {
        builder.setProjectId(projectId)
    }

    return builder.build().service
}
