## 1. Data Layer — DAO + Repository

- [x] 1.1 Add `markUnseen(releaseId: String): suspend` and `restoreAllDismissed(): suspend` to `NewReleasesDao` — SQL: `UPDATE new_releases SET state = 'UNSEEN' WHERE id = :releaseId` and `UPDATE new_releases SET state = 'UNSEEN' WHERE state = 'DISMISSED'`
- [x] 1.2 Add `markUnseen(releaseId: String): suspend` and `restoreAllDismissed(): suspend` to `NewReleasesRepository` domain interface
- [x] 1.3 Implement both methods in `ApiNewReleasesRepository` — delegate to the DAO

## 2. Domain Layer — DismissalHistory + Actions

- [x] 2.1 Add `@Serializable` data class `DismissHistory(val dismissedIds: List<String>)` as a field in `NewReleasesListState` (default `DismissHistory()`), initialize `dismissedIds` to empty list
- [x] 2.2 Add `RestoreAllDismissed` and `UndoLastDismissal` variants to `NewReleasesListAction` sealed interface
- [x] 2.3 Wire the two new actions in `NewReleasesList.invoke()`: `RestoreAllDismissed` → update state `dismissHistory.dismissedIds = emptyList()` + call `repository.restoreAllDismissed()`; `UndoLastDismissal` → pop last ID from state `dismissHistory.dismissedIds` + call `repository.markUnseen(id)`
- [x] 2.4 Wire existing `Dismiss` action to append the release ID to `state.dismissHistory.dismissedIds` before calling `repository.markDismissed()`

## 3. UI Layer — Title Slot Leading Icon + Popup

- [x] 3.1 Create `NewReleasesFastActions` composable in `newReleases/ui/list/` (or alongside `NewReleasesList.kt`) — a `Row` with a leading `Res.drawable.tune` `IconButton` that toggles an `AnimatedVisibility` block containing two `clickableItem`s: "Undo Last" (`Res.drawable.undo`) and "Restore All" (`Res.drawable.revert`)
- [x] 3.2 Update `NewReleasesPanelSlots.titleContent` to include the `NewReleasesFastActions` composable before the "Releases" title text in the `Column`
- [x] 3.3 Wire the dismiss history availability from `NewReleasesListState.dismissHistory.dismissedIds` to the "Undo Last" item's enabled/disabled visual state

## 4. Tests

- [x] 4.1 Update `FakeNewReleasesDao` with `markUnseen` and `restoreAllDismissed` recording methods
- [x] 4.2 Write unit tests for `NewReleasesDao.markUnseen` and `restoreAllDismissed` (via the fake)
- [x] 4.3 Write unit tests for `NewReleasesListState.DismissHistory` behavior (append, pop, empty, serialization)
- [x] 4.4 Write unit tests for `NewReleasesList` action handling: `RestoreAllDismissed`, `UndoLastDismissal`, and `Dismiss` + state interaction
