// ============================================================================
// Centralized Color Definitions and Styling Functions
//
// Usage: #import "../styles.typ": *
//
// This file provides:
// - Consistent color scheme across the entire document
// - Reusable styling functions for semantic text coloring
// - Centralized maintenance (change colors in one place)
//
// Color Scheme:
//   Green  - Additions, increases, positive changes
//   Red    - Deletions, decreases, removals
//   Blue   - Modifications, neutral metrics, cross-references
//   Gray   - Unchanged values, zero deltas
//
// Functions:
//   #txt-add[content]     - Bold green text for additions/increases
//   #txt-del[content]     - Bold red text for deletions/decreases
//   #txt-mod[content]     - Blue text for modifications/neutral info
//   #txt-neutral[content] - Gray text for zero/unchanged values
//
// Example:
//   [Lines added], [#txt-add[+7,362]]
//   [Lines deleted], [#txt-del[-231]]
//   [Files changed], [#txt-mod[62 files]]
// ============================================================================

// Color Definitions
#let color-addition = rgb("#2E7D32")      // Green for additions/increases
#let color-deletion = rgb("#C62828")      // Red for deletions/decreases
#let color-modification = rgb("#1976D2")  // Blue for modifications/neutral
#let color-neutral = rgb("#757575")       // Gray for zero/unchanged values

// Table header backgrounds
#let color-android-header = rgb("#E8F5E9")   // Light green
#let color-web-header = rgb("#E3F2FD")       // Light blue
#let color-breakdown-header = rgb("#FFF9C4") // Light yellow
#let color-total-row = rgb("#F5F5F5")        // Light gray

// Styling Functions - Reusable text formatting
#let txt-add(content) = text(fill: color-addition)[#content]
#let txt-del(content) = text(fill: color-deletion)[#content]
#let txt-mod(content) = text(fill: color-modification)[#content]
#let txt-neutral(content) = text(fill: color-neutral)[#content]
