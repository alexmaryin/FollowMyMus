## Why

Users who accidentally swipe-to-dismiss a release on the New Releases page have no way to recover it — the row is immediately hidden by the `WHERE state != 'DISMISSED'` query filter and can never be re-surfaced. This is a permanent data-loss problem for a user gesture that is intentionally easy to trigger. The page's top bar currently has no trailing actions, while the Artists and Favorites pages each provide fast-action icons in the same slot.

## What Changes

- **Restore All Dismissed Releases** — Action that resets every `DISMISSED` row in `new_releases` back to `UNSEEN`, re-surfacing all hidden releases.
- **Undo Last Dismissal** — Action that reverts the most recent `markDismissed` call by restoring the last dismissed release to `UNSEEN`.
- **Dismissal History** — The dismissal stack is stored as a serializable sub-data class of `NewReleasesListState`, surviving config changes via `saveableMutableValue`.
- **Title slot leading icon** — A `Res.drawable.tune` leading icon in the title slot (matching the `ReleasesListTitle` pattern) opens an inline `AnimatedVisibility` panel with two action items: "Undo Last" and "Restore All". The popup uses the existing `ButtonGroup`+`clickableItem` pattern from `TitleActions`, referencing the pre-existing `undo.xml` and `revert.xml` icons.

## Capabilities

### New Capabilities

*(none — all changes modify the existing new-releases capability)*

### Modified Capabilities

- `new-releases`: The existing new-releases spec gains two new requirements — "Restore All Dismissals" and "Undo Last Dismissal" — plus the associated UI and in-memory history tracking.

## Impact

- **UI**: `NewReleasesPanelSlots.titleContent` gains a leading `tune` icon button that toggles an inline actions popup with restore-all and undo-last options.
- **Domain**: `NewReleasesListAction` gains two new variants (`RestoreAllDismissed`, `UndoLastDismissal`). `NewReleasesListState` gains a `dismissHistory` sub-data class holding the stack of dismissed IDs.
- **Repository**: `NewReleasesRepository` gains `suspend fun markUnseen(releaseId: String)` and `suspend fun restoreAllDismissed()`. `NewReleasesDao` gains corresponding `@Query` methods.
- **Data layer**: Dismissed IDs stack is stored as a serializable `DismissHistory` data class inside `NewReleasesListState`, persisted across config changes via `saveableMutableValue`.
