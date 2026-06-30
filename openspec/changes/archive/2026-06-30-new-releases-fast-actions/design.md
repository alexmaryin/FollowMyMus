## Context

The New Releases page (tab `PagerConfig.Releases`) shows release-groups from the user's favorite artists with a three-state lifecycle: `UNSEEN` → `SEEN` → `DISMISSED`. Swiping a row calls `repository.markDismissed(id)` which sets `state = 'DISMISSED'` in the Room `new_releases` table, and the `WHERE state != 'DISMISSED'` query filter immediately hides it. There is no recovery mechanism.

The page's top bar already has `ScaffoldSlots` via `NewReleasesHostSlots` (title delegation, leading back icon) and `NewReleasesPanelSlots` (release title + last-sync timestamp), but the `trailingIcon` slot is never overridden — it falls through to `DefaultScaffoldSlots.trailingIcon` which is `empty`.

The shared `ReleasesListTitle` component embeds a leading `more_vert` icon that opens an inline `AnimatedVisibility` panel with `TitleActions` (a `ButtonGroup` of `clickableItem`s). This is the pattern to follow: a leading icon in the title slot row that toggles an action popup.

## Goals / Non-Goals

**Goals:**
- Add "Restore All" and "Undo Last" actions triggered from a `tune` leading icon in the title slot
- Track dismissal history in a serializable sub-data class of `NewReleasesListState`, surviving config changes
- Follow the existing `ReleasesListTitle` + `TitleActions` pattern (leading icon → `AnimatedVisibility` popup)

**Non-Goals:**
- Persisting the dismissal history to Room or DataStore (the state class survives saved-instance-state across config changes, but not process death)
- Animating individual rows returning to the list (the PagingSource re-query naturally adds them back)
- Adding swipe-to-dismiss to the shared `ArtistReleasesList` (only the New Releases tab gets fast actions)
- Showing a Snackbar with an "Undo" action (the popup actions in the title slot are sufficient)

## Decisions

### Decision: Dismissal stack as serializable sub-data class of NewReleasesListState

**Chosen:** A `@Serializable` data class `DismissHistory` (with a `val dismissedIds: List<String>`) embedded as a field in `NewReleasesListState`.

**Alternatives considered:**
- **Retained InstanceKeeper holder** — unnecessary complication; the stack is a simple `List<String>` that fits naturally in the existing `saveableMutableValue` / `@Serializable` state pattern.
- **DataStore-backed history** — undo is explicitly session-only; durable storage is overkill.

**Rationale:** `NewReleasesListState` is already `@Serializable` and managed by `saveableMutableValue`. Adding a sub-data class with a list of strings is minimal overhead and automatically survives config changes (rotation, theme switch). The stack is discarded on process death, which is acceptable.

### Decision: Two new Repository methods

- `suspend fun markUnseen(releaseId: String)` → DAO maps to `UPDATE new_releases SET state = 'UNSEEN' WHERE id = :releaseId`
- `suspend fun restoreAllDismissed()` → DAO maps to `UPDATE new_releases SET state = 'UNSEEN' WHERE state = 'DISMISSED'`

**Rationale:** Minimal SQL surface. `restoreAllDismissed()` is a single atomic UPDATE that avoids looping. `markUnseen` does the same for one ID.

### Decision: Leading tune icon in NewReleasesPanelSlots.titleContent with AnimatedVisibility popup

**Chosen:** `NewReleasesPanelSlots.titleContent` is updated to include a leading `Res.drawable.tune` `IconButton` that toggles an `AnimatedVisibility` block containing two `clickableItem` buttons (undo + restore), matching the `ReleasesListTitle` + `TitleActions` pattern.

**Alternatives considered:**
- **Trailing icons in `NewReleasesHostSlots.trailingIcon`** — would work but deviates from the project's more common pattern of embedding actions in the title row itself (as `ReleasesListTitle` and `FavoritesListTitle` do).
- **Separate TrailingIcon composable** — adds an extra slot when the title slot already includes the release name and last-sync timestamp; grouping the actions in the same row is cleaner.

**Rationale:** The title row already contains "Releases" + "Last synced: ..." text. Adding a leading `tune` icon before those elements keeps the actions visually associated with the page title rather than separated into a different `TopAppBar` slot. The `AnimatedVisibility` popup reuses the same `ButtonGroup` + `clickableItem` API from `TitleActions`.

### Decision: Pre-existing drawables — undo.xml and revert.xml

**Chosen:** The icon files `undo.xml` and `revert.xml` already exist in `composeResources/drawable/`. The popup references `Res.drawable.undo` for "Undo Last" and `Res.drawable.revert` for "Restore All".

### Decision: "Undo Last" action is conditionally enabled

**Chosen:** The "Undo Last" `clickableItem` is rendered inside the popup but visually dimmed and non-interactive when `dismissHistory.dismissedIds` is empty.

**Alternatives considered:**
- Hiding the action entirely from the popup — would cause the popup size to change, creating a jarring visual shift.
- Always enabled with a no-op — violates the principle of least surprise.

**Rationale:** Disabling the action communicates to the user that undo exists and will become available after they dismiss a release.

## Risks / Trade-offs

- **[Session-only undo]** The dismissal history is lost on process death (survives config changes via state serialization but not OS kill). **Mitigation:** The state survives tab switches and rotations within the same process; the user can switch to Artists and back and still undo. Only a full process kill resets the stack.
- **[Restore All is destructive to SEEN state]** `restoreAllDismissed()` sets ALL dismissed rows to `UNSEEN`, including those that were previously `SEEN` before being dismissed. This means they reappear with the blue dot and bold title. **Mitigation:** This is the intended behavior — "restore all" is meant to bring everything back for review. An alternative (restoring only the original UNSEEN state) would require storing the pre-dismissal state per row, adding complexity with marginal benefit.
- **[PagingSource re-query delay]** After `restoreAllDismissed()`, the PagingSource's `INVALIDATE` triggers a re-query, but there may be a brief UI flash as items re-appear. **Mitigation:** This is inherent to Room + Paging; the delay is typically sub-second and consistent with the existing list behavior.
