= Executive Summary

This report documents the refactoring of the *Beams and Jointers* (_Hranolky a Spárovky_) warehouse management application
according to Normalized Systems (NS) Theory principles.
The application, built with Kotlin and Jetpack Compose for Firebase, runs on Zebra TC200J Android terminals and
manages inventory data for a woodworking company producing timber beams (_hranolky_) and jointer boards (_spárovky_).

== Motivation

The original codebase, while functional, exhibited several maintainability challenges common to Android applications:

- *Monolithic ViewModels:* The `ManageItemViewModel` contained 522 lines handling device identification, quantity parsing, and business rules. Similarly, `UpdateManager` (628 lines) handled downloading, installation, and UI updates in a single class.

- *Scattered Configuration:* Firestore collection names ("Hranolky", "Sparovky"), quality code mappings, and business rules (75-day inventory validity) were hardcoded across multiple files.

- *Mixed Responsibilities:* The `WarehouseSlot` data class (186 lines) contained parsing logic, dimension adjustments, and display formatting alongside pure data fields.

- *Tight Coupling:* Algorithm implementations (quantity parsing, inventory status checking) were embedded directly in ViewModels, making them impossible to test in isolation or evolve independently.

These issues created *combinatorial effects*—situations where small changes required modifications across multiple files, increasing maintenance cost and defect risk.

== Approach

Using NS Theory's four core theorems—Separation of Concerns (SoC), Data Version Transparency (DVT), Action Version Transparency (AVT), and Separation of States (SoS)—we:

1. *Analyzed* the codebase to identify 13 specific violations across all four theorems
2. *Designed* a target architecture with centralized configuration and extracted use cases
3. *Established* a comprehensive test suite (338+ tests) as a safety net
4. *Refactored* incrementally, verifying tests after each change

== Key Results

#figure(
  text(size: 10pt)[
    #table(
    columns: (auto, auto, auto),
    inset: 7pt,
    align: left,
    [*Category*], [*Extracted Module*], [*Benefit*],
    [DVT], [FirestoreConfig/QualityConfig], [Centralized config with remote override],
    [DVT], [ConfigCache/Provider], [Runtime updates without app releases],
    [SoC], [ManageItemViewModel], [Reduced from 522 to ~200 lines],
    [SoC], [UpdateManager], [Decomposed into 4 focused components],
    [SoC/AVT], [CheckInventoryStatusUseCase], [Business logic isolated, fully tested],
    [SoC/AVT], [QuantityParser], [Complex parsing logic extracted & tested],
    [AVT], [SlotActionOperations], [Interfaces for action versioning],
    [AVT], [ExternalActionLogger], [Strategy pattern for logging, 6 tests],
    [SoS], [UpdateStateMachine], [Compile-time valid transitions (34 tests)],
    [SoS], [AuthState/UpdateState], [Decomposed into granular sealed classes],
  )
  ],
  caption: [Summary of extracted modules and improvements]
)

A comprehensive test suite was established as the foundation:

#figure(
text(size: 10pt)[
    #table(
    columns: (auto, auto),
    inset: 7pt,
    align: left,
    [*Test Category*], [*Test Count*],
    [Domain Logic (InputValidator, Parsers)], [46],
    [Model Classes (WarehouseSlot, SlotAction)], [30+],
    [Use Cases (Inventory, Quantity, Logging)], [60+],
    [ViewModels (Start, History, ManageItem)], [60+],
    [State Isolation (Update, Auth)], [37+],
    [Configuration & Utils], [40+],
    [*Total*], [*338*],
  )],
  caption: [Test suite coverage summary]
)

== Document Structure

The remainder of this report is organized as follows:

- *Section 2 (Theory):* Introduces NS Theory and its four theorems, with Android-specific examples
- *Section 3 (Analysis):* Documents violations found in the original codebase with code excerpts
- *Section 4 (Refactoring):* Presents the target architecture and implementation details
- *Section 5 (Conclusion):* Summarizes achievements, lessons learned, and future work

== Conclusion

The refactoring successfully improved the codebase's modularity and testability while maintaining
full backward compatibility.
All existing functionality remains intact, verified through comprehensive automated testing.
The extracted modules now enable changes to configuration, parsing logic, and business rules
without triggering modifications across multiple files—achieving the *linear scalability*
promised by Normalized Systems Theory.
