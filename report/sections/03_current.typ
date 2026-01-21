= Current System Analysis

This section analyzes the original codebase structure and identifies specific violations of Normalized Systems (NS) Theory.
The analysis serves as the foundation for the refactoring work documented in the next section.

== System Architecture

The hranolky-firestore application is a native Kotlin Android application with the following architecture:

#table(
  columns: (auto, auto),
  inset: 8pt,
  align: left,
  [*Layer*], [*Technology*],
  [Frontend], [Kotlin, Jetpack Compose],
  [UI Framework], [Jetpack Compose],
  [State], [],
  [Data], [Firebase Firestore],
)

The application manages warehouse inventory for two product types: *Hranolky* (beams) and *Sparovky* (jointers), allowing filtering, sorting, and exporting of slot data.

== Separation of Concerns (SoC) Violations

According to NS Theory, each software element should address exactly one concern. When a component mixes multiple responsibilities, changes to one area require modifications to unrelated code, creating combinatorial effects that hinder evolvability.

// TODO: analyze and document SoC violations in the Kotlin codebase

== Separation of States (SoS) Violations

// TODO: analyze and document SoS violations in the Kotlin codebase

== Data Version Transparency (DVT) Violations

// TODO: analyze and document DVT violations in the Kotlin codebase

== Action Version Transparency (AVT) Violations

// TODO: analyze and document AVT violations in the Kotlin codebase

== Evolvability Violations (DRY)

Significant DRY principle violations hinder the codebase's ability to evolve:

// TODO: analyze and document DRY violations in the Kotlin codebase