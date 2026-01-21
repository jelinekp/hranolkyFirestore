= Current System Analysis

This section analyzes the original codebase structure and identifies specific violations of Normalized Systems (NS) Theory.
The analysis serves as the foundation for the refactoring work documented in the next section.

== System Architecture

The hranolky-firestore application is a native Kotlin Android application with the following architecture:

#figure(
  table(
    columns: (auto, auto),
    inset: 8pt,
    align: left,
    [*Layer*], [*Technology/Pattern*],
    [Presentation], [Jetpack Compose, ViewModels],
    [Domain], [Use Cases, Managers],
    [Data], [Firebase Firestore, Repositories],
    [DI], [Koin],
  ),
  caption: [Technology stack by layer]
)

The application manages warehouse inventory for two product types: *Hranolky* (beams) and *Spárovky* (jointers).
Users scan QR codes on inventory slots using Zebra TC200J terminals to track additions, removals, and inventory checks.

== Separation of Concerns (SoC) Violations <soc-violations>

According to NS Theory, each software element should address exactly one concern.
When a component mixes multiple responsibilities, changes to one area require modifications to unrelated code,
creating combinatorial effects that hinder evolvability.

=== SoC-1: ManageItemViewModel (522 lines) — God Class

The `ManageItemViewModel` handles at least 6 distinct concerns:

#figure(
```kotlin
class ManageItemViewModel(...) : AndroidViewModel(application) {
    // Concern 1: Device ID retrieval (Android system access)
    private fun getDeviceId(): String {
        return Settings.Secure.getString(
            getApplication<Application>().contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }

    // Concern 2: Quantity parsing with complex business rules
    private fun parseQuantityStringToLong(quantityStr: String): Result<Long> {
        // 40+ lines handling sums like "X+Y", item code detection
    }

    // Concern 3: Inventory check date calculation
    private fun checkInventoryDone() {
        val calendar75DaysAgo = Calendar.getInstance()
        calendar75DaysAgo.add(Calendar.DAY_OF_YEAR, -75)
        // Business rule: inventory check valid for 75 days
    }

    // Concern 4: SharedPreferences access
    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(getApplication())
    }

    // Concern 5: Navigation logic
    private val _navigateToAnotherItem = MutableSharedFlow<String>()

    // Concern 6: Undo action coordination
    fun undoLastAction(undoableAction: UndoableAction) { ... }
}
```,
  caption: [ManageItemViewModel mixes 6+ concerns]
)

*Impact:* Any change to quantity parsing, device identification, or preference storage requires modifying this 522-line class.

=== SoC-2: WarehouseSlot Data Class with Business Logic

The `WarehouseSlot` data class (186 lines) contains business logic that should be extracted:

#figure(
```kotlin
data class WarehouseSlot(
    val fullProductId: String,
    val quantity: Long,
    // ... data fields
) {
    // Business logic embedded in data class:
    fun parsePropertiesFromProductId(): WarehouseSlot {
        // 50+ lines of parsing logic with hardcoded mappings
        val thickness = when (rawThickness) {
            20.0f -> 20.0f
            27.0f -> 27.4f  // Hardcoded dimension adjustment
            42.0f -> 42.4f
            else -> rawThickness
        }
    }

    fun getFullQualityName(): String {
        return when (this.quality) {
            "DUB-A|A" -> "DUB A/A"
            "DUB-ABP" -> "DUB A/B-P"
            // 15+ hardcoded quality code mappings
        }
    }

    fun getVolume(): Double? {
        // Volume calculation business logic
    }
}
```,
  caption: [Data class with embedded business logic]
)

*Impact:* Adding a new quality code or adjusting dimension mappings requires modifying the core data model.

=== SoC-3: UpdateManager (628 lines) — Monolithic Update Handler

The `UpdateManager` handles downloading, progress monitoring, installation, broadcast receiving, and file cleanup in a single class:

#figure(
```kotlin
class UpdateManager(private val appConfigRepository: AppConfigRepository) {
    // Concern 1: Version checking
    suspend fun checkForUpdate(currentVersionCode: Int, context: Context)

    // Concern 2: Download management
    private fun downloadAndInstallUpdate(apkUrl: String, context: Context, fileName: String)

    // Concern 3: Progress monitoring (spawns coroutine)
    private fun monitorDownloadProgress(downloadId: Long, ...)

    // Concern 4: Broadcast receiver creation
    private fun createDownloadCompleteReceiver(downloadId: Long): BroadcastReceiver

    // Concern 5: Silent installation via PackageInstaller
    private fun installUpdateSilently(context: Context, apkFile: File)

    // Concern 6: Manual installation fallback
    private fun installUpdateManually(context: Context, apkFile: File)

    // Concern 7: APK cleanup
    private fun deleteDownloadedApks(context: Context)

    // Nested class for installation callbacks
    class InstallReceiver : BroadcastReceiver() { ... }
}
```,
  caption: [UpdateManager handles 7+ concerns in 628 lines]
)

*Impact:* Changes to update handling, download management, or installation logic require touching this monolithic class.

== Data Version Transparency (DVT) Violations <dvt-violations>

DVT requires that data structures and configuration values can evolve without causing ripple effects.
Configuration should be centralized, not scattered across components.

=== DVT-1: Hardcoded Firestore Collection Names

Collection names are embedded in multiple locations:

#figure(
```kotlin
// In SlotType.kt
enum class SlotType {
    Beam, Jointer;

    fun toFirestoreCollectionName(): String = when (this) {
        Beam -> "Hranolky"      // Hardcoded
        Jointer -> "Sparovky"   // Hardcoded
    }
}

// In SlotRepositoryImpl.kt (used ~15 times)
firestoreDb.collection(collectionName)
    .document(normalizedId)
    .collection("SlotActions")  // Hardcoded subcollection name
```,
  caption: [Collection names scattered across codebase]
)

*Impact:* Renaming a Firestore collection requires changes in multiple files.

=== DVT-2: Hardcoded Web Client ID

The Google Sign-In client ID is embedded directly in `AuthManager`:

#figure(
```kotlin
class AuthManager(private val auth: FirebaseAuth) {
    companion object {
        const val WEB_CLIENT_ID =
            "657740368257-25rd5mbgs9scckap948c8u4s4rtdlpku.apps.googleusercontent.com"
    }
}
```,
  caption: [Hardcoded authentication configuration]
)

*Impact:* Changing the web client ID requires code changes and redeployment.

=== DVT-3: Hardcoded Quality Code Mappings

Quality code to display name mappings are embedded in `WarehouseSlot.getFullQualityName()`:

#figure(
```kotlin
fun getFullQualityName(): String {
    return when (this.quality) {
        "DUB-A|A" -> "DUB A/A"
        "DUB-A|B" -> "DUB A/B"
        "DUB-B|B" -> "DUB B/B"
        // ... 15 more hardcoded mappings
        "KŠT-KŠT" -> "KAŠTAN"
        else -> this.quality
    }
}
```,
  caption: [Quality mappings hardcoded in data class]
)

*Impact:* Adding a new wood quality type requires modifying core business logic.

=== DVT-4: Hardcoded Dimension Adjustments

Raw measurement to actual dimension mappings:

#figure(
```kotlin
val thickness = when (rawThickness) {
    20.0f -> 20.0f
    27.0f -> 27.4f   // Hardcoded calibration
    42.0f -> 42.4f   // Hardcoded calibration
    else -> rawThickness
}
```,
  caption: [Dimension calibration values hardcoded]
)

*Impact:* Changing a dimension mapping requires code changes in multiple places.

=== DVT-5: Hardcoded Inventory Check Duration

The 75-day inventory check validity period is embedded in business logic:

#figure(
```kotlin
private fun checkInventoryDone() {
    val calendar75DaysAgo = Calendar.getInstance()
    calendar75DaysAgo.add(Calendar.DAY_OF_YEAR, -75)  // Magic number
}
```,
  caption: [Business rule duration hardcoded]
)

*Impact:* Adjusting the inventory check period requires code changes and redeployment.

== Action Version Transparency (AVT) Violations <avt-violations>

AVT ensures that actions can evolve independently of their consumers.
Business logic should not be embedded in UI components or tightly coupled to specific implementations.

=== AVT-1: Business Logic in ViewModels

The `ManageItemViewModel.parseQuantityStringToLong()` method (40+ lines) contains complex business rules including item code detection and navigation:

#figure(
```kotlin
private fun parseQuantityStringToLong(quantityStr: String): Result<Long> {
    // Handle sums like "X+Y+Z"
    if (parts.size > 1) {
        var sum = 0L
        for (part in parts) { ... }
        return Result.success(sum)
    }

    // Detect if input is actually an item code (navigation trigger)
    val itemCode = quantityStr.trim().uppercase()
    val validatedItemCode = inputValidator.manipulateAndValidateItemCode(itemCode)
    if (validatedItemCode != null) {
        viewModelScope.launch {
            _navigateToAnotherItem.emit(validatedItemCode)
        }
        return Result.failure(NumberFormatException("Přesměrování..."))
    }
}
```,
  caption: [Quantity parsing mixed with navigation logic in ViewModel]
)

*Impact:* Changing quantity parsing rules requires modifying the ViewModel, risking UI regressions.

=== AVT-2: Slot ID Normalization Duplicated

The logic to extract slot type and normalize IDs appears in multiple places:

#figure(
```kotlin
// In SlotRepositoryImpl.kt
private fun getNormalizedIdAndSlotType(slotId: String): Pair<String, SlotType> {
    return if (slotId.length > 16 && (slotId.startsWith('S') || slotId.startsWith('H'))
        && slotId[1] == '-') {
        val normalizedId = slotId.substring(2)
        val slotType = if (slotId.startsWith('S')) SlotType.Jointer else SlotType.Beam
        Pair(normalizedId, slotType)
    } else {
        Pair(slotId, SlotType.Beam)
    }
}

// Similar logic in WarehouseSlot.parsePropertiesFromProductId()
val (type, processedProductId) = if (initialProductId.startsWith("H")
    || initialProductId.startsWith("S")) {
    initialProductId.take(1) to initialProductId.substring(2)
} else {
    "" to initialProductId
}
```,
  caption: [Duplicated slot ID parsing logic]
)

*Impact:* Slot ID normalization changes require updates in multiple locations.

== Separation of States (SoS) Violations <sos-violations>

SoS requires that state changes are isolated to prevent unintended side effects.
Combined state objects mixing unrelated concerns create coupling between independent features.

=== SoS-1: StartUiState Combines Unrelated Concerns

#figure(
```kotlin
data class StartUiState(
    // Scanning state
    val scannedCode: String = "",
    val isFormatError: Boolean = false,

    // Device state
    val shortenedDeviceId: String = "",
    val deviceName: String? = null,

    // App version state
    val appVersion: String = "",
    val appVersionCode: Int = -1,

    // Inventory permission state
    val isInventoryCheckPermitted: Boolean = false,
    val isInventoryCheckEnabled: Boolean = false,

    // Sign-in state
    val isSigningIn: Boolean = false,
    val isSignInProblem: Boolean = false,
    val isSignInError: Boolean = false
)
```,
  caption: [StartUiState combines 4 unrelated state groups]
)

*Impact:* A change to scanning state triggers recomposition for device info, version display, and sign-in UI.

=== SoS-2: ManageItemScreenState Mixed Concerns

#figure(
```kotlin
data class ManageItemScreenState(
    val slot: WarehouseSlot? = null,              // Slot data
    val screenTitle: String = "",                  // Display formatting
    val resultStatus: ResultStatus = LOADING,      // Network status
    val error: String? = null,                     // Error handling
    val isOnline: Boolean = true,                  // Connectivity
    val isInventoryCheckEnabled: Boolean = false,  // Feature flag
    val showConfirmSettingPopup: Boolean = false,  // Dialog state
    val inventoryCheckPopupMessage: ...,           // Dialog content
    val isInventoryCheckDone: Boolean = false,     // Business rule state
)
```,
  caption: [ManageItemScreenState mixes 5+ concern categories]
)

*Impact:* Modifying slot data structure or network result handling affects unrelated UI concerns.

=== SoS-3: UpdateState Overloaded

#figure(
```kotlin
data class UpdateState(
    val isUpdateAvailable: Boolean = false,
    val isDownloading: Boolean = false,
    val isInstalling: Boolean = false,
    val downloadProgress: Int = 0,
    val latestVersion: String = "",
    val latestVersionCode: Int = 0,
    val releaseNotes: String = "",
    val error: String? = null,
    val justUpdated: Boolean = false
)
```,
  caption: [UpdateState combines availability, progress, and post-update states]
)

*Impact:* A change in update logic or state handling requires touching this overloaded state class.

== Summary of Violations

#figure(
  table(
    columns: (auto, auto, auto, auto),
    inset: 8pt,
    align: left,
    [*ID*], [*Theorem*], [*Location*], [*Severity*],
    [SoC-1], [SoC], [ManageItemViewModel (522 lines)], [High],
    [SoC-2], [SoC], [WarehouseSlot business logic], [Medium],
    [SoC-3], [SoC], [UpdateManager (628 lines)], [High],
    [DVT-1], [DVT], [Hardcoded collection names], [Medium],
    [DVT-2], [DVT], [Hardcoded Web Client ID], [Low],
    [DVT-3], [DVT], [Quality code mappings], [Medium],
    [DVT-4], [DVT], [Dimension adjustments], [Low],
    [DVT-5], [DVT], [75-day inventory period], [Low],
    [AVT-1], [AVT], [Business logic in ViewModels], [High],
    [AVT-2], [AVT], [Duplicated ID normalization], [Medium],
    [SoS-1], [SoS], [StartUiState (4 concern groups)], [Medium],
    [SoS-2], [SoS], [ManageItemScreenState], [Medium],
    [SoS-3], [SoS], [UpdateState overloaded], [Low],
  ),
  caption: [Summary of identified NS violations]
)

The next section documents how these violations were systematically addressed through targeted refactoring.
