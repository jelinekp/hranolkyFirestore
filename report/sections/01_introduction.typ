= Executive Summary

This report documents the refactoring of the *hranolky-firestore* warehouse management application
according to Normalized Systems (NS) Theory principles.
The application, built with Kotlin and Firebase, runs on Zebra Android terminals and
 manages inventory data for a woodworking company.

== Motivation

The original codebase exhibited several maintainability issues:
// TODO: expand

== Approach

Using NS Theory's four core theorems—Separation of Concerns (SoC), Data Version Transparency (DVT), Action Version Transparency (AVT),
and Separation of States (SoS)—we systematically identified violations and refactored the codebase.

== Key Results

#table(
  columns: (auto, auto, auto),
  inset: 8pt,
  align: left,
  [*Phase*], [*Extracted Module*], [*Lines Saved*],
)

A comprehensive test suite was established as the foundation:

== Conclusion

The refactoring successfully improved the codebase's modularity and testability while maintaining
full backward compatibility.
All existing functionality remains intact, verified through manual testing and the new automated test suite.
