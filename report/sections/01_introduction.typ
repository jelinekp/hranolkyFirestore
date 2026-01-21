= Executive Summary

This report documents the refactoring of the *hranolky-firestore* warehouse management application
according to Normalized Systems (NS) Theory principles.
The application, built with Kotlin and Jetpack Compose for Firebase, runs on Zebra TC200J Android terminals and
manages inventory data for a woodworking company producing timber beams (_hranolky_) and jointer boards (_spárovky_).

== Motivation

The original codebase, while functional, exhibited several maintainability challenges common to Android applications:

- *Monolithic ViewModels:* The `ManageItemViewModel` contained 522 lines handling device identification, quantity parsing, inventory calculations, preferences, navigation, and undo logic—six distinct concerns in a single class.

- *Scattered Configuration:* Firestore collection names ("Hranolky", "Sparovky"), quality code mappings, and business rules (75-day inventory validity) were hardcoded across multiple files.

- *Mixed Responsibilities:* The `WarehouseSlot` data class (186 lines) contained parsing logic, dimension adjustments, and display formatting alongside pure data fields.

- *Tight Coupling:* Algorithm implementations (quantity parsing, inventory status checking) were embedded directly in ViewModels, making them impossible to test in isolation or evolve independently.

These issues created *combinatorial effects*—situations where small changes required modifications across multiple files, increasing maintenance cost and defect risk.

== Approach

Using NS Theory's four core theorems—Separation of Concerns (SoC), Data Version Transparency (DVT), Action Version Transparency (AVT), and Separation of States (SoS)—we:

1. *Analyzed* the codebase to identify 13 specific violations across all four theorems
2. *Designed* a target architecture with centralized configuration and extracted use cases
3. *Established* a comprehensive test suite (112+ tests) as a safety net
4. *Refactored* incrementally, verifying tests after each change

== Key Results

#figure(
  table(
    columns: (auto, auto, auto),
    inset: 8pt,
    align: left,
    [*Category*], [*Extracted Module*], [*Benefit*],
    [DVT], [FirestoreConfig.kt], [Single source for collection names],
    [DVT], [QualityConfig.kt], [18 quality mappings centralized],
    [DVT], [DimensionConfig.kt], [Dimension adjustments in one place],
    [DVT], [AppConfig.kt], [Business rule constants extracted],
    [SoC/AVT], [QuantityParser.kt], [40+ lines extracted, 18 tests],
    [SoC/AVT], [CheckInventoryStatusUseCase.kt], [Business logic isolated, 17 tests],
    [AVT], [UndoSlotActionUseCase.kt], [Undo operation decoupled],
    [AVT], [SlotActionOperations.kt], [Interfaces for action versioning],
  ),
  caption: [Summary of extracted modules]
)

A comprehensive test suite was established as the foundation:

#figure(
  table(
    columns: (auto, auto),
    inset: 8pt,
    align: left,
    [*Test Category*], [*Test Count*],
    [Domain Logic (InputValidator, Parsers)], [46],
    [Model Classes (WarehouseSlot, SlotAction)], [27],
    [Use Cases (Inventory, Quantity)], [35],
    [ViewModels (Start, History)], [14],
    [*Total*], [*112+*],
  ),
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
