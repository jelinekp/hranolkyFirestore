package eu.jelinek.hranolky.domain

import android.app.DownloadManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileInputStream

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
                        // Get the actual file path from the download manager
                        val query = DownloadManager.Query().setFilterById(downloadId)
                        val cursor = downloadManager.query(query)
                        if (cursor.moveToFirst()) {
                            val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            val localUri = cursor.getString(columnIndex)
                            cursor.close()

                            if (localUri != null) {
                                val file = File(Uri.parse(localUri).path!!)
                                installUpdateSilently(context, file)
                            } else {
                                Log.e("AppUpdate", "Could not get local file path")
                            }
                        } else {
                            cursor.close()
                            Log.e("AppUpdate", "Download query returned no results")
                        }
                    } else {
                        Log.e("AppUpdate", "Download failed, could not retrieve file URI.")
                    }
                    // Unregister the receiver to prevent memory leaks
                    context.unregisterReceiver(this)
                }
            }
        }
    }

    private fun installUpdateSilently(context: Context, apkFile: File) {
        try {
            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)

            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)

            FileInputStream(apkFile).use { inputStream ->
                session.openWrite("package", 0, -1).use { outputStream ->
                    inputStream.copyTo(outputStream)
                    session.fsync(outputStream)
                }
            }

            val intent = Intent(context, InstallReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            session.commit(pendingIntent.intentSender)
            session.close()

            Log.i("AppUpdate", "Silent installation initiated for session: $sessionId")
        } catch (e: Exception) {
            Log.e("AppUpdate", "Silent installation failed", e)
            // Fallback to manual installation
            installUpdateManually(context, apkFile)
        }
    }

    private fun installUpdateManually(context: Context, apkFile: File) {
        val fileUri = Uri.fromFile(apkFile)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(installIntent)
            Log.i("AppUpdate", "Manual installation intent started")
        } catch (e: SecurityException) {
            Log.e("AppUpdate", "Install failed. App may not have permission to install unknown apps.", e)
        } catch (e: Exception) {
            Log.e("AppUpdate", "Failed to start install intent for unknown reason.", e)
        }
    }

    class InstallReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    val confirmIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                    confirmIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(confirmIntent)
                        Log.i("AppUpdate", "User action required for installation")
                    } catch (e: Exception) {
                        Log.e("AppUpdate", "Failed to start user confirmation", e)
                    }
                }
                PackageInstaller.STATUS_SUCCESS -> {
                    Log.i("AppUpdate", "Installation succeeded!")
                }
                PackageInstaller.STATUS_FAILURE,
                PackageInstaller.STATUS_FAILURE_ABORTED,
                PackageInstaller.STATUS_FAILURE_BLOCKED,
                PackageInstaller.STATUS_FAILURE_CONFLICT,
                PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
                PackageInstaller.STATUS_FAILURE_INVALID,
                PackageInstaller.STATUS_FAILURE_STORAGE -> {
                    val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    Log.e("AppUpdate", "Installation failed with status $status: $message")
                }
            }
        }
    }
}

