= Refactored System Design

This section documents the design decisions for refactoring the system to comply with Normalized Systems Theory.
Each violation identified in the previous section is addressed with a specific refactoring strategy.

== Target Architecture Overview

The refactored architecture introduces several new modules to separate concerns:

#figure(
```
hranolky-firestore/
├── app/src/main/java/eu/jelinek/hranolky/
│   ├── config/                      # NEW: Centralized configuration
│   │   ├── FirestoreConfig.kt       # Collection names, paths
│   │   ├── AuthConfig.kt            # Client IDs, OAuth settings
│   │   └── BusinessRules.kt         # Thresholds, periods, limits
│   │
│   ├── domain/
│   │   ├── usecase/                 # NEW: Dedicated use cases
│   │   │   ├── ParseQuantityUseCase.kt
│   │   │   ├── CheckInventoryStatusUseCase.kt
│   │   │   ├── NormalizeSlotIdUseCase.kt
│   │   │   └── ... (one per business operation)
│   │   │
│   │   ├── mapping/                 # NEW: Data transformations
│   │   │   ├── QualityNameMapper.kt
│   │   │   ├── DimensionAdjuster.kt
│   │   │   └── SlotIdParser.kt
│   │   │
│   │   ├── update/                  # REFACTORED: Split UpdateManager
│   │   │   ├── VersionChecker.kt
│   │   │   ├── ApkDownloader.kt
│   │   │   ├── ApkInstaller.kt
│   │   │   └── UpdateStateHolder.kt
│   │   │
│   │   └── ... (existing managers, slimmed down)
│   │
│   ├── model/
│   │   └── WarehouseSlot.kt         # Pure data class (logic extracted)
│   │
│   └── ui/
│       ├── state/                   # NEW: Granular state objects
│       │   ├── ScanningState.kt
│       │   ├── DeviceState.kt
│       │   ├── UpdateProgressState.kt
│       │   └── ...
│       └── ... (ViewModels now thin orchestrators)
```,
  caption: [Target module structure after refactoring]
)

== DVT Refactorings — Configuration Extraction

=== DVT-R1: Centralized Firestore Configuration

Create a `FirestoreConfig` object to hold all collection and document names:

#figure(
```kotlin
// config/FirestoreConfig.kt
object FirestoreConfig {
    // Collection names
    const val COLLECTION_BEAMS = "Hranolky"
    const val COLLECTION_JOINTERS = "Sparovky"
    const val SUBCOLLECTION_ACTIONS = "SlotActions"

    // Document paths
    const val DOC_APP_CONFIG = "AppConfig/latest"
    const val DOC_DEVICES = "Devices"

    // Query limits
    const val QUERY_LIMIT_ACTIONS = 10
    const val QUERY_LIMIT_RECENT_SLOTS = 10
}

// Usage in SlotRepositoryImpl.kt
firestoreDb.collection(FirestoreConfig.COLLECTION_BEAMS)
    .document(normalizedId)
    .collection(FirestoreConfig.SUBCOLLECTION_ACTIONS)
```,
  caption: [Centralized Firestore configuration]
)

*Impact:* Changing a collection name now requires editing a single file.

=== DVT-R2: Authentication Configuration

Extract authentication settings to `AuthConfig`:

#figure(
```kotlin
// config/AuthConfig.kt
object AuthConfig {
    const val WEB_CLIENT_ID =
        "657740368257-25rd5mbgs9scckap948c8u4s4rtdlpku.apps.googleusercontent.com"

    // Future: Load from BuildConfig or remote config
    // val webClientId: String by lazy { Firebase.remoteConfig.getString("web_client_id") }
}
```,
  caption: [Authentication configuration extracted]
)

=== DVT-R3: Business Rules Configuration

Centralize business rule constants:

#figure(
```kotlin
// config/BusinessRules.kt
object BusinessRules {
    // Inventory check validity period (days)
    const val INVENTORY_CHECK_VALIDITY_DAYS = 75

    // Dimension adjustments for measurement calibration
    val THICKNESS_ADJUSTMENTS = mapOf(
        27.0f to 27.4f,
        42.0f to 42.4f
    )

    val WIDTH_ADJUSTMENTS = mapOf(
        42.0f to 42.4f
    )
}
```,
  caption: [Business rules as configuration]
)

=== DVT-R4: Quality Code Mappings

Extract quality mappings to a dedicated mapper:

#figure(
```kotlin
// domain/mapping/QualityNameMapper.kt
object QualityNameMapper {
    private val QUALITY_DISPLAY_NAMES = mapOf(
        "DUB-A|A" to "DUB A/A",
        "DUB-A|B" to "DUB A/B",
        "DUB-ABP" to "DUB A/B-P",
        // ... all mappings
    )

    fun getDisplayName(code: String?): String {
        return code?.let { QUALITY_DISPLAY_NAMES[it] ?: it } ?: ""
    }
}

// WarehouseSlot now delegates:
fun getFullQualityName(): String = QualityNameMapper.getDisplayName(quality)
```,
  caption: [Quality mappings extracted to dedicated mapper]
)

== SoC Refactorings — Responsibility Separation

=== SoC-R1: Split ManageItemViewModel

Extract business logic into dedicated use cases:

#figure(
```kotlin
// BEFORE: ManageItemViewModel (522 lines)
class ManageItemViewModel(...) {
    private fun parseQuantityStringToLong(quantityStr: String): Result<Long> { ... }
    private fun checkInventoryDone() { ... }
    private fun getDeviceId(): String { ... }
}

// AFTER: ManageItemViewModel (~200 lines) + Use Cases
class ManageItemViewModel(
    private val parseQuantityUseCase: ParseQuantityUseCase,
    private val checkInventoryStatusUseCase: CheckInventoryStatusUseCase,
    private val deviceIdProvider: DeviceIdProvider,
    // ...
) {
    fun addAction(actionType: ActionType) {
        val quantity = parseQuantityUseCase(quantityState.value)
        // ... orchestration only
    }
}

// domain/usecase/ParseQuantityUseCase.kt
class ParseQuantityUseCase(private val inputValidator: InputValidator) {
    operator fun invoke(input: String): Result<Long> { ... }
}

// domain/usecase/CheckInventoryStatusUseCase.kt
class CheckInventoryStatusUseCase(private val businessRules: BusinessRules) {
    operator fun invoke(slotActions: List<SlotAction>): Boolean { ... }
}
```,
  caption: [ManageItemViewModel split into orchestrator + use cases]
)

=== SoC-R2: Pure WarehouseSlot Data Class

Move business logic out of the data class:

#figure(
```kotlin
// BEFORE: WarehouseSlot with embedded logic (186 lines)
data class WarehouseSlot(...) {
    fun parsePropertiesFromProductId(): WarehouseSlot { ... }
    fun getFullQualityName(): String { ... }
    fun getVolume(): Double? { ... }
}

// AFTER: Pure data class + extension functions/mappers
data class WarehouseSlot(
    val fullProductId: String,
    val quantity: Long,
    val slotType: SlotType? = null,
    val quality: String? = null,
    val width: Float? = null,
    val thickness: Float? = null,
    val length: Int? = null,
    val lastModified: Timestamp? = null,
    val slotActions: List<SlotAction> = emptyList()
)

// domain/mapping/SlotIdParser.kt
class SlotIdParser(
    private val dimensionAdjuster: DimensionAdjuster
) {
    fun parseProperties(slot: WarehouseSlot): WarehouseSlot { ... }
}

// Extension functions for convenience
fun WarehouseSlot.displayName() = QualityNameMapper.getDisplayName(quality)
fun WarehouseSlot.volume() = VolumeCalculator.calculate(this)
```,
  caption: [WarehouseSlot as pure data class]
)

=== SoC-R3: Split UpdateManager

Decompose the monolithic UpdateManager:

#figure(
```kotlin
// BEFORE: UpdateManager (628 lines) with 7+ concerns

// AFTER: Focused classes
// domain/update/VersionChecker.kt (~50 lines)
class VersionChecker(private val appConfigRepository: AppConfigRepository) {
    suspend fun checkForUpdate(currentVersion: Int): UpdateAvailability { ... }
}

// domain/update/ApkDownloader.kt (~150 lines)
class ApkDownloader(private val downloadManager: DownloadManager) {
    fun downloadApk(url: String): Flow<DownloadProgress> { ... }
}

// domain/update/ApkInstaller.kt (~100 lines)
class ApkInstaller(private val packageInstaller: PackageInstaller) {
    fun installSilently(apkFile: File): InstallResult { ... }
    fun installManually(apkFile: File) { ... }
}

// domain/update/UpdateCoordinator.kt (~100 lines)
class UpdateCoordinator(
    private val versionChecker: VersionChecker,
    private val downloader: ApkDownloader,
    private val installer: ApkInstaller
) {
    suspend fun checkAndInstall(currentVersion: Int) { ... }
}
```,
  caption: [UpdateManager decomposed into focused classes]
)

== AVT Refactorings — Action Independence

=== AVT-R1: Slot ID Normalization Use Case

Create a single source of truth for slot ID parsing:

#figure(
```kotlin
// domain/usecase/NormalizeSlotIdUseCase.kt
class NormalizeSlotIdUseCase {
    data class Result(
        val normalizedId: String,
        val slotType: SlotType,
        val prefix: String?
    )

    operator fun invoke(fullSlotId: String): Result {
        return if (fullSlotId.length > 16
            && (fullSlotId.startsWith('S') || fullSlotId.startsWith('H'))
            && fullSlotId[1] == '-') {
            Result(
                normalizedId = fullSlotId.substring(2),
                slotType = if (fullSlotId.startsWith('S')) SlotType.Jointer else SlotType.Beam,
                prefix = fullSlotId.take(1)
            )
        } else {
            Result(normalizedId = fullSlotId, slotType = SlotType.Beam, prefix = null)
        }
    }
}

// Used in both SlotRepositoryImpl and SlotIdParser
```,
  caption: [Unified slot ID normalization]
)

=== AVT-R2: Strategy Pattern for Quantity Parsing

Allow quantity parsing rules to vary:

#figure(
```kotlin
// domain/usecase/ParseQuantityUseCase.kt
interface QuantityParser {
    fun parse(input: String): Result<Long>
}

class SumQuantityParser : QuantityParser {
    override fun parse(input: String): Result<Long> {
        // Handle "X+Y+Z" format
    }
}

class SimpleQuantityParser : QuantityParser {
    override fun parse(input: String): Result<Long> {
        return input.toLongOrNull()
            ?.let { Result.success(it) }
            ?: Result.failure(NumberFormatException())
    }
}

class ParseQuantityUseCase(
    private val parsers: List<QuantityParser>
) {
    operator fun invoke(input: String): Result<Long> {
        for (parser in parsers) {
            val result = parser.parse(input)
            if (result.isSuccess) return result
        }
        return Result.failure(NumberFormatException("Invalid quantity format"))
    }
}
```,
  caption: [Quantity parsing with strategy pattern]
)

== SoS Refactorings — State Isolation

=== SoS-R1: Granular Start Screen States

Split the monolithic StartUiState:

#figure(
```kotlin
// BEFORE: StartUiState (12 fields, 4 concern groups)

// AFTER: Focused state objects
// ui/state/ScanningState.kt
data class ScanningState(
    val scannedCode: String = "",
    val isFormatError: Boolean = false
)

// ui/state/DeviceState.kt
data class DeviceState(
    val shortenedDeviceId: String = "",
    val deviceName: String? = null
)

// ui/state/AppVersionState.kt
data class AppVersionState(
    val version: String = "",
    val versionCode: Int = -1
)

// ui/state/InventoryPermissionState.kt
data class InventoryPermissionState(
    val isPermitted: Boolean = false,
    val isEnabled: Boolean = false
)

// StartViewModel now exposes multiple flows
class StartViewModel(...) {
    val scanningState: StateFlow<ScanningState>
    val deviceState: StateFlow<DeviceState>
    val appVersionState: StateFlow<AppVersionState>
    val inventoryState: StateFlow<InventoryPermissionState>
}
```,
  caption: [StartUiState split into focused states]
)

=== SoS-R2: Update Progress State Machine

Model update states explicitly:

#figure(
```kotlin
// domain/update/UpdateState.kt
sealed class UpdateState {
    object Idle : UpdateState()
    data class Available(
        val version: String,
        val versionCode: Int,
        val releaseNotes: String
    ) : UpdateState()

    data class Downloading(val progress: Int) : UpdateState()
    object Installing : UpdateState()
    data class Completed(val newVersion: String) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

// Transitions are explicit and type-safe
fun UpdateState.canDownload(): Boolean = this is UpdateState.Available
fun UpdateState.canInstall(): Boolean = this is UpdateState.Downloading && progress == 100
```,
  caption: [Update state as sealed class hierarchy]
)

== Refactoring Order

To minimize risk, refactorings will be applied in the following order:

#figure(
  table(
    columns: (auto, auto, auto, auto),
    inset: 8pt,
    align: left,
    [*Order*], [*Refactoring*], [*Risk*], [*Rationale*],
    [1], [DVT-R1: FirestoreConfig], [Low], [Mechanical extraction, no behavior change],
    [2], [DVT-R3: BusinessRules], [Low], [Constants extraction, easy to verify],
    [3], [DVT-R4: QualityNameMapper], [Low], [Pure function extraction],
    [4], [AVT-R1: NormalizeSlotIdUseCase], [Medium], [Removes duplication, needs testing],
    [5], [SoC-R2: Pure WarehouseSlot], [Medium], [Extracts logic, changes API slightly],
    [6], [SoC-R1: ManageItemViewModel], [High], [Core business logic extraction],
    [7], [SoC-R3: Split UpdateManager], [Medium], [Large refactoring, good test coverage],
    [8], [SoS-R1: Granular States], [Medium], [UI changes, manual testing needed],
  ),
  caption: [Refactoring order by risk level]
)

Each refactoring will be:
1. Implemented in a focused commit
2. Verified with existing tests
3. Manually tested on a device before proceeding

== Expected Outcomes

After refactoring, the codebase will exhibit:

- *Reduced file sizes:* ManageItemViewModel from 522 to ~200 lines
- *Improved testability:* Each use case testable in isolation
- *Configuration centralization:* Single source for Firestore paths, business rules
- *Type-safe states:* Sealed classes prevent invalid state combinations
- *Linear change propagation:* Adding a quality code requires editing one file

== Implementation Status

=== Completed Refactorings

The following refactorings have been implemented and verified with tests:

==== DVT Refactorings — Configuration Extraction

1. *FirestoreConfig.kt* — Centralized Firestore collection names and paths
   - `COLLECTION_BEAMS`, `COLLECTION_JOINTERS` constants
   - `SUBCOLLECTION_ACTIONS` constant
   - Used by `SlotType` enum and `SlotRepositoryImpl`

2. *AppConfig.kt* — Application-wide settings
   - Inventory check stale period (75 days)
   - Future: feature flags, timeouts

3. *QualityConfig.kt* — Quality code display name mappings
   - 18 quality code mappings (DUB-A|A → "DUB A/A", etc.)
   - Used by `WarehouseSlot.getFullQualityName()`

4. *DimensionConfig.kt* — Dimension adjustment rules
   - Thickness adjustments (27mm → 27.4mm, 42mm → 42.4mm)
   - Width adjustments (42mm → 42.4mm)
   - Used by `WarehouseSlot.parsePropertiesFromProductId()`

==== SoC Refactorings — Responsibility Separation

1. *CheckInventoryStatusUseCase.kt* — Inventory status checking logic
   - Extracted from `ManageItemViewModel.checkInventoryDone()`
   - 17 unit tests covering all edge cases

2. *QuantityParser.kt* — Quantity input parsing
   - Extracted from `ManageItemViewModel.parseQuantityStringToLong()`
   - Supports simple numbers, sums (50+30+20), item code detection
   - 18 unit tests covering parsing scenarios

==== AVT Refactorings — Action Independence

1. *SlotActionOperations.kt* — Interfaces for slot operations
   - `SlotActionOperation` interface for adding actions
   - `UndoSlotActionOperation` interface for undoing actions
   - Enables future algorithm versioning

2. *AddSlotActionUseCase* — Now implements `SlotActionOperation`
   - Backward-compatible `invoke` operator maintained
   - Interface allows alternative implementations

3. *UndoSlotActionUseCase.kt* — Dedicated undo operation
   - Implements `UndoSlotActionOperation`
   - Extracted from `ManageItemViewModel.undoLastAction()`

=== Test Coverage Summary

#figure(
  table(
    columns: (auto, auto, auto),
    inset: 8pt,
    align: left,
    [*Test Class*], [*Tests*], [*Coverage*],
    [QuantityParserTest], [18], [All parsing scenarios],
    [CheckInventoryStatusUseCaseTest], [17], [All inventory check scenarios],
    [InputValidatorTest], [28], [Quantity validation, code validation],
    [WarehouseSlotTest], [18], [Property parsing, volume calculation],
    [FirestoreSlotTest], [4], [Firestore conversion],
    [SlotActionTest], [5], [Action creation],
    [SlotTypeTest], [4], [Enum behavior],
    [FormatHelperFunctionsTest], [5], [Date formatting],
    [StartViewModelTest], [11], [Scanned code handling],
    [HistoryViewModelTest], [3], [History loading],
    [*Total*], [*112+*], [],
  ),
  caption: [Test coverage after refactoring]
)

