= Case Study 2: Web App Refactoring

This section documents the systematic changes made to address the identified NS Theory violations in the React web application.

== Architecture Comparison

The following diagrams illustrate the structural changes from the original architecture to the refactored NS-compliant design.

=== Before Refactoring

The original architecture featured a *flat component structure* with mixed concerns:

#figure(
  table(
    columns: (1fr, 2fr),
    inset: 10pt,
    align: left,
    fill: (col, row) => if row == 0 { rgb("#FFB3B3") } else { white },
    [*Component*], [*Mixed Responsibilities*],
    [`Filters.tsx`], [Filter UI + Export Dialog + Export Logic + Progress Bar],
    [`VolumeInTimeChart.tsx`], [Chart + Animation + Data Transform + Modal + Axis],
    [`SlotsTable.tsx`], [Table Rendering + Sorting Logic + Quality Mapping],
    [`ContentLayoutContainer`], [Layout + Filter State + Sorting State],
    [`AdminPanel.tsx`], [Access Control + Device Table + Edit State],
  ),
  caption: [Original component structure with mixed concerns],
)

#figure(
  pad(
    x: -1cm,
    image("../diagrams/svg/web_architecture_before.svg", width: calc.abs(110%)),
  ),
  caption: [Directory structure before refactoring — 35 source files with significant coupling],
)

=== After Refactoring

The refactored architecture follows NS Theory principles with *modular, single-responsibility components*:

#figure(
  table(
    columns: (1fr, 2fr, 1fr),
    inset: 10pt,
    align: left,
    fill: (col, row) => if row == 0 { rgb("#B3FFB3") } else { white },
    [*Module*], [*Single Responsibility*], [*Principle*],
    [`ExportDialog.tsx`], [Export options modal UI only], [SoC],
    [`ChartOverlay.tsx`], [Chart overlay rendering only], [SoC],
    [`slotSorting.ts`], [Pure sorting functions only], [SoC],
    [`useSlotFiltering.ts`], [Filter state management only], [SoS],
    [`useChartLoadingState.ts`], [Loading state coordination only], [SoS],
    [`ErrorBoundary.tsx`], [Fault isolation wrapper], [SoS],
    [`appConfig.ts`], [Centralized configuration], [DVT],
    [`slotFilterUtils.ts`], [Pure filter operations], [AVT],
  ),
  caption: [Refactored modules with single responsibilities],
)

#figure(
  pad(
    x: -1cm,
    image("../diagrams/svg/web_architecture_after.svg", width: calc.abs(110%)),
  ),
  caption: [Directory structure after refactoring — 66 source files with clear boundaries],
)

=== Architectural Improvements

#table(
  columns: (auto, 1fr, 1fr),
  inset: 8pt,
  align: left,
  [*Metric*], [*Before*], [*After*],
  [Source app logic files], [35], [66 (+89%)],
  [Average component size], [~200 lines], [~60 lines],
  [Largest component], [482 lines], [~150 lines],
  [Test coverage], [0 tests], [196 tests],
  [Directory depth], [Flat], [Organized by concern],
)

The increase in file count reflects the extraction of focused modules from monolithic components.
Each module now has a single reason to change, enabling independent evolution as predicted by NS Theory.

== Test-Driven Approach

Before refactoring, I established a comprehensive test suite as a safety net.
According to @mannaert2016normalized, tests serve as executable specifications that ensure behavior preservation during restructuring.

#table(
  columns: (auto, auto, auto),
  inset: 8pt,
  align: left,
  [*Test Category*], [*Files*], [*Tests*],
  [Unit Tests],
  [`slotSorting.test.ts`, `slotFilterUtils.test.ts`, `weekUtils.test.ts`, `qualityMapping.test.ts`, `exportToCsv.test.ts`, `chartAxisUtils.test.ts`, `WarehouseSlot.test.ts`, `appSettings.test.ts`, `SlotFilter.test.ts`],
  [101],

  [Component Tests],
  [`SlotsTable.test.tsx`, `ExportDialog.test.tsx`, `VolumeInTimeChart.test.tsx`, `AccessDenied.test.tsx`, `ChartOverlay.test.tsx`, `ErrorBoundary.test.tsx`, `LoadingOverlay.test.tsx`, `AppSettingsCard.test.tsx`],
  [52],

  [Hook Tests], [`useSlotFiltering.test.ts`, `useChartLoadingState.test.ts`, `useAppSettings.test.ts`], [20],
  [Integration & E2E],
  [`FilteringFlow.test.tsx`, `AdminFlow.test.tsx`, `ExportFlow.test.tsx`, `warehouse.spec.ts`, `admin.spec.ts`],
  [23],

  [*Total*], [26 files], [*196 tests*],
)

== Data Version Transparency (DVT) Resolution

DVT requires that configuration values are isolated so they can evolve without ripple effects.
The solution centralizes all configurable values in a single module.

=== Configuration Module (`config/appConfig.ts`)

```typescript
// After: Centralized configuration
export const ADMIN_EMAILS = [
  'abc@xyz.com',
  'def@xyz.com',
] as const

export function isAdminUser(email: string | null): boolean {
  if (!email) return false
  return ADMIN_EMAILS.includes(email)
}

export const COLLECTION_NAMES = {
  [SlotType.Beam]: 'Hranolky',
  [SlotType.Jointer]: 'Sparovky',
} as const
```

*DVT Benefit:* Adding a new admin or slot type now requires a single-line change in one file.
All consuming code automatically receives the update.

== Separation of Concerns (SoC) Resolution

SoC requires each module to have a single, well-defined responsibility.
The following extractions eliminate combinatorial effects by isolating concerns.

=== Sorting Logic (`utils/slotSorting.ts`)

Extracted comparison logic as a pure utility function:

```typescript
// After: Pure, testable utility function
export function sortSlots(
  slots: WarehouseSlotClass[],
  sortingBy: SortingBy,
  sortingOrder: SortingOrder
): WarehouseSlotClass[] {
  const ascending = sortingOrder === SortingOrder.asc
  const comparator = getComparator(sortingBy, ascending)
  return [...slots].sort(comparator)
}
```

*SoC Benefit:* Sorting logic can now be unit tested in isolation.
Changes to the algorithm don't require touching UI code.

=== Export Dialog (`components/ExportDialog.tsx`)

Created a standalone, reusable dialog component:

```typescript
// After: Dedicated component with clear interface
interface ExportDialogProps {
  isOpen: boolean
  onClose: () => void
  onExportCsv: () => void
  onCopyToClipboard: () => void
  isExporting: boolean
  isCopying: boolean
  itemCount: number
}
```

*SoC Benefit:* The dialog can be reused in other contexts.
Export functionality changes don't affect filter logic.

=== Quality Mapping (`common/utils/qualityMapping.ts`)

Replaced procedural switch statement with data-driven approach:

```typescript
// After: Data structure instead of code
export const QUALITY_MAPPINGS: Record<string, string> = {
  'DUB-A|A': 'DUB A/A',
  'DUB-B|B': 'DUB B/B',
  'ZIR-ZIR': 'ZIRBE',
  // ... more mappings
}

export function getFullQualityName(code: string | null): string {
  if (!code) return ''
  return QUALITY_MAPPINGS[code] ?? code
}
```

Support for external configuration was added such that the configuration is now shared with the Android app.
If the configuration cannot be loaded, the app falls back to built-in defaults.
A new admin panel for editing shared app settings was also created.
Privileged users can now update quality mappings, dimension corrections,
inventory check intervals, etc at runtime without code changes or app releases.

*SoC Benefit:* Adding new quality types requires only data changes, not code changes.
The mappings are loaded from external configuration primarily.

=== Chart Components

Decomposed the 482-line VolumeInTimeChart into focused modules:

- `hooks/useChartAnimation.ts` - Pulsing loading animation logic
- `components/chart/ChartTooltip.tsx` - Tooltip rendering
- `components/chart/ChartXAxisTick.tsx` - Custom X-axis with year grouping
- `components/chart/ChartOverlay.tsx` - Reusable "No matches" and "Manual load" overlays
- `components/chart/ExpandedChartModal.tsx` - Fullscreen modal with ESC handling

*SoC Benefit:* Each component has a single responsibility. Animation can evolve independently from data logic.

=== Admin Components

Extracted focused components from AdminPanel:

- `components/admin/AccessDenied.tsx` - Reusable access denied screen
- `components/admin/DeviceRow.tsx` - Single table row with edit capabilities

*SoC Benefit:* The access denied screen can be reused across protected routes.

== Separation of States (SoS) Resolution

SoS requires that state changes are isolated to prevent unintended side effects.
Custom hooks encapsulate related state and logic.

=== Filter State (`hooks/useSlotFiltering.ts`)

Extracted all filtering concerns into a dedicated hook:

```typescript
// After: Encapsulated filter state and logic
export const useSlotFiltering = (slots: WarehouseSlotClass[]) => {
  const [activeFilters, setActiveFilters] = useState(SlotFiltersClass.EMPTY);

  const filteredSlots = useMemo(() => filterSlots(slots, activeFilters), [...]);
  const distinctValues = useMemo(() => getDistinctFilterValues(slots), [slots]);

  return {
    activeFilters,
    setActiveFilters,
    filteredSlots,
    volumeSum: calculateVolume(filteredSlots),
    distinctValues,
    hasActiveFilters: !activeFilters.isEmpty()
  };
};
```

*SoS Benefit:* Filter state is completely isolated.
The hook can be tested independently and reused in other contexts.

=== Chart Loading State (`hooks/useChartLoadingState.ts`)

Isolated manual load coordination logic:

```typescript
// After: Encapsulated loading state
export const useChartLoadingState = (hasActiveFilters: boolean, slotCount: number) => {
  const [manualLoadRequested, setManualLoadRequested] = useState(false);

  const shouldWaitForManualLoad = hasActiveFilters && slotCount > MANUAL_LOAD_THRESHOLD;
  const shouldFetchData = !shouldWaitForManualLoad || manualLoadRequested;

  return { manualLoadRequested, setManualLoadRequested, shouldFetchData };
};
```

*SoS Benefit:* Loading logic is now testable in isolation with clear, deterministic behavior.

== DRY Resolution for Evolvability

Created shared utility modules to eliminate code duplication:

=== Week Utilities (`common/utils/weekUtils.ts`)

Consolidated 10 week-related functions into a single source of truth:

```typescript
export function getWeekNumber(date: Date): WeekInfo { ... }
export function formatWeekId(year: number, week: number): string { ... }
export function parseWeekId(weekId: string): { year, week } | null { ... }
export function getCurrentWeekLabel(): string { ... }
```
