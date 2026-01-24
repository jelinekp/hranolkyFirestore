= Case Study 2: React Web Application Analysis

This section analyzes the original codebase structure of the React web application and identifies specific violations of Normalized Systems (NS) Theory. The analysis serves as the foundation for the refactoring work documented in the subsequent section.

== System Architecture

The hranolky-react application is a single-page React application with the following architecture:

#table(
  columns: (auto, auto),
  inset: 8pt,
  align: left,
  [*Layer*], [*Technology*],
  [Frontend], [React 19, TypeScript, Vite],
  [UI Framework], [Tailwind CSS, MUI, Actify],
  [State], [React Context, Local State],
  [Data], [Firebase Firestore],
  [Charts], [Recharts],
)

The application manages warehouse inventory for two product types: *Hranolky* (beams) and *Sparovky* (jointers), allowing filtering, sorting, and exporting of slot data.

#figure(
  image("../media/web_app.png", width: 100%),
  caption: [Screenshot of the hranolky-react application main screen],
) <fig:web_app>

== Separation of Concerns (SoC) Violations

According to NS Theory, each software element should address exactly one concern. When a component mixes multiple responsibilities, changes to one area require modifications to unrelated code, creating combinatorial effects that hinder evolvability.

=== Filters.tsx (321 lines)

This component exhibited the most severe SoC violation, combining four distinct concerns:

1. *Filter chip UI rendering* - Managing chip selection states
2. *Export dialog modal* - Complete modal with buttons and styling
3. *Export progress tracking* - State management for progress bar
4. *CSV generation coordination* - Orchestrating the export process

```typescript
// Original: Export dialog embedded in filter component
{showExportDialog && (
  <div className="fixed inset-0 ...">
    <div className="bg-[var(--color-bg-01)] ...">
      <h3>Exportovat historii stavů</h3>
      <button onClick={handleExport}>...</button>
      <button onClick={handleCopyToClipboard}>...</button>
    </div>
  </div>
)}
```

*NS Impact:* Any change to the export dialog (e.g., adding a new export format) requires modifying the entire Filters component, risking unintended side effects on filter functionality.

=== SlotsTable.tsx (179 lines)

This component mixed presentation logic with complex business logic:

```typescript
// Original: 50+ lines of inline sorting
{warehouseSlots.sort((a, b) => {
  if (sortingBy === SortingBy.quality) {
    return b.quality.localeCompare(a.quality);
  } else if (sortingBy === SortingBy.thickness) {
    return (a.thickness ?? 0) - (b.thickness ?? 0);
  }
  // ... more sorting cases
}).flatMap(slot => <TableRow />)}
```

*NS Impact:* Sorting algorithm changes require modifying presentation code, and the sorting logic cannot be unit tested in isolation.

=== VolumeInTimeChart.tsx (482 lines)

The chart component combined multiple concerns in a single file:

1. *Animation state* - `pulseOpacity`, `goofyOffsets` for loading effects
2. *Loading coordination* - `manualLoadRequested`, `shouldFetchData` logic
3. *Expanded modal state* - ESC key handling, backdrop, body scroll lock
4. *Overlay rendering* - "No matches" and "Manual load required" overlays
5. *Data transformation* - Y-axis calculation, inventory week detection

```typescript
// Original: 6 useState hooks mixing different concerns
const [pulseOpacity, setPulseOpacity] = useState(1);        // Animation
const [displayData, setDisplayData] = useState(...);        // Data
const [goofyOffsets, setGoofyOffsets] = useState([]);       // Animation
const [manualLoadRequested, setManualLoadRequested] = useState(false); // UI
const [isExpanded, setIsExpanded] = useState(false);        // Modal
```

*NS Impact:* The component is difficult to test, understand, and modify. Animation changes risk breaking data logic.

=== WarehouseSlotClass (187 lines)

Data model class mixed with display formatting:

```typescript
// Original: 40-line switch statement in data class
private getFullQualityName(quality: string): string {
  switch (quality) {
    case "DUB-A|A": return "DUB A/A";
    case "DUB-B|B": return "DUB B/B";
    // ... 15+ more cases
  }
}
```

*NS Impact:* Adding new quality types requires modifying the core data model, which should remain stable.

=== AdminPanel.tsx (240 lines)

The admin panel combined multiple responsibilities:

1. *Access control UI* - Access denied screen with navigation
2. *Device table rows* - Inline rendering of 50+ lines per row
3. *Edit state management* - Tracking which device is being edited

*NS Impact:* Changes to the access denied message require touching the entire admin component.

== Data Version Transparency (DVT) Violations

DVT requires that configuration and data structures can evolve without causing ripple effects across the codebase. Hardcoded values scattered throughout components violate this principle.

=== Hardcoded Admin Emails

```typescript
// Original: In WarehouseScreen.tsx
{['abc@xyz.com', 'def@xyz.com']
  .includes(user.email || '') && (
    <button onClick={() => navigate('/admin')}>Admin</button>
)}
```

*NS Impact:* Adding a new admin requires finding and modifying multiple files.

=== Hardcoded Collection Names

```typescript
// Repeated pattern in 5+ files:
const collectionName = slotType === SlotType.Beam ? 'Hranolky' : 'Sparovky';
```

*NS Impact:* Adding a new slot type requires changes across many files.

== Separation of States (SoS) Violations

SoS requires that state changes are isolated to prevent unintended side effects. When unrelated states are managed together, changes to one state can inadvertently affect others.

=== ContentLayoutContainer.tsx (111 lines)

Mixed filter state with sorting state and inline filtering logic:

```typescript
// Original: Multiple unrelated states combined
const [sortingBy, setSortingBy] = useState(SortingBy.none);     // Sorting
const [sortingOrder, setSortingOrder] = useState(SortingOrder.desc); // Sorting
const [activeFilters, setActiveFilters] = useState(SlotFiltersClass.EMPTY); // Filtering

// Plus: Inline filter logic (20 lines)
const filteredSlots = warehouseSlots.filter((slot) => {
  const matchesQuality = activeFilters.qualityFilters.size === 0 || ...;
  // ... more filter conditions
});
```

*NS Impact:* Filter logic cannot be tested independently. Adding new filter types requires modifying the container component.

=== WarehouseScreen.tsx (117 lines)

Combined multiple loading states inline:

```typescript
// Original: Three loading states combined
const loading = authLoading || slotsLoading || devicesLoading;
```

*NS Impact:* No separation between "authentication in progress" and "data loading" states for UI feedback.

== Evolvability Violations (DRY)

Significant DRY principle violations hinder the codebase's ability to evolve:

#table(
  columns: (auto, auto, auto),
  inset: 8pt,
  align: left,
  [*Duplicated Code*], [*Files Affected*], [*Impact*],
  [`VolumeDataPoint` interface], [4 files], [Type inconsistency risk],
  [`getWeekNumber()` function], [3 files], [Bug duplication risk],
  [Collection path logic], [5+ files], [Maintenance burden],
  [`WarehouseSlotClass`], [common/ & functions/], [Quality mapping divergence],
)

=== Week Calculation Duplication

```typescript
// Duplicated in multiple files:
function getWeekNumber(date: Date): { year: number; week: number } {
  const d = new Date(Date.UTC(date.getFullYear()...));
  // ... 15 lines of week calculation logic
}
```

*NS Impact:* A bug fix in one location must be manually propagated to all copies.

=== Collection Path Duplication

```typescript
// Repeated pattern in 5+ files:
const collectionSegments = slotType === SlotType.Beam
  ? ['WeeklyReports', 'Hranolky', 'WeeklyData']
  : ['WeeklyReports', 'Sparovky', 'WeeklyData'];
```

=== Loading Overlay Duplication (DRY)

```typescript
// App.tsx - Same loading overlay duplicated:
{loading && (
  <div className="fixed inset-0 bg-[var(--color-bg-05)]/80 ...">
    <div className="w-12 h-12 border-4 ... rounded-full animate-spin" />
    <span>Přihlašování...</span>  // or "Načítání..."
  </div>
)}
```

*NS Impact:* Loading overlay styling changes require editing two places. Risk of visual inconsistencies.

== Action Version Transparency (AVT) Violations

AVT requires that operations (actions) can evolve independently. Mutable class patterns violate this principle by combining state and operations.

=== SlotFiltersClass - Mutable Class Pattern

```typescript
// Original: Class with mutable state + operations mixed
export class SlotFiltersClass implements SlotFilters {
  typeFilters: Set<SlotType>
  qualityFilters: Set<string>
  // ...

  isEmpty(): boolean { ... }
  hasQualityFilters(): boolean { ... }
}
```

*NS Impact:* Testing filter logic requires instantiating the class. Operations cannot be composed or reused independently.

== Metrics Summary

#table(
  columns: (auto, auto, auto),
  inset: 8pt,
  align: left,
  [*File*], [*Lines*], [*Violations*],
  [`Filters.tsx`], [321], [SoC: UI + Dialog + Export],
  [`SlotsTable.tsx`], [179], [SoC: Presentation + Sorting],
  [`VolumeInTimeChart.tsx`], [482], [SoC: Animation + Data + UI + Modal; DVT: Magic numbers],
  [`ContentLayoutContainer.tsx`], [111], [SoS: Mixed filter/sort state],
  [`WarehouseSlotClass`], [187], [SoC: Data + Display],
  [`WarehouseScreen.tsx`], [117], [DVT: Hardcoded config; SoS: Mixed loading],
  [`AdminPanel.tsx`], [240], [SoC: Access + Table + Edit state],
  [`App.tsx`], [76], [DRY: Duplicated loading overlay],
  [`SlotFilter.ts`], [115], [AVT: Mutable class pattern],
)

The identified violations represent approximately *1,800 lines* of code that would benefit from NS Theory-guided refactoring.
