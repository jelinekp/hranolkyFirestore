= Refactored System

This section documents the systematic changes made to address the identified NS Theory violations.
Each refactoring follows the principle of extracting focused,
testable modules that can evolve independently.

== Test-Driven Approach

Before refactoring, I established a comprehensive test suite as a safety net.
According to @mannaert2016normalized, tests serve as executable specifications
that ensure behavior preservation during restructuring.

#table(
  columns: (auto, auto, auto),
  inset: 8pt,
  align: left,
  [*Test Category*], [*Files*], [*Tests*],
  // TODO update
  [Unit Tests], [], [],
  [UI components Tests],
  [],
  [],
  [Integration Tests], [], [],
  [*Total*], [], [],
)

== Separation of Concerns (SoC) Resolution
// TODO
== Separation of States (SoS) Resolution
// TODO
== Data Version Transparency (DVT) Resolution
// TODO
== Action Version Transparency (AVT) Resolution
// TODO
== DRY Resolution for Evolvability
// TODO
== Summary of Changes
// TODO