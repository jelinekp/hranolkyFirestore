## Plan: Add Undo Snackbar for Slot Actions ✅ COMPLETED

Implement an undo feature that shows a 10-second snackbar after adding/removing slot actions, allowing users to rollback the change. The approach uses Compose Material3's `SnackbarHost` with a custom duration and captures action metadata for reversal.

### Steps

1. ✅ **Add `undoSlotAction` method to [`SlotRepository`](app/src/main/java/eu/jelinek/hranolky/data/SlotRepository.kt)** - Added interface method for reversing a slot action by re-applying the opposite quantity change and deleting the action document from `SlotActions` subcollection. Also changed `addSlotAction` return type to `String` to return document ID.

2. ✅ **Implement `undoSlotAction` in [`SlotRepositoryImpl`](app/src/main/java/eu/jelinek/hranolky/data/SlotRepositoryImpl.kt)** - Used Firestore transaction to atomically: (1) decrement/increment quantity by reversing the original change, (2) delete the action document by its ID, (3) update `lastModified` timestamp.

3. ✅ **Extend [`SlotAction`](app/src/main/java/eu/jelinek/hranolky/model/SlotAction.kt) model** - Added nullable `documentId: String?` field to track the Firestore document ID for later deletion.

4. ✅ **Update `getSlotActions` in [`SlotRepositoryImpl`](app/src/main/java/eu/jelinek/hranolky/data/SlotRepositoryImpl.kt)** - Included document ID when mapping `SlotAction` objects from Firestore using `.copy(documentId = document.id)`.

5. ✅ **Add undo state and event in [`ManageItemViewModel`](app/src/main/java/eu/jelinek/hranolky/ui/manageitem/ManageItemViewModel.kt)** - Added `UndoableAction` data class, `_undoSnackbarEvent: MutableSharedFlow<UndoableAction>` to emit undo info after successful action, `_errorSnackbarEvent` for error messages, and `undoLastAction()` method.

6. ✅ **Add `SnackbarHost` to [`ManageItemScreen`](app/src/main/java/eu/jelinek/hranolky/ui/manageitem/ManageItemScreen.kt)** - Added `SnackbarHostState` to `Scaffold`, collect undo events with `LaunchedEffect`, show snackbar with "Vrátit zpět" action label and `SnackbarDuration.Long` (~10 seconds).

7. ✅ **Update [`AddSlotActionUseCase`](app/src/main/java/eu/jelinek/hranolky/domain/AddSlotActionUseCase.kt)** - Added `AddSlotActionResult` data class and changed return type to `Result<AddSlotActionResult>` to pass document ID and quantity change back for undo.

### Implementation Decisions

1. **INVENTORY_CHECK is reversible** - Per user request, all action types including INVENTORY_CHECK can be undone.

2. **Network failure handling** - Shows error snackbar with message "Nepodařilo se vrátit změnu: {error}" when undo fails.

3. **Document ID tracked at creation time** - `addSlotAction` now returns the document ID immediately after creation, enabling immediate undo without re-fetching.

