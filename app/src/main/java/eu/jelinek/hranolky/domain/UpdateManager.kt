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
import eu.jelinek.hranolky.data.AppConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream

data class UpdateState(
    val isUpdateAvailable: Boolean = false,
    val isDownloading: Boolean = false,
    val isInstalling: Boolean = false,
    val downloadProgress: Int = 0,
    val latestVersion: String = "",
    val latestVersionCode: Int = 0,
    val releaseNotes: String = "",
    val error: String? = null,
    val justUpdated: Boolean = false // True if app just completed an update
)

class UpdateManager(private val appConfigRepository: AppConfigRepository) {
    private val _updateState = MutableStateFlow(UpdateState())
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    companion object {
        private const val PREFS_NAME = "update_manager_prefs"
        private const val PREF_INSTALLING_VERSION = "installing_version_code"
        private const val PREF_INSTALLING_VERSION_NAME = "installing_version_name"
    }

    /**
     * Check if the app was just updated (called on app start).
     * Returns true if we detect that an update was in progress and now complete.
     */
    fun checkIfJustUpdated(context: Context, currentVersionCode: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val installingVersionCode = prefs.getInt(PREF_INSTALLING_VERSION, -1)
        val installingVersionName = prefs.getString(PREF_INSTALLING_VERSION_NAME, "") ?: ""

        Log.d("AppUpdate", "checkIfJustUpdated: current=$currentVersionCode, wasInstalling=$installingVersionCode")

        if (installingVersionCode > 0 && installingVersionCode == currentVersionCode) {
            // Clear the persisted state
            prefs.edit()
                .remove(PREF_INSTALLING_VERSION)
                .remove(PREF_INSTALLING_VERSION_NAME)
                .apply()

            Log.i("AppUpdate", "App was just updated to version $installingVersionName (code: $currentVersionCode)")
            _updateState.value = _updateState.value.copy(justUpdated = true)
            return true
        }

        // If we had an installing version but it doesn't match current, the update may have failed
        if (installingVersionCode > 0) {
            Log.w("AppUpdate", "Update may have failed - was installing $installingVersionCode but current is $currentVersionCode")
            prefs.edit()
                .remove(PREF_INSTALLING_VERSION)
                .remove(PREF_INSTALLING_VERSION_NAME)
                .apply()
        }

        return false
    }

    /**
     * Persist that we're about to install a specific version.
     * This allows us to detect successful updates after app restart.
     */
    private fun persistInstallingState(context: Context, versionCode: Int, versionName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(PREF_INSTALLING_VERSION, versionCode)
            .putString(PREF_INSTALLING_VERSION_NAME, versionName)
            .apply()
        Log.d("AppUpdate", "Persisted installing state: version=$versionName, code=$versionCode")
    }

    /**
     * Clear the "just updated" flag after it has been displayed to the user.
     */
    fun clearJustUpdatedFlag() {
        _updateState.value = _updateState.value.copy(justUpdated = false)
    }

    suspend fun checkForUpdate(currentVersionCode: Int, context: Context) {
        Log.d("AppUpdate", "checkForUpdate() started - currentVersionCode: $currentVersionCode")

        if (currentVersionCode == -1) {
            Log.w("AppUpdate", "Current version code is invalid (-1), skipping update check")
            return
        }

        try {
            val updateInfo = appConfigRepository.getLatestAppVersion()

            if (updateInfo != null) {
                val latestVersionCode = updateInfo.versionCode
                val downloadUrl = updateInfo.downloadUrl
                val latestVersion = updateInfo.version
                val releaseNotes = updateInfo.releaseNotes

                Log.d("AppUpdate", "Current version: $currentVersionCode, Latest version: $latestVersionCode")
                Log.d("AppUpdate", "Download URL: $downloadUrl")
                Log.d("AppUpdate", "Release notes: $releaseNotes")

                if (latestVersionCode > currentVersionCode) {
                    Log.i("AppUpdate", "New app version found! Upgrading from v$currentVersionCode to v$latestVersionCode")

                    // Update state to show update is available
                    _updateState.value = UpdateState(
                        isUpdateAvailable = true,
                        latestVersion = latestVersion,
                        latestVersionCode = latestVersionCode,
                        releaseNotes = releaseNotes
                    )

                    downloadUrl?.let { url ->
                        // Provide deterministic filename for the download (avoid generic names)
                        val fileName = "hranolky_update_v${latestVersionCode}.apk"
                        downloadAndInstallUpdate(url, context, fileName)
                    } ?: run {
                        Log.w("AppUpdate", "Download URL is null for the latest version.")
                        _updateState.value = _updateState.value.copy(
                            error = "Download URL is missing"
                        )
                    }
                } else if (latestVersionCode == currentVersionCode) {
                    Log.d("AppUpdate", "App is up to date (both at version $currentVersionCode). Cleaning any stale downloaded APKs.")
                    deleteDownloadedApks(context)
                } else {
                    Log.d("AppUpdate", "Current version ($currentVersionCode) is newer than Firestore version ($latestVersionCode). Cleaning any stale downloaded APKs.")
                    deleteDownloadedApks(context)
                }
            } else {
                Log.w("AppUpdate", "Could not find latest app version info")
            }
        } catch (e: Exception) {
            Log.e("AppUpdate", "Error checking for update: ${e.message}", e)
        }
    }

    private fun downloadAndInstallUpdate(apkUrl: String, context: Context, fileName: String) {
        Log.d("AppUpdate", "downloadAndInstallUpdate() called with URL: $apkUrl")

        // Set downloading state
        _updateState.value = _updateState.value.copy(
            isDownloading = true,
            downloadProgress = 0
        )

        // Use application context to ensure receiver persists
        val appContext = context.applicationContext
        Log.d("AppUpdate", "Using application context: ${appContext.javaClass.simpleName}")

        val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        Log.d("AppUpdate", "DownloadManager service obtained")

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Stahuji novou verzi Hranolek")
            .setDescription("Smaží se nové Hranolky!")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalFilesDir(appContext, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setMimeType("application/vnd.android.package-archive")

        val downloadId = downloadManager.enqueue(request)
        Log.i("AppUpdate", "Download enqueued with ID: $downloadId")
        Log.d("AppUpdate", "Download destination: ${appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)}")

        // Immediately check download status
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
            val status = cursor.getInt(statusIndex)
            val reason = cursor.getInt(reasonIndex)

            val statusText = when (status) {
                DownloadManager.STATUS_PENDING -> "PENDING"
                DownloadManager.STATUS_RUNNING -> "RUNNING"
                DownloadManager.STATUS_PAUSED -> "PAUSED"
                DownloadManager.STATUS_SUCCESSFUL -> "SUCCESSFUL"
                DownloadManager.STATUS_FAILED -> "FAILED"
                else -> "UNKNOWN($status)"
            }

            Log.d("AppUpdate", "Initial download status: $statusText, reason code: $reason")

            if (status == DownloadManager.STATUS_FAILED) {
                val reasonText = when (reason) {
                    DownloadManager.ERROR_CANNOT_RESUME -> "ERROR_CANNOT_RESUME"
                    DownloadManager.ERROR_DEVICE_NOT_FOUND -> "ERROR_DEVICE_NOT_FOUND"
                    DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "ERROR_FILE_ALREADY_EXISTS"
                    DownloadManager.ERROR_FILE_ERROR -> "ERROR_FILE_ERROR"
                    DownloadManager.ERROR_HTTP_DATA_ERROR -> "ERROR_HTTP_DATA_ERROR"
                    DownloadManager.ERROR_INSUFFICIENT_SPACE -> "ERROR_INSUFFICIENT_SPACE"
                    DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "ERROR_TOO_MANY_REDIRECTS"
                    DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "ERROR_UNHANDLED_HTTP_CODE"
                    DownloadManager.ERROR_UNKNOWN -> "ERROR_UNKNOWN"
                    else -> "UNKNOWN_ERROR($reason)"
                }
                Log.e("AppUpdate", "Download failed immediately: $reasonText")
            }
        }
        cursor.close()

        val receiver = createDownloadCompleteReceiver(downloadId)

        // Using ContextCompat.registerReceiver with APPLICATION CONTEXT for compatibility and security
        ContextCompat.registerReceiver(
            appContext,  // Use application context, not activity context
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED // Flag for security on Android 14+
        )
        Log.d("AppUpdate", "BroadcastReceiver registered on APPLICATION context for download completion")

        // Start monitoring download progress
        monitorDownloadProgress(downloadId, downloadManager, appContext)
    }

    private fun monitorDownloadProgress(downloadId: Long, downloadManager: DownloadManager, context: Context) {
        Log.d("AppUpdate", "Starting download progress monitoring for ID: $downloadId")

        CoroutineScope(Dispatchers.IO).launch {
            var isComplete = false
            var checkCount = 0

            while (!isComplete && checkCount < 60) { // Monitor for up to 2 minutes (60 * 2 seconds)
                delay(2000) // Check every 2 seconds
                checkCount++

                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)

                if (cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                    val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                    val status = cursor.getInt(statusIndex)
                    val reason = cursor.getInt(reasonIndex)
                    val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
                    val bytesTotal = cursor.getLong(bytesTotalIndex)

                    when (status) {
                        DownloadManager.STATUS_PENDING -> {
                            Log.d("AppUpdate", "Download #$checkCount: PENDING")
                        }
                        DownloadManager.STATUS_RUNNING -> {
                            val progress = if (bytesTotal > 0) {
                                ((bytesDownloaded * 100) / bytesTotal).toInt()
                            } else {
                                -1
                            }
                            // Update download progress
                            _updateState.value = _updateState.value.copy(
                                isDownloading = true,
                                downloadProgress = if (progress >= 0) progress else 0
                            )
                            Log.d("AppUpdate", "Download #$checkCount: RUNNING - $bytesDownloaded/$bytesTotal bytes ($progress%)")
                        }
                        DownloadManager.STATUS_PAUSED -> {
                            val pauseReasonText = when (reason) {
                                DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "QUEUED_FOR_WIFI"
                                DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "WAITING_FOR_NETWORK"
                                DownloadManager.PAUSED_WAITING_TO_RETRY -> "WAITING_TO_RETRY"
                                DownloadManager.PAUSED_UNKNOWN -> "UNKNOWN"
                                else -> "CODE_$reason"
                            }
                            Log.w("AppUpdate", "Download #$checkCount: PAUSED - Reason: $pauseReasonText")
                        }
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            Log.i("AppUpdate", "Download #$checkCount: SUCCESSFUL - $bytesDownloaded bytes downloaded")
                            Log.d("AppUpdate", "Download completed successfully, triggering installation manually as fallback...")

                            // Update state to installing
                            _updateState.value = _updateState.value.copy(
                                isDownloading = false,
                                downloadProgress = 100,
                                isInstalling = true
                            )

                            // Manually trigger installation as fallback in case broadcast isn't received
                            val fileUri = downloadManager.getUriForDownloadedFile(downloadId)
                            if (fileUri != null) {
                                Log.d("AppUpdate", "Fallback: Got file URI: $fileUri")
                                val localUriQuery = DownloadManager.Query().setFilterById(downloadId)
                                val localCursor = downloadManager.query(localUriQuery)

                                if (localCursor.moveToFirst()) {
                                    val columnIndex = localCursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                                    val localUri = localCursor.getString(columnIndex)
                                    localCursor.close()

                                    if (localUri != null) {
                                        val file = File(Uri.parse(localUri).path!!)
                                        Log.i("AppUpdate", "Fallback: APK file located at: ${file.absolutePath}")
                                        Log.d("AppUpdate", "Fallback: File exists: ${file.exists()}, Size: ${file.length()} bytes")
                                        installUpdateSilently(context, file)
                                    } else {
                                        Log.e("AppUpdate", "Fallback: Could not get local file path - localUri is null")
                                        _updateState.value = _updateState.value.copy(
                                            isInstalling = false,
                                            error = "Could not get local file path"
                                        )
                                    }
                                } else {
                                    localCursor.close()
                                    Log.e("AppUpdate", "Fallback: Download query returned no results")
                                    _updateState.value = _updateState.value.copy(
                                        isInstalling = false,
                                        error = "Download query failed"
                                    )
                                }
                            } else {
                                Log.e("AppUpdate", "Fallback: Could not retrieve file URI")
                                _updateState.value = _updateState.value.copy(
                                    isInstalling = false,
                                    error = "Could not retrieve file"
                                )
                            }

                            isComplete = true
                        }
                        DownloadManager.STATUS_FAILED -> {
                            val errorText = when (reason) {
                                DownloadManager.ERROR_CANNOT_RESUME -> "CANNOT_RESUME"
                                DownloadManager.ERROR_DEVICE_NOT_FOUND -> "DEVICE_NOT_FOUND"
                                DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "FILE_ALREADY_EXISTS"
                                DownloadManager.ERROR_FILE_ERROR -> "FILE_ERROR"
                                DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP_DATA_ERROR"
                                DownloadManager.ERROR_INSUFFICIENT_SPACE -> "INSUFFICIENT_SPACE"
                                DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "TOO_MANY_REDIRECTS"
                                DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "UNHANDLED_HTTP_CODE"
                                DownloadManager.ERROR_UNKNOWN -> "UNKNOWN"
                                else -> "CODE_$reason"
                            }
                            Log.e("AppUpdate", "Download #$checkCount: FAILED - Error: $errorText")
                            _updateState.value = _updateState.value.copy(
                                isDownloading = false,
                                error = "Download failed: $errorText"
                            )
                            isComplete = true
                        }
                    }
                } else {
                    Log.w("AppUpdate", "Download #$checkCount: Query returned no results - download may have been removed")
                    isComplete = true
                }
                cursor.close()
            }

            if (checkCount >= 60) {
                Log.e("AppUpdate", "Download monitoring timed out after 2 minutes")
            }

            Log.d("AppUpdate", "Download progress monitoring completed after $checkCount checks")
        }
    }

    private fun createDownloadCompleteReceiver(downloadId: Long): BroadcastReceiver {
        Log.d("AppUpdate", "Creating BroadcastReceiver for download ID: $downloadId")
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d("AppUpdate", "BroadcastReceiver.onReceive() called")
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                Log.d("AppUpdate", "Received download ID: $id, expected ID: $downloadId")

                if (id == downloadId) {
                    Log.i("AppUpdate", "Download completed for ID: $downloadId")

                    // Update state to installing
                    _updateState.value = _updateState.value.copy(
                        isDownloading = false,
                        downloadProgress = 100,
                        isInstalling = true
                    )

                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val fileUri = downloadManager.getUriForDownloadedFile(downloadId)
                    Log.d("AppUpdate", "File URI from DownloadManager: $fileUri")

                    if (fileUri != null) {
                        Log.d("AppUpdate", "Querying DownloadManager for file details...")
                        // Get the actual file path from the download manager
                        val query = DownloadManager.Query().setFilterById(downloadId)
                        val cursor = downloadManager.query(query)

                        if (cursor.moveToFirst()) {
                            Log.d("AppUpdate", "Download query successful, extracting file path...")
                            val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            val localUri = cursor.getString(columnIndex)
                            Log.d("AppUpdate", "Local URI: $localUri, column index: $columnIndex")
                            cursor.close()

                            if (localUri != null) {
                                val file = File(Uri.parse(localUri).path!!)
                                Log.i("AppUpdate", "APK file located at: ${file.absolutePath}")
                                Log.d("AppUpdate", "File exists: ${file.exists()}, Size: ${file.length()} bytes")
                                installUpdateSilently(context, file)
                            } else {
                                Log.e("AppUpdate", "Could not get local file path - localUri is null")
                                _updateState.value = _updateState.value.copy(
                                    isInstalling = false,
                                    error = "Could not get local file path"
                                )
                            }
                        } else {
                            cursor.close()
                            Log.e("AppUpdate", "Download query returned no results - cursor is empty")
                            _updateState.value = _updateState.value.copy(
                                isInstalling = false,
                                error = "Download query failed"
                            )
                        }
                    } else {
                        Log.e("AppUpdate", "Download failed, could not retrieve file URI - fileUri is null")
                        _updateState.value = _updateState.value.copy(
                            isInstalling = false,
                            error = "Could not retrieve file"
                        )
                    }
                    // Unregister the receiver to prevent memory leaks
                    Log.d("AppUpdate", "Unregistering BroadcastReceiver")
                    context.unregisterReceiver(this)
                } else {
                    Log.d("AppUpdate", "Ignoring broadcast for different download ID: $id")
                }
            }
        }
    }

    private fun installUpdateSilently(context: Context, apkFile: File) {
        Log.i("AppUpdate", "installUpdateSilently() called for file: ${apkFile.absolutePath}")

        // Persist installation state before starting - this allows us to detect
        // successful updates after the app restarts
        val currentState = _updateState.value
        if (currentState.latestVersionCode > 0) {
            persistInstallingState(context, currentState.latestVersionCode, currentState.latestVersion)
        }

        try {
            Log.d("AppUpdate", "Getting PackageInstaller...")
            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)

            Log.d("AppUpdate", "Creating installation session...")
            val sessionId = packageInstaller.createSession(params)
            Log.i("AppUpdate", "Installation session created with ID: $sessionId")

            Log.d("AppUpdate", "Opening installation session...")
            val session = packageInstaller.openSession(sessionId)

            Log.d("AppUpdate", "Writing APK to session (file size: ${apkFile.length()} bytes)...")
            FileInputStream(apkFile).use { inputStream ->
                session.openWrite("package", 0, -1).use { outputStream ->
                    val bytesCopied = inputStream.copyTo(outputStream)
                    Log.d("AppUpdate", "Copied $bytesCopied bytes to installation session")
                    session.fsync(outputStream)
                    Log.d("AppUpdate", "Session fsync completed")
                }
            }

            Log.d("AppUpdate", "Creating PendingIntent for installation callback...")
            val intent = Intent(context, InstallReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            Log.d("AppUpdate", "Committing installation session...")
            session.commit(pendingIntent.intentSender)
            session.close()

            Log.i("AppUpdate", "Silent installation initiated for session: $sessionId")
        } catch (e: Exception) {
            Log.e("AppUpdate", "Silent installation failed: ${e.message}", e)
            Log.w("AppUpdate", "Falling back to manual installation...")
            // Fallback to manual installation
            installUpdateManually(context, apkFile)
        }
    }

    private fun installUpdateManually(context: Context, apkFile: File) {
        Log.i("AppUpdate", "installUpdateManually() called for file: ${apkFile.absolutePath}")
        Log.d("AppUpdate", "File exists: ${apkFile.exists()}, readable: ${apkFile.canRead()}")

        val fileUri = Uri.fromFile(apkFile)
        Log.d("AppUpdate", "Created file URI: $fileUri")

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        Log.d("AppUpdate", "Intent created with flags: NEW_TASK, GRANT_READ_URI_PERMISSION")
        try {
            context.startActivity(installIntent)
            Log.i("AppUpdate", "Manual installation intent started successfully")
        } catch (e: SecurityException) {
            Log.e("AppUpdate", "Install failed. App may not have permission to install unknown apps.", e)
        } catch (e: Exception) {
            Log.e("AppUpdate", "Failed to start install intent: ${e.message}", e)
        }
    }

    private fun isOwnApk(context: Context, file: File): Boolean {
        if (!file.name.startsWith("update") || !file.name.endsWith(".apk")) return false
        val pm = context.packageManager
        val pkgInfo = pm.getPackageArchiveInfo(file.path, 0)
        val matches = pkgInfo?.packageName == context.packageName
        Log.d("AppUpdate", "APK ${file.name} package check: pkgInfoPackage=${pkgInfo?.packageName} matchesApp=$matches")
        return matches
    }

    private fun deleteDownloadedApks(context: Context) {
        try {
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir == null) {
                Log.w("AppUpdate", "Downloads directory is null; cannot clean APKs")
                return
            }
            val apkFiles = downloadsDir.listFiles { file ->
                file.isFile && file.name.endsWith(".apk") && file.name.startsWith("update")
            } ?: emptyArray()
            if (apkFiles.isEmpty()) {
                Log.d("AppUpdate", "No matching update APK files to delete in ${downloadsDir.absolutePath}")
                return
            }
            var deletedCount = 0
            apkFiles.forEach { file ->
                if (isOwnApk(context, file)) {
                    val deleted = file.delete()
                    Log.d("AppUpdate", "Delete ${file.name}: ${if (deleted) "SUCCESS" else "FAILED"}")
                    if (deleted) deletedCount++
                } else {
                    Log.d("AppUpdate", "Skipping ${file.name} - package does not match app")
                }
            }
            Log.i("AppUpdate", "Deleted $deletedCount/${apkFiles.size} update APK file(s)")
        } catch (e: Exception) {
            Log.e("AppUpdate", "Failed to delete downloaded APKs: ${e.message}", e)
        }
    }

    class InstallReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("AppUpdate", "InstallReceiver.onReceive() called")
            val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
            Log.d("AppUpdate", "Installation status code: $status")

            when (status) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    Log.i("AppUpdate", "Installation requires user action")
                    val confirmIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                    confirmIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(confirmIntent)
                        Log.i("AppUpdate", "User confirmation intent started successfully")
                    } catch (e: Exception) {
                        Log.e("AppUpdate", "Failed to start user confirmation: ${e.message}", e)
                    }
                }
                PackageInstaller.STATUS_SUCCESS -> {
                    Log.i("AppUpdate", "Installation succeeded! App update complete.")
                    // Don't automatically relaunch - let the user open the app manually
                    // Automatic relaunch can cause crashes during the installation process
                }
                PackageInstaller.STATUS_FAILURE -> {
                    val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    Log.e("AppUpdate", "Installation FAILED with status $status: $message")
                }
                PackageInstaller.STATUS_FAILURE_ABORTED -> {
                    val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    Log.e("AppUpdate", "Installation ABORTED by user with status $status: $message")
                }
                PackageInstaller.STATUS_FAILURE_BLOCKED -> {
                    val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    Log.e("AppUpdate", "Installation BLOCKED with status $status: $message")
                }
                PackageInstaller.STATUS_FAILURE_CONFLICT -> {
                    val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    Log.e("AppUpdate", "Installation CONFLICT with status $status: $message")
                }
                PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> {
                    val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    Log.e("AppUpdate", "Installation INCOMPATIBLE with status $status: $message")
                }
                PackageInstaller.STATUS_FAILURE_INVALID -> {
                    val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    Log.e("AppUpdate", "Installation INVALID with status $status: $message")
                }
                PackageInstaller.STATUS_FAILURE_STORAGE -> {
                    val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    Log.e("AppUpdate", "Installation failed due to STORAGE issue with status $status: $message")
                }
                else -> {
                    val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    Log.w("AppUpdate", "Unknown installation status $status: $message")
                }
            }
        }
    }
}
