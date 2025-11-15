package eu.jelinek.hranolky.domain

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UpdateManager(private val firestoreDb: FirebaseFirestore) {
    suspend fun checkForUpdate(currentVersionCode: Int, context: Context) {
        try {
            val document = firestoreDb.collection("app_config").document("latest").get().await()

            if (document.exists()) {
                val latestVersionCode = document.getLong("versionCode")?.toInt() ?: -1

                Log.d("AppUpdate", "Current version: $currentVersionCode, Latest version from Firestore: $latestVersionCode")

                if (currentVersionCode != -1 && latestVersionCode > currentVersionCode) {
                    Log.i("AppUpdate", "New app version found!")
                    document.getString("downloadUrl")?.let { url ->
                        downloadAndInstallUpdate(url, context)
                    } ?: Log.w("AppUpdate", "Download URL is null for the latest version.")
                } else {
                    Log.d("AppUpdate", "App is up to date or local version code is invalid.")
                }
            } else {
                Log.w("AppUpdate", "Could not find 'latest' app_config document.")
            }
        } catch (e: Exception) {
            Log.e("AppUpdate", "Error checking for update", e)
        }
    }

    private fun downloadAndInstallUpdate(apkUrl: String, context: Context) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Hranolky Update")
            .setDescription("Downloading new version...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "hranolky_update.apk")
            .setMimeType("application/vnd.android.package-archive")

        val downloadId = downloadManager.enqueue(request)

        val receiver = createDownloadCompleteReceiver(downloadId)

        // Using ContextCompat.registerReceiver for compatibility and security
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED // Flag for security on Android 14+
        )
    }

    private fun createDownloadCompleteReceiver(downloadId: Long): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val fileUri = downloadManager.getUriForDownloadedFile(downloadId)

                    if (fileUri != null) {
                        installUpdate(context, fileUri)
                    } else {
                        Log.e("AppUpdate", "Download failed, could not retrieve file URI.")
                    }
                    // Unregister the receiver to prevent memory leaks
                    context.unregisterReceiver(this)
                }
            }
        }
    }

    private fun installUpdate(context: Context, fileUri: Uri) {
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(installIntent)
        } catch (e: SecurityException) {
            Log.e("AppUpdate", "Install failed. App may not have permission to install unknown apps.", e)
            // Here you could notify the user that they need to enable "Install from unknown sources"
        } catch (e: Exception) {
            Log.e("AppUpdate", "Failed to start install intent for unknown reason.", e)
        }
    }
}