= Executive Summary

This report documents the refactoring of the *Beams and Jointers* (_Hranolky a Spárovky_) warehouse management system according to Normalized Systems (NS) Theory principles.
The system consists of two primary components sharing a common Firebase backend:
1.  A native *Android terminal application* (Zebra TC200J) for warehouse data entry.
2.  A *React web application* for administration and reporting.

== Motivation

Both codebases, while functional, exhibited maintainability challenges typical of rapid application development:

- *Android Challenges:* Monolithic ViewModels (e.g., `ManageItemViewModel` with 522 lines), scattered configuration, and business logic tightly coupled to UI components.
- *Web App Challenges:* React components mixing UI rendering with complex filtering logic, hardcoded configuration values, and state management causing unnecessary re-renders.

These issues created *combinatorial effects*—situations where small changes required modifications across multiple files, increasing maintenance cost and defect risk.

== Approach

Using NS Theory's four core theorems—Separation of Concerns (SoC), Data Version Transparency (DVT), Action Version Transparency (AVT), and Separation of States (SoS)—we:

1.  *Analyzed* both codebases to identify specific violations (13 in Android, 9 in Web App).
2.  *Designed* target architectures enabling independent evolution of components.
3.  *Established* comprehensive test suites (338 Android tests, 196 Web tests) as a safety net.
4.  *Refactored* incrementally, verifying tests after each change.

== Key Results

=== Android Application Refactoring

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
  caption: [Summary of Android improvements]
)

=== Web Application Refactoring

#figure(
  text(size: 10pt)[
    #table(
    columns: (auto, auto, auto),
    inset: 7pt,
    align: left,
    [*Category*], [*Extracted Module*], [*Benefit*],
    [SoC], [Custom Hooks], [Logic separated from UI components],
    [SoC], [Chart Subcomponents], [Complex chart broken down into 5 focused files],
    [DVT], [Config Module], [Centralized admin emails and collection names],
    [SoS], [Filter State Hook], [Isolated filtering logic, independently testable],
  )
  ],
  caption: [Summary of Web App improvements]
)

== Conclusion

The refactoring successfully improved the modularity and testability of both applications while maintaining full backward compatibility.
The combined test suite of over *530 automated tests* ensures reliability.
The extracted modules now enable changes to configuration, business rules, and UI logic without triggering modifications across multiple files—achieving the *linear scalability* promised by Normalized Systems Theory across the entire system.

