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

NS Theory addresses these issues by defining four fundamental theorems that, when followed,
eliminate combinatorial effects and enable *linear scalability* of maintenance effort—meaning
that the effort to implement a change grows linearly with the size of the change itself,
not exponentially with the system's complexity.

== Four Core Theorems

=== Separation of Concerns (SoC)

The SoC theorem states that each software element should address exactly one concern.
When a component mixes multiple concerns (e.g., UI rendering and business logic),
changes to one concern require modifications to the entire component, creating unnecessary coupling.

*In Android applications:* SoC violations commonly appear as:
- *God ViewModels:* ViewModels that handle navigation, data fetching, validation, and business logic
- *Fat data classes:* Model classes that contain transformation logic, formatting, and business rules
- *Monolithic managers:* Single classes handling downloading, installation, progress monitoring, and cleanup

*Key insight:*
Each extracted module should be independently testable and deployable.
If you cannot write a unit test for a piece of functionality without instantiating an entire ViewModel, SoC is likely violated.

*Violation example in Beams and Jointers app:*
```kotlin
// ManageItemViewModel.kt (522 lines) handles 6+ concerns:
class ManageItemViewModel(...) : AndroidViewModel(application) {
    // Concern 1: Device identification
    private fun getDeviceId(): String { ... }

    // Concern 2: Quantity parsing (40+ lines)
    private fun parseQuantityStringToLong(quantityStr: String): Result<Long> { ... }

    // Concern 3: Inventory date calculations
    private fun checkInventoryDone() { ... }

    // Concern 4: SharedPreferences access
    private val sharedPreferences by lazy { ... }

    // Concern 5: Navigation coordination
    private val _navigateToAnotherItem = MutableSharedFlow<String>()

    // Concern 6: Undo action management
    fun undoLastAction(undoableAction: UndoableAction) { ... }
}
```

=== Data Version Transparency (DVT)

The DVT theorem requires that data structures and configuration values can evolve without
causing ripple effects throughout the codebase.
Instead of hardcoding values directly in components,
configuration should be centralized in dedicated modules.

*In Android applications:* DVT violations often appear as:
- Hardcoded API endpoints, collection names, or client IDs scattered across classes
- Business rule constants (timeouts, thresholds, limits) embedded in logic
- Enum-to-string mappings duplicated in multiple locations
- Feature flags hardcoded rather than remotely configurable

*Key insight:* If adding a new admin user, changing a database collection name, or adjusting
a business rule threshold requires editing multiple files, DVT is violated.

*Violation example:*
```kotlin
// TODO find an example in my app
```

=== Action Version Transparency (AVT)

The AVT theorem ensures that actions (operations, functions, algorithms)
can evolve independently of their consumers.
When business logic is embedded directly in UI components,
changes to the logic require modifying presentation code.

*In Android applications:* AVT violations manifest as:
- Business validation logic embedded in ViewModels or Composables
- Data transformation algorithms duplicated across repositories and ViewModels
- Complex parsing logic mixed with UI state management
- Navigation decisions embedded in business rule processing

*Key insight:* Use Cases (Interactors) should encapsulate business operations,
allowing the operation to evolve without touching UI code.

=== Separation of States (SoS)

The SoS theorem requires that state changes are isolated to prevent unintended side effects.
When multiple unrelated pieces of state are managed together,
changes to one state can inadvertently affect others.

*In Android applications:* SoS violations manifest as:
- Monolithic UI state classes combining loading, error, data, and feature flags
- Shared mutable state accessed by multiple ViewModels
- State objects where changing one field triggers unnecessary recompositions
- Combined states that make it difficult to test individual features

*Key insight:* State should be granular enough that a change to one concern
does not trigger updates to unrelated UI components.

*Violation example:*
```kotlin
// TODO find an example in my app
```

== Application to Android Development

While NS Theory was originally developed for enterprise Java systems,
its principles translate directly to Kotlin Android applications which share Java roots:

#figure(
  table(
    columns: (auto, auto, auto),
    inset: 8pt,
    align: left,
    [*NS Theorem*], [*Android Pattern*], [*Refactoring Example*],
    [SoC], [Use Cases], [Extract business logic from ViewModels to dedicated Use Case classes],
    [DVT], [Config modules], [Centralize collection names, client IDs, and thresholds in config objects],
    [AVT], [Strategy pattern], [Allow algorithms to vary independently via interfaces],
    [SoS], [Granular states], [Split combined UI states into focused state classes],
  ),
  caption: [NS Theorems mapped to Android patterns]
)

== Benefits of NS-Compliant Architecture

Systems that follow NS Theory principles exhibit:

1. *Linear maintenance scalability:*
   The effort to implement a change is proportional to the change's inherent complexity, not the system's size
2. *Improved testability:*
   Focused modules can be unit tested in isolation without complex mocking
3. *Reduced fear of change:*
   Developers can confidently make modifications knowing the impact is contained
4. *Better code comprehension:*
   Each module has a single, clear purpose

The following sections demonstrate how these principles were applied to identify violations
in the current system and systematically resolve them through targeted refactoring.
