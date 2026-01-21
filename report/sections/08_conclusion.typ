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
    [Test count], [~30], [149], [+397%],
    [Config locations], [12+ files], [4 config files], [Centralized],
    [ManageItemViewModel lines], [522], [~450], [-14%],
    [Extracted use cases], [0], [4], [+4 reusable],
    [Extracted config modules], [0], [4], [+4 centralized],
    [State isolation modules], [0], [2], [+2 focused],
  ),
  caption: [Quantitative improvements from NS refactoring]
)

=== Qualitative Improvements

- *Improved Testability:* Business logic extracted into use cases (QuantityParser, CheckInventoryStatusUseCase) can now be tested in isolation with 35 dedicated tests, compared to near-zero testability of the embedded logic before.

- *Better Maintainability:* Configuration changes now require editing a single file (e.g., QualityConfig.kt for quality mappings) instead of hunting through multiple classes.

- *Centralized Configuration:* All 18 quality code mappings, dimension adjustments, and Firestore collection names are now in dedicated config files, eliminating scattered magic strings.

- *Reusable Components:* The extracted QuantityParser can be reused in future features (e.g., bulk import) without duplicating the 40+ lines of parsing logic.

- *Data-Driven Design:* Quality mappings and dimension adjustments are now data structures that could be loaded from external sources (Firestore, JSON) without code changes.

- *State Isolation:* UpdateState and AuthState are now decomposed into focused states (UpdateAvailability, DownloadState, InstallationState, AuthenticationStatus, SignInOperation), reducing combinatorial complexity and enabling independent evolution.

== Lessons Learned

1. *Test First:* Establishing 112+ tests before refactoring provided confidence that behavior is preserved. Several subtle bugs in test expectations were caught early.

2. *Incremental Extraction:* Small, focused extractions (one config file, one use case at a time) are safer than large rewrites. Each extraction was verified with the test suite before proceeding.

3. *NS Theory Applicability:* The four theorems provided a systematic framework for identifying violations. DVT was particularly effective for finding scattered configuration, while SoC guided the extraction of use cases from ViewModels.

4. *Backward Compatibility:* Using Kotlin's `invoke()` operator and delegation patterns allowed new interfaces to coexist with existing code, enabling gradual migration.

5. *State Isolation Benefits:* Breaking down monolithic state classes (UpdateState with 15+ fields) into focused states (UpdateAvailability, DownloadState, etc.) improved code readability and will simplify future state machine implementations.

== Limitations and Future Work

While this refactoring addressed the most impactful NS violations, several opportunities remain:

- *Complete ViewModel Decomposition:* ManageItemViewModel still has multiple responsibilities; further extraction of validation and navigation logic would improve modularity.

- *State Machine Implementation:* The isolated states (UpdateStates.kt, AuthStates.kt) are prepared for state machine patterns but not yet implemented.

- *External Configuration:* Config files could be loaded from Firestore/Remote Config for runtime updates without app releases.

- *Strategy Pattern for Logging:* SheetDB logging could be abstracted behind a strategy interface for easier replacement or testing.

== Final Remarks

This refactoring demonstrates that Normalized Systems Theory provides actionable guidance for improving Android application maintainability. The four theorems—SoC, DVT, AVT, and SoS—offer a systematic approach to identifying and addressing architectural issues that cause combinatorial effects.

The investment in a comprehensive test suite (149 tests) proved essential: it enabled confident refactoring while ensuring full backward compatibility. All existing functionality remains intact, verified through automated testing.

The extracted modules now enable changes to configuration, parsing logic, business rules, and state management without triggering modifications across multiple files—achieving the *linear scalability* promised by Normalized Systems Theory.

For practitioners considering similar refactoring efforts, the key recommendations are:
1. Establish tests before refactoring
2. Extract one concern at a time
3. Maintain backward compatibility through interfaces and delegation
4. Use NS theorems as a systematic checklist for identifying violations

The hranolky-firestore application is now better positioned for future evolution, with clear separation of concerns and centralized configuration that will reduce maintenance cost and defect risk.
