= Theory

This section introduces Normalized Systems (NS) Theory and its four core theorems that
guided the refactoring process.

== Normalized Systems Theory

Normalized Systems Theory, developed by Herwig Mannaert at the University of Antwerp,
provides a theoretical framework for building evolvable software systems.
The theory identifies *combinatorial effects*—situations where a single change propagates
to multiple locations in the codebase—as the primary obstacle to software
evolvability @mannaert2016normalized.

In traditional software development, small functional changes often require modifications
across multiple files and components.
This phenomenon, sometimes called "ripple effects" or "shotgun surgery," leads to:

- *Increased maintenance cost:* Developers spend more time locating and modifying code
- *Higher defect rates:* Each additional change point increases the probability of introducing bugs
- *Reduced developer confidence:* Fear of unintended side effects discourages refactoring
- *Technical debt accumulation:* Teams avoid changes that should be made

NS Theory addresses these issues by defining four fundamental theorems that, when followed, eliminate combinatorial effects and enable *linear scalability* of maintenance effort—meaning that the effort to implement a change grows linearly with the size of the change itself, not exponentially with the system's complexity.

== Four Core Theorems

=== Separation of Concerns (SoC)

The SoC theorem states that each software element should address exactly one concern.
When a component mixes multiple concerns (e.g., UI rendering and business logic),
changes to one concern require modifications to the entire component, creating unnecessary coupling.

*In Android applications:* // TODO explain in Android context

*Key insight:*
Each extracted module should be independently testable and deployable.
If you cannot write a unit test for a piece of functionality without rendering an entire component, SoC is likely violated.

*Violation example in hranolky-firestore:*
```kotlin
// TODO find an example in my app
```

=== Data Version Transparency (DVT)

The DVT theorem requires that data structures and configuration values can evolve without
causing ripple effects throughout the codebase.
Instead of hardcoding values directly in components,
configuration should be centralized in dedicated modules.

*In Android applications:* DVT violations often appear as:
// TODO expand this part

*Key insight:* If adding a new admin user or changing a database collection name requires
editing multiple files, DVT is violated.

*Violation example:*
```kotlin
// TODO find an example in my app
```

=== Action Version Transparency (AVT)

The AVT theorem ensures that actions (operations, functions, algorithms)
can evolve independently of their consumers.
When business logic is embedded directly in UI components,
changes to the logic require modifying presentation code.

*In Android applications:* // TODO explain in Android context

*Key insight:* // TODO

=== Separation of States (SoS)

The SoS theorem requires that state changes are isolated to prevent unintended side effects.
When multiple unrelated pieces of state are managed together,
changes to one state can inadvertently affect others.

*In Adnroid applications:* SoS violations manifest as:
// TODO expand this part

*Key insight:* // TODO

*Violation example:*
```kotlin
// TODO find an example in my app
```

== Application to Android Development

While NS Theory was originally developed for enterprise Java systems,
its principles translate directly to Kotlin Android applications which share the Java roots:

#table(
  columns: (auto, auto, auto),
  inset: 8pt,
  align: left,
  [*NS Theorem*], [*Android Pattern*], [*Example*],
  // TODO fill in the table
  [SoC],
  [DVT],
  [AVT],
  [SoS],
)

== Benefits of NS-Compliant Architecture

Systems that follow NS Theory principles exhibit:

1. *Linear maintenance scalability:*
The effort to implement a change is proportional to the change's inherent complexity, not the system's size
2. *Improved testability:*
Focused modules can be unit tested in isolation
3. *Reduced fear of change:*
Developers can confidently make modifications knowing the impact is contained
4. *Better code comprehension:*
Each module has a single, clear purpose

The following sections demonstrate how these principles were applied to identify violations in the current system and systematically resolve them through targeted refactoring.
