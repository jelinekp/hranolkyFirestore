package eu.jelinek.hranolky.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import eu.jelinek.hranolky.model.AppUpdateInfo
import kotlinx.coroutines.tasks.await

class AppConfigRepositoryImpl(private val firestoreDb: FirebaseFirestore) : AppConfigRepository {

    private val TAG = "AppConfigRepository"

    override suspend fun getLatestAppVersion(): AppUpdateInfo? {
        return try {
            Log.d(TAG, "Fetching latest app version from Firestore...")
            val document = firestoreDb.collection("AppConfig")
                .document("latest")
                .get()
                .await()

            if (document.exists()) {
                val versionCode = document.getLong("versionCode")?.toInt() ?: -1
                val version = document.getString("version") ?: "Unknown"
                val downloadUrl = document.getString("downloadUrl")
                val releaseNotes = document.getString("releaseNotes") ?: ""

                Log.d(TAG, "Retrieved version info: versionCode=$versionCode, version=$version")

                AppUpdateInfo(
                    versionCode = versionCode,
                    version = version,
                    downloadUrl = downloadUrl,
                    releaseNotes = releaseNotes
                )
            } else {
                Log.w(TAG, "AppConfig 'latest' document not found")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching latest app version", e)
            throw e
        }
    }
}

