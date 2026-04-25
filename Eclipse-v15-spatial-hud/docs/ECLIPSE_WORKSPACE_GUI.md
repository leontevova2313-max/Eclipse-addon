# Eclipse Workspace GUI

This pass introduces the first actual Eclipse-owned GUI shell.

## Entry points
- `Right Shift` toggles the workspace screen.
- `Eclipse` button on the title screen.
- `Eclipse` button in the pause menu.

## Goals of this pass
- stop depending on Meteor's click GUI for the main Eclipse experience
- provide a real Eclipse screen with its own sections and layout
- keep using the existing addon/runtime while the standalone shell grows

## Current layout
- left rail: Eclipse sections
- center workspace: searchable module list
- right detail panel: module description, state, visible settings

## Current supported setting interactions
- bool settings: toggle
- enum settings: cycle
- int settings: +/-
- double settings: +/-
- other setting types: visible as read-only text for now

## Known limits of this pass
- not a fully detached standalone runtime yet
- not a full HUD editor yet
- not a full inspector panel yet
- still reads actual Meteor module data underneath

The key difference is that the user-facing shell is now Eclipse-owned, not Meteor-owned.
