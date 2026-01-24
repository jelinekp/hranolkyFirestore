= Conclusion

== Achievements

The refactoring effort achieved measurable improvements across both the Android terminal application and the React web application.

=== Quantitative Results

#grid(
  columns: (1fr, 1fr),
  column-gutter: 1em,
  [
    #figure(
      table(
        columns: (auto, auto),
        inset: 6pt,
        align: left,
        [*Android App Metric*], [*Value*],
        [Total tests], [337],
        [Test increase], [+1023%],
        [Config locations], [Centralized],
        [ViewModel size], [-62%],
        [Extracted use cases], [6],
        [State machine tests], [34],
        [Remote config], [Enabled],
      ),
      caption: [Android App Results]
    )
  ],
  [
    #figure(
      table(
        columns: (auto, auto),
        inset: 6pt,
        align: left,
        [*Web App Metric*], [*Value*],
        [Total tests], [196],
        [Test files], [26],
        [Modules extracted], [20],
        [Source files], [+51%],
        [Chart component], [Split to 5],
        [Code reorg], [~4700 lines],
        [Error boundaries], [Added],
      ),
      caption: [Web App Results]
    )
  ]
)

=== Qualitative Improvements

*Cross-Platform Benefits:*
- *Testability:* Both platforms moved from near-zero testability to having comprehensive test suites (530+ tests total). Business logic is now isolated from UI code.
- *Maintainability:* Centralized configuration (DVT) in both apps allows changing admin emails, collection names, and business rules without hunting through dozens of files.
- *Linear Scalability:* Adding new features (e.g., a new wood quality type) now requires changes in predictable, isolated locations rather than scattered across the codebase.

*Android Specifics:*
- *Runtime Updates:* The Android app can now receive configuration updates via Firestore without a new APK deployment.
- *Compile-Time Safety:* The `UpdateStateMachine` enforces valid transitions, eliminating a class of runtime errors.

*Web App Specifics:*
- *Reusable Components:* Components like `ExportDialog` and `AccessDenied` are now generic and reusable.
- *Fault Isolation:* Error boundaries prevent single-component failures from crashing the entire dashboard.

== Lessons Learned

1.  *Universality of NS Theory:* The four theorems (SoC, DVT, AVT, SoS) proved equally effective for analyzing and refactoring strictly typed Kotlin code and flexible TypeScript/React code. The manifestations differ (e.g., God ViewModels vs. Giant Components), but the root cause—combinatorial effects—is identical.

2.  *Test-First Strategy:* Establishing a safety net of tests was crucial for both projects. It allowed for safe structural changes and provided executable documentation of the system's behavior.

3.  *Incremental Extraction:* Attempts to "rewrite everything" usually fail. The successful strategy was extracting one concern at a time (e.g., "extract sorting logic" or "extract quantity parser") and verifying it.

4.  *State Management:* Both platforms suffered significantly from Separation of States (SoS) violations. In Android, this meant overloaded State classes; in React, it meant component-local state that should have been lifted or isolated in hooks. Fixing SoS improved code clarity dramatically.

== Final Remarks

This project demonstrates that *Normalized Systems Theory provides a robust, platform-agnostic framework for software evolution.*

By identifying violations of SoC, DVT, AVT, and SoS, we transformed two monolithic applications into modular, testable, and adaptable systems.
The resulting architecture supports the *linear scalability* of maintenance effort: the cost of a change now corresponds to the size of the change itself, not the size of the system.

For practitioners considering similar refactoring efforts, the key recommendations are:
1. Establish tests before refactoring
2. Extract one concern at a time
3. Maintain backward compatibility through interfaces and delegation
4. Use NS theorems as a systematic checklist for identifying violations

The *Beams and Jointers* (_Hranolky a Spárovky_) application is now better positioned for future evolution, with clear separation of concerns and centralized configuration that will reduce maintenance cost and defect risk.
