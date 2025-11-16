# Silent Update Installation Implementation

## Overview
This implementation adds silent APK installation functionality to the Hranolky Firestore app using Android's PackageInstaller API. The system automatically downloads and attempts to install updates silently on ZEBRA TC200J terminals.

## How It Works

### 1. Download Phase
- When a new version is detected via Firestore, the `UpdateManager` initiates a download using Android's `DownloadManager`
- The APK is downloaded to the app's external files directory
- A notification shows download progress to the user

### 2. Installation Phase
The system attempts silent installation in the following order:

#### Primary: Silent Installation (PackageInstaller API)
- Uses Android's `PackageInstaller` API to install the APK programmatically
- Creates an installation session and streams the APK file into it
- Commits the session with a `PendingIntent` callback to track installation status
- The `InstallReceiver` BroadcastReceiver handles the installation result

#### Fallback: Manual Installation
- If silent installation fails (e.g., due to insufficient permissions), the system falls back to manual installation
- Opens the standard Android install dialog for the user to confirm

### 3. Installation Status Tracking
The `InstallReceiver` handles various installation states:
- **STATUS_PENDING_USER_ACTION**: User confirmation is required (launches the confirmation dialog)
- **STATUS_SUCCESS**: Installation completed successfully
- **STATUS_FAILURE_***: Various failure states are logged with detailed error messages

## Implementation Details

### Modified Files

#### 1. UpdateManager.kt
**New imports added:**
```kotlin
import android.app.PendingIntent
import android.content.pm.PackageInstaller
import java.io.File
import java.io.FileInputStream
```

**New methods:**
- `installUpdateSilently(context: Context, apkFile: File)`: Main silent installation logic
- `installUpdateManually(context: Context, apkFile: File)`: Fallback manual installation
- `InstallReceiver` (inner class): Handles installation callbacks

**Modified method:**
- `createDownloadCompleteReceiver()`: Now extracts the actual file path and calls silent installation

#### 2. AndroidManifest.xml
**New receiver registration:**
```xml
<receiver
    android:name="eu.jelinek.hranolky.domain.UpdateManager$InstallReceiver"
    android:exported="false" />
```

**Required permission** (already present):
```xml
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

## ZEBRA Device Considerations

### Enterprise Device Management
ZEBRA TC200J terminals are typically managed enterprise devices. For fully silent installation without any user interaction, you may need to:

1. **Configure Device Owner Mode**: 
   - ZEBRA devices support MDM (Mobile Device Management) solutions
   - If the app is installed as a Device Owner or through an MDM solution, it can have enhanced installation privileges

2. **StageNow Configuration**:
   - Use ZEBRA's StageNow tool to configure installation permissions
   - Can whitelist your app package for silent installations

3. **Enterprise Mobility Management (EMM)**:
   - Deploy the app through ZEBRA's EMM solutions (like MX or EMDK profiles)
   - This can grant additional installation permissions

### Current Implementation Benefits
Even without enterprise management:
- Minimizes user interaction by attempting silent installation first
- Automatically falls back to manual installation if needed
- Provides detailed logging for troubleshooting
- Handles all installation states gracefully

## Testing

### Testing Silent Installation
1. Build and install a lower version of the app
2. Upload a higher version APK to your hosting service
3. Update the Firestore `app_config/latest` document with the new version code and download URL
4. Launch the app and observe the logs:
   - Look for "Silent installation initiated" in logs
   - Check installation status messages from `InstallReceiver`

### Log Tags
Monitor these log tags for debugging:
- `AppUpdate`: All update-related operations
- Filter by these messages:
  - "New app version found!"
  - "Silent installation initiated"
  - "Installation succeeded!"
  - "Installation failed with status"
  - "Manual installation intent started"

## Troubleshooting

### Silent Installation Fails
If silent installation consistently fails:
1. Check if `REQUEST_INSTALL_PACKAGES` permission is granted
2. Verify the app has "Install unknown apps" permission enabled in device settings
3. Consider using ZEBRA's MDM solution for enhanced privileges
4. Review logs for specific failure status codes

### Download Fails
If downloads fail:
1. Verify network connectivity
2. Check that the download URL in Firestore is accessible
3. Ensure the app has proper storage permissions
4. Review DownloadManager status in device settings

### User Still Sees Installation Dialog
This is expected behavior on non-managed devices or devices without enhanced privileges. The PackageInstaller API will request user confirmation for security reasons unless the app has special privileges (Device Owner, System App, or MDM-managed).

## Security Considerations

1. **Download Source**: Always use HTTPS URLs for APK downloads
2. **Signature Verification**: Android automatically verifies that the new APK is signed with the same key
3. **Permission Model**: The app can only update itself, not install other apps
4. **User Control**: On non-managed devices, users retain final control over installation

## Future Enhancements

Potential improvements for ZEBRA deployment:
1. Integrate with ZEBRA EMDK for enhanced device management
2. Add APK signature verification before installation
3. Implement retry logic for failed downloads
4. Add notification for successful updates
5. Support for staged rollouts based on device groups
6. Schedule updates during off-peak hours

## API Compatibility

- **Minimum SDK**: API 27 (Android 8.1) - As per your device specification
- **PackageInstaller API**: Available since API 21 (Android 5.0)
- **Fully Compatible**: Implementation uses APIs available on ZEBRA TC200J (API 27)

