## ADDED Requirements

### Requirement: User can restore all dismissed releases

The system MUST provide a "Restore All" action in the New Releases page top bar that resets every `DISMISSED` row in the `new_releases` table to `UNSEEN`. After the action completes, the list MUST re-query the Room PagingSource so that all previously-dismissed rows re-appear with the UNSEEN visual treatment (blue dot, bold title).

The action MUST be triggered from a `Res.drawable.tune` leading `IconButton` in the `NewReleasesPanelSlots.titleContent` row. The tune button MUST toggle an `AnimatedVisibility` popup (matching the `ReleasesListTitle` / `TitleActions` pattern) containing two `clickableItem`s: "Undo Last" and "Restore All." The "Restore All" item MUST always be clickable; its effect is a no-op when no releases have been dismissed.

#### Scenario: User restores all dismissed releases

- **WHEN** the user has dismissed one or more releases
- **AND** the user taps the "Restore All" icon in the top bar
- **THEN** every row with `state = 'DISMISSED'` in the `new_releases` table is updated to `state = 'UNSEEN'`
- **AND** the PagingSource is invalidated so the list re-queries
- **AND** all previously-dismissed rows re-appear with the UNSEEN dot indicator and bold title

#### Scenario: No dismissed releases — Restore All is a no-op

- **WHEN** no releases have been dismissed during the current session
- **AND** the user taps the "Restore All" icon
- **THEN** the database is not modified
- **AND** the UI does not change

### Requirement: User can undo the last dismissal

The system MUST track the chronological order of dismissals during a session using an in-memory stack (never persisted to disk). The system MUST provide an "Undo Last" action in the New Releases page top bar that reverts the most recent `markDismissed` call by updating the dismissed release's state back to `UNSEEN`.

The action MUST be triggered from the same `AnimatedVisibility` popup as "Restore All," using `Res.drawable.undo`. The `clickableItem` MUST be visually dimmed and non-interactive when `dismissHistory.dismissedIds` is empty.

#### Scenario: User undoes the last dismissal

- **WHEN** the user has dismissed at least one release
- **AND** the user taps the "Undo Last" icon
- **THEN** the most recently dismissed release is updated from `state = 'DISMISSED'` to `state = 'UNSEEN'`
- **AND** the PagingSource is invalidated so that release re-appears
- **AND** the dismissal stack size decreases by one
- **AND** if the stack is now empty, the "Undo Last" icon becomes visually inactive

#### Scenario: No dismissals — Undo Last is a no-op

- **WHEN** the dismissal stack is empty
- **AND** the user taps the "Undo Last" icon
- **THEN** nothing happens (the action handler checks for a non-empty stack before calling the repository)

#### Scenario: Multiple undos restore in reverse order

- **WHEN** the user dismisses releases A, B, C in that order
- **AND** the user taps "Undo Last" twice
- **THEN** release C is restored (undo #1)
- **AND** release B is restored (undo #2)
- **AND** release A remains dismissed

### Requirement: Dismissal history is session-only

The system MUST store the dismissal history as a `@Serializable` sub-data class of `NewReleasesListState`, managed by `saveableMutableValue`. The stack MUST be initialized empty. On process death the stack is lost; on config changes (rotation, theme switch) it survives.

#### Scenario: User navigates away and back

- **WHEN** the user dismisses a release
- **AND** the user navigates to another tab (e.g., Artists)
- **AND** the user returns to the New Releases tab
- **THEN** the dismissal stack is preserved (state survives via `saveableMutableValue`)
- **AND** undo actions remain available for the previous session's dismissals

#### Scenario: App process is killed

- **WHEN** the user dismisses releases
- **AND** the Android OS kills the app process
- **AND** the user re-opens the app
- **THEN** the dismissal stack is empty (process death resets the serialized state)
- **AND** previously dismissed releases remain dismissed

### Requirement: Fast-action popup uses leading tune icon in the title slot

The system MUST add a leading `Res.drawable.tune` `IconButton` in `NewReleasesPanelSlots.titleContent`. The icon button MUST toggle an `AnimatedVisibility` block (following the `ReleasesListTitle` pattern) that renders two `clickableItem`s: "Undo Last" (`Res.drawable.undo`) and "Restore All" (`Res.drawable.revert`). The popup MUST NOT appear when the media details panel is active (the host slot handles panel switching).

#### Scenario: Main list panel is active

- **WHEN** the New Releases page shows the main list
- **THEN** the title row shows the "Releases" label, the "Last synced" timestamp, and a `tune` leading icon
- **AND** tapping the `tune` icon opens an `AnimatedVisibility` popup with "Undo Last" and "Restore All"
- **AND** the "Undo Last" action is dimmed when `dismissHistory.dismissedIds` is empty

#### Scenario: Media details panel is active

- **WHEN** the user opens a release and the media details panel is shown
- **THEN** the `titleContent` is delegated to the media panel (no fast-action popup)
