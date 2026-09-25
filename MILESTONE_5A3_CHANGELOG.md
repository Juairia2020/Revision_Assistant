# Milestone 5A-3 — First-Launch Feature Tour & UI Polish

## Added
- Nine-step first-launch feature tour for newly registered users.
- Reusable tour overlay hosted by the existing application shell.
- Next, Previous, Skip Tour, and Start Studying controls.
- Step progress indicator (`Step N of 9`).
- Sidebar target highlighting for the feature being explained.
- Replayable tour through **Take Feature Tour** in the existing shell.
- Short fade transitions for tour entry and step changes.
- Small authentication/shell CSS consistency pass.

## Onboarding state
- Added `users.onboarding_completed` as the smallest possible database change.
- New registrations start with onboarding incomplete.
- Existing users from Milestone 5A-2 are treated as already onboarded when the migration adds the column, so the tour does not unexpectedly appear for them.
- Completing or skipping the tour marks onboarding complete.
- Replaying the tour does not require changing the completion state.

## Preserved
- SQLite architecture and existing data.
- DAO/service/algorithm layers.
- JSON/Jackson import workflow.
- Public API demonstration.
- JavaFX background Task/concurrency implementation.
- Existing study screens and navigation.
- No new external dependency.

## Deliberately not implemented
- Dashboard redesign
- Flashcard redesign
- Quiz redesign
- Analytics redesign
- Notifications/gamification
- Cloud accounts
- New AI/API/JSON functionality
- Feature-tour duplication in separate screens

## 5A-3 correction pass — first-launch tour + account data isolation

The first 5A-3 build exposed two issues during real use: the tour overlay was not reliably surfacing after registration, and study records were still global rather than account-scoped.

This correction pass fixes both without changing the existing study features:

- The first-launch tour is now scheduled after the main shell has been attached to its scene/window.
- The tour overlay is explicitly brought to the front and made interactive.
- Existing replay control continues to use the same tour implementation.
- All study data tables now carry a `user_id` ownership column.
- All study-data DAO reads, updates, deletes, counts, and inserts are scoped to `CurrentUser`.
- Existing pre-account study records are preserved and assigned to the oldest existing account when possible.
- If a legacy database contains study records but no accounts yet, the first registered account claims those legacy records.
- Newly registered accounts therefore start with an empty study workspace while the original account keeps its existing subjects, topics, tasks, exams, sessions, flashcards, quizzes, and dependencies.
- No study records are deleted during migration.

## 5A-3 hotfix — tour still failed to display after the correction pass

The scheduling/z-order correction above was necessary but not sufficient: the tour still never appeared, because `MainController.highlightTourTarget` used a plain `switch` on the step's target name, and a plain `switch` on a `null` `String` throws a `NullPointerException`.

The Welcome and Finish steps intentionally pass a `null` target (there is no sidebar item to highlight for them). Since the tour always renders the Welcome step first, this exception was thrown immediately when the tour opened — before the overlay was ever added to the scene or made visible — so the tour silently failed to display every time, for both first-launch and replay.

- `highlightTourTarget` now checks for a `null` target before evaluating the switch, restoring the tour for both first-launch and the sidebar **Take Feature Tour** replay.
- No other behaviour, screen, or step content changed.
