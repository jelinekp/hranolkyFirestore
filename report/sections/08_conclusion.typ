= Conclusion

== Achievements

=== Quantitative Results

The refactoring effort achieved measurable improvements across several dimensions:

#figure(
  table(
    columns: (auto, auto, auto, auto),
    inset: 8pt,
    align: left,
    [*Metric*], [*Before*], [*After*], [*Change*],
    [Test count], [~30], [155], [+417%],
    [Config locations], [12+ files], [4 config files], [Centralized],
    [ManageItemViewModel lines], [522], [~450], [-14%],
    [Extracted use cases], [0], [4], [+4 reusable],
    [Extracted config modules], [0], [4], [+4 centralized],
    [State isolation modules], [0], [2], [+2 focused],
  ),
  caption: [Quantitative improvements from NS refactoring]
)

=== Qualitative Improvements

- *Improved Testability:* Business logic extracted into use cases (QuantityParser, CheckInventoryStatusUseCase) can now be tested in isolation with 41 dedicated tests, compared to near-zero testability of the embedded logic before.

- *Better Maintainability:* Configuration changes now require editing a single file (e.g., QualityConfig.kt for quality mappings) instead of hunting through multiple classes.

- *Centralized Configuration:* All 18 quality code mappings, dimension adjustments, and Firestore collection names are now in dedicated config files, eliminating scattered magic strings.

- *Reusable Components:* The extracted QuantityParser can be reused in future features (e.g., bulk import) without duplicating the 40+ lines of parsing logic.

- *Data-Driven Design:* Quality mappings and dimension adjustments are now data structures that could be loaded from external sources (Firestore, JSON) without code changes.

- *State Isolation:* UpdateState and AuthState are now decomposed into focused states (UpdateAvailability, DownloadState, InstallationState, AuthenticationStatus, SignInOperation), reducing combinatorial complexity and enabling independent evolution.

- *Strategy Pattern for External Logging:* The ExternalActionLogger interface allows SheetDB logging to be replaced or disabled without modifying use cases, improving testability and future flexibility.

== Lessons Learned

1. *Test First:* Establishing 155 tests before refactoring provided confidence that behavior is preserved. Several subtle bugs in test expectations were caught early.

2. *Incremental Extraction:* Small, focused extractions (one config file, one use case at a time) are safer than large rewrites. Each extraction was verified with the test suite before proceeding.

3. *NS Theory Applicability:* The four theorems provided a systematic framework for identifying violations. DVT was particularly effective for finding scattered configuration, while SoC guided the extraction of use cases from ViewModels.

4. *Backward Compatibility:* Using Kotlin's `invoke()` operator and delegation patterns allowed new interfaces to coexist with existing code, enabling gradual migration.

5. *State Isolation Benefits:* Breaking down monolithic state classes (UpdateState with 15+ fields) into focused states (UpdateAvailability, DownloadState, etc.) improved code readability and will simplify future state machine implementations.

== Limitations and Future Work

While this refactoring addressed the most impactful NS violations, several opportunities remain for further improvement:

=== Complete ViewModel Decomposition

ManageItemViewModel (522 lines) still handles multiple responsibilities that could be further extracted:

*Current concerns in ManageItemViewModel:*
- Slot data loading and caching
- Quantity input validation
- Action execution (add/undo)
- Inventory check toggle persistence
- Navigation to other items
- Snackbar event coordination

*Recommended extractions:*
1. *NavigationCoordinator:* Extract navigation logic (`navigateToAnotherItem`, item code scanning redirect) into a dedicated class
2. *InventoryCheckPreferences:* Move SharedPreferences operations for inventory check toggle into a repository
3. *QuantityInputState:* Create a dedicated state holder for quantity input with its validation

*Implementation effort:* ~8-12 hours for clean separation with tests

=== State Machine Implementation

The isolated states (`UpdateStates.kt`, `AuthStates.kt`) are semantically prepared for finite state machine patterns but currently use manual state transitions. A proper state machine would provide:

*Benefits:*
- Compile-time verification of valid state transitions
- Elimination of impossible state combinations
- Clearer documentation of the update/auth flows
- Easier debugging with state history

*Implementation approach:*
```kotlin
// Example state machine definition
sealed class UpdateEvent {
    data object CheckForUpdates : UpdateEvent()
    data class UpdateFound(val info: UpdateInfo) : UpdateEvent()
    data object StartDownload : UpdateEvent()
    data class DownloadProgress(val percent: Int) : UpdateEvent()
    data object InstallUpdate : UpdateEvent()
}

class UpdateStateMachine {
    fun transition(event: UpdateEvent): CompositeUpdateState
    fun canHandle(event: UpdateEvent): Boolean
}
```

*Implementation effort:* ~6-10 hours using a library like Tinder StateMachine or custom implementation

=== External Configuration

Config files (`QualityConfig.kt`, `DimensionConfig.kt`, `FirestoreConfig.kt`) currently contain compile-time constants. Loading from Firestore Remote Config would enable:

*Benefits:*
- Quality code additions without app releases
- A/B testing of dimension adjustments
- Emergency configuration changes
- Per-device or per-user configuration

*Implementation approach:*
1. Create `RemoteConfigRepository` interface
2. Implement Firestore document-based config loading
3. Add caching layer with expiration (e.g., 1 hour)
4. Provide fallback to compiled defaults when offline

```kotlin
interface ConfigProvider {
    suspend fun getQualityMappings(): Map<String, String>
    suspend fun getDimensionAdjustments(): Map<Float, Float>
}

class RemoteConfigProvider(
    private val firestore: FirebaseFirestore,
    private val cache: ConfigCache
) : ConfigProvider
```

*Implementation effort:* ~10-15 hours including offline handling and migration

=== Action Versioning

The `SlotActionOperation` and `UndoSlotActionOperation` interfaces are prepared for versioning but not yet implemented. This would allow:

*Benefits:*
- Different action formats for different app versions
- Backward-compatible action replay
- Audit trail with version metadata
- Migration support for legacy actions

*Implementation approach:*
```kotlin
interface VersionedSlotActionOperation : SlotActionOperation {
    val version: Int
    fun canHandle(actionVersion: Int): Boolean
}

class SlotActionOperationFactory {
    fun getOperation(version: Int): SlotActionOperation
    fun getCurrentVersion(): Int
}

// Action document would include version field
data class VersionedSlotAction(
    val version: Int = CURRENT_VERSION,
    // ... existing fields
)
```

*Implementation effort:* ~8-12 hours including migration logic and tests

=== Summary of Future Work

#figure(
  table(
    columns: (auto, auto, auto),
    inset: 8pt,
    align: left,
    [*Improvement*], [*Effort*], [*Priority*],
    [ViewModel Decomposition], [8-12 hours], [Medium],
    [State Machine], [6-10 hours], [Low],
    [External Configuration], [10-15 hours], [High],
    [Action Versioning], [8-12 hours], [Low],
  ),
  caption: [Estimated effort for remaining improvements]
)

The *External Configuration* improvement has the highest priority as it would enable operational agility without requiring app store releases—a significant benefit for a warehouse management application.

== Final Remarks

This refactoring demonstrates that Normalized Systems Theory provides actionable guidance for improving Android application maintainability. The four theorems—SoC, DVT, AVT, and SoS—offer a systematic approach to identifying and addressing architectural issues that cause combinatorial effects.

The investment in a comprehensive test suite (155 tests) proved essential: it enabled confident refactoring while ensuring full backward compatibility. All existing functionality remains intact, verified through automated testing.

The extracted modules now enable changes to configuration, parsing logic, business rules, and state management without triggering modifications across multiple files—achieving the *linear scalability* promised by Normalized Systems Theory.

For practitioners considering similar refactoring efforts, the key recommendations are:
1. Establish tests before refactoring
2. Extract one concern at a time
3. Maintain backward compatibility through interfaces and delegation
4. Use NS theorems as a systematic checklist for identifying violations

The hranolky-firestore application is now better positioned for future evolution, with clear separation of concerns and centralized configuration that will reduce maintenance cost and defect risk.
