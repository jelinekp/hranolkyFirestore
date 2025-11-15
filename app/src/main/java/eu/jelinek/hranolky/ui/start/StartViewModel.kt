package eu.jelinek.hranolky.ui.start

import android.annotation.SuppressLint
import android.app.Application
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class StartViewModel(
    application: Application,
    private val firestoreDb: FirebaseFirestore,
    private val auth: FirebaseAuth
) : AndroidViewModel(application) {
    private val _startScreenState = MutableStateFlow(StartUiState())
    val startScreenState get() = _startScreenState.asStateFlow()

    private val _navigateToManageItem = MutableSharedFlow<String>()
    val navigateToManageItem = _navigateToManageItem.asSharedFlow()

    private companion object {
        const val PREF_INVENTORY_CHECK_ENABLED = "inventory_check_enabled"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(getApplication())
    }

    init {
        signInAnonymously()
    }

    private fun signInAnonymously() {
        viewModelScope.launch {
            _startScreenState.value = _startScreenState.value.copy(isSigningIn = true)
            try {
                if (auth.currentUser == null) {
                    auth.signInAnonymously().await()
                    Log.d("Auth", "signInAnonymously:success. UID: ${auth.currentUser?.uid}")
                } else {
                    Log.d("Auth", "User already signed in with UID: ${auth.currentUser?.uid}")
                }

                fetchDeviceNameAndInventoryPermit()
                val currentVersionCode = logDeviceId()
                checkForUpdate(currentVersionCode)

            } catch (e: Exception) {
                Log.w("Auth", "signInAnonymously:failure", e)
                _startScreenState.value = _startScreenState.value.copy(isSignInProblem = true)
            } finally {
                _startScreenState.value = _startScreenState.value.copy(isSigningIn = false)
            }
        }
    }

    fun onScannedCodeChange(text: String, isAutoScanEnabled: Boolean) {
        viewModelScope.launch {
            _startScreenState.value = _startScreenState.value.copy(
                scannedCode = text.uppercase(),
                isFormatError = false,
                isSignInError = false
            )
            if (isAutoScanEnabled && isValidScannedTextFormat(text)) {
                processAndNavigate(text)
            }
        }
    }

    fun onSubmit() {
        viewModelScope.launch {
            if (_startScreenState.value.isSigningIn) {
                _startScreenState.value = _startScreenState.value.copy(isSignInError = true)
                return@launch
            }
            _startScreenState.value = _startScreenState.value.copy(isSignInError = false)

            val code = _startScreenState.value.scannedCode
            if (isValidScannedTextFormat(code)) {
                processAndNavigate(code)
                _startScreenState.value = _startScreenState.value.copy(isFormatError = false)
            } else {
                _startScreenState.value = _startScreenState.value.copy(isFormatError = true)
            }
        }
    }

    private suspend fun processAndNavigate(code: String) {
        var manipulatedCode = code
        val textLength = manipulatedCode.length
        if (textLength == 16) {
            if (manipulatedCode[1] != '-' && manipulatedCode[textLength - 5] == '-' && manipulatedCode.substring(textLength - 4).all(Char::isDigit)) {
                if (manipulatedCode.first() != 'S') {
                    manipulatedCode = "H-$manipulatedCode"
                }
            }
        }
        _navigateToManageItem.emit(manipulatedCode)
    }

    @SuppressLint("HardwareIds")
    private fun getDeviceId(): String {
        return Settings.Secure.getString(
            getApplication<Application>().contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }

    private fun logDeviceId(): Int {
        val deviceId = getDeviceId()
        val context = getApplication<Application>().applicationContext

        val appVersion = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("AppVersion", "Could not get package info", e)
            "Unknown"
        }

        val currentVersionCode = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("AppVersion", "Could not get package info", e)
            -1
        }

        _startScreenState.value = _startScreenState.value.copy(
            shortenedDeviceId = deviceId.substring(0..2),
            appVersion = appVersion ?: "Neznámá verze",
            appVersionCode = currentVersionCode
        )

        val deviceData = hashMapOf(
            "lastSeen" to FieldValue.serverTimestamp(),
            "appVersion" to appVersion
        )

        firestoreDb.collection("devices").document(deviceId)
            .set(deviceData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(
                    "Firestore",
                    "Device document created/updated for ID: $deviceId"
                )
            }
            .addOnFailureListener { e -> Log.w("Firestore", "Error writing document", e) }
        return currentVersionCode
    }

    private suspend fun fetchDeviceNameAndInventoryPermit() {
        val deviceId = getDeviceId()
        Log.d("StartViewModel", "Fetching device name for ID: $deviceId")
        try {
            val documentSnapshot = firestoreDb.collection("devices").document(deviceId).get().await()

            if (documentSnapshot.exists()) {
                val deviceName = documentSnapshot.getString("deviceName")
                val isInventoryCheckPermitted = documentSnapshot.getBoolean("isInventoryCheckPermitted") ?: false

                if (isInventoryCheckPermitted) {
                    loadInventoryCheckSetting()
                } else {
                    toggleInventoryCheck(isEnabled = false)
                }

                if (deviceName != null) {
                    Log.d("StartViewModel", "Device name found: $deviceName")
                    _startScreenState.value = _startScreenState.value.copy(
                        deviceName = deviceName,
                        isInventoryCheckPermitted = isInventoryCheckPermitted
                    )
                } else {
                    Log.d("StartViewModel", "deviceName field is null or not found in document for $deviceId.")
                    _startScreenState.value = _startScreenState.value.copy(
                        deviceName = null,
                        isInventoryCheckPermitted = isInventoryCheckPermitted
                    )
                    toggleInventoryCheck(isEnabled = false)
                }
            } else {
                Log.d("StartViewModel", "Device document not found for ID: $deviceId. Cannot fetch device name.")
                _startScreenState.value = _startScreenState.value.copy(
                    deviceName = null,
                    isInventoryCheckPermitted = false,
                )
                toggleInventoryCheck(isEnabled = false)
            }
        } catch (e: Exception) {
            Log.e("StartViewModel", "Error fetching device name for ID: $deviceId", e)
            _startScreenState.value = _startScreenState.value.copy(deviceName = null)
        }
    }

    private suspend fun checkForUpdate(currentVersionCode: Int) {
        try {
            val document = firestoreDb.collection("app_config").document("latest").get().await()

            if (document.exists()) {
                val latestVersionCode = document.getLong("versionCode")?.toInt() ?: -1

                Log.d("AppUpdate", "Current version: $currentVersionCode, Latest version from Firestore: $latestVersionCode")

                if (currentVersionCode != -1 && latestVersionCode > currentVersionCode) {
                    Log.i("AppUpdate", "New app version found!")
                    document.getString("downloadUrl")?.let { url ->
                        downloadAndInstallUpdate(url)
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

    private fun downloadAndInstallUpdate(apkUrl: String) {
        val context = getApplication<Application>()
        val downloadManager = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager

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
                    val downloadManager = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
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

    fun toggleInventoryCheck(isEnabled: Boolean) {
        _startScreenState.value = _startScreenState.value.copy(isInventoryCheckEnabled = isEnabled)
        sharedPreferences.edit { putBoolean(PREF_INVENTORY_CHECK_ENABLED, isEnabled) }
        Log.d("StartViewModel", "Saved inventory check setting: $isEnabled")
    }

    private fun loadInventoryCheckSetting() {
        val isEnabled = sharedPreferences.getBoolean(PREF_INVENTORY_CHECK_ENABLED, false)
        _startScreenState.value = _startScreenState.value.copy(isInventoryCheckEnabled = isEnabled)
        Log.d("StartViewModel", "Loaded inventory check setting: $isEnabled")
    }

    fun isValidScannedTextFormat(text: String): Boolean {
        val textLength = text.length

        if (textLength == 16) {
            if (text[1] != '-'
                && text[textLength - 5] == '-'
                && text.substring(textLength - 4).all(Char::isDigit)
            ) {
                return true
            }
        }

        if (textLength == 18) {
            if (text[0] == 'H'
                && text[1] == '-'
                && text[textLength - 5] == '-'
                && text.substring(textLength - 4).all(Char::isDigit)
            ) {
                return true
            }
        }

        if (textLength == 22) {
            val jointerRegex = Regex("^S-[A-Z]{3}-[A-Z|]{3}-[0-9]{2}-[0-9]{4}-[0-9]{4}$")
            return jointerRegex.matches(text)
        }

        return false
    }

}

data class StartUiState(
    val scannedCode: String = "",
    val shortenedDeviceId: String = "",
    val deviceName: String? = null,
    val appVersion: String = "",
    val appVersionCode: Int = -1,
    val isInventoryCheckPermitted: Boolean = false,
    val isInventoryCheckEnabled: Boolean = false,
    val isSigningIn: Boolean = false,
    val isSignInProblem: Boolean = false,
    val isFormatError: Boolean = false,
    val isSignInError: Boolean = false
)
