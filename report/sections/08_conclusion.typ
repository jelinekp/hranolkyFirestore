#import "../styles.typ": *

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
        fill: (col, row) => if row == 0 { color-android-header } else { white },
        [*Android App Metric*], [*Value*],
        [Total tests], [#txt-add[*337*]],
        [Test increase], [#txt-add[*+1023%*]],
        [Kotlin files changed], [#txt-mod[*62*]],
        [Production files], [39 #txt-mod[(+3,295 lines)]],
        [Test files], [23 #txt-mod[(+3,836 lines)]],
        [Total lines changed], [#txt-add[+7,362], #txt-del[-231], #txt-mod[*+7,131*]],
        [Extracted use cases], [#txt-add[*6*]],
        [State machine tests], [#txt-add[*34*]],
        [Remote config], [#txt-mod[Shared with Web App]],
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
        fill: (col, row) => if row == 0 { color-web-header } else { white },
        [*Web App Metric*], [*Value*],
        [Total tests], [#txt-add[*196*]],
        [Total files changed], [#txt-mod[*120*]],
        [Test files], [26 #txt-add[(+2,381 lines)]],
        [App logic files], [66 #txt-mod[(+2,487 lines)]],
        [Other files], [28 #txt-mod[(+1,030 lines)]],
        [Total lines changed], [#txt-add[+6,911], #txt-del[-1,013], #txt-mod[*+5,898*]],
        [Modules extracted], [#txt-add[*20*]],
        [Chart component], [#txt-mod[Split to 5]],
        [Error boundaries], [#txt-add[Added]],
        [Remote config], [#txt-mod[Shared with Android App]],
      ),
      caption: [Web App Results]
    )
  ]
)

#text(size: 9pt, style: "italic")[
  _Note: "Other files" includes documentation, configuration files, and assets. The significant insertions in App Logic reflect the introduction of new modules and strong typing._
]

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
