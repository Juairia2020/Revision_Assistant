# Latest merge

- Resources are now persisted to the database (a `resources` table, scoped per user) instead of living only in memory for the session — a saved link, its subject and category now survive an app restart, and the old "stored for this session" notice is gone. Backed by a new `Resource` model, `ResourceDAO`, and `ResourceService` following the same layering as every other feature.

# Merge notes (StudyPlanner/StudyPath/Exam/Reminders + StudySession/MindMap/Fixes)

- Study sessions can now be logged directly from a Study Task card ("+ Study Session" button).
- In-app toast notifications are compact (smaller card, tighter padding/spacing, shorter font) and stack neatly in the top-right corner.
- Reminders now show both as in-app toasts *and* as native desktop (system tray) notifications, so they're visible even if the app isn't focused.
- My Study Path is a gamified, per-subject journey: circular progress nodes, "Study Now" / "Completed" / "Upcoming" badges, and Mark Done actions — all driven automatically by real topic completion.
- Flashcards are centered on the page; the answer face now sits on a high-contrast highlighted chip so it's clearly readable against the gradient background in both themes.
- The exam progress slider is gone. Exams are created by selecting the syllabus topics that belong to them, and the progress bar is calculated automatically from how many of those topics are completed.
- Mind map connections are smooth curved (Bezier) lines, node positions are never re-clamped/recalculated on load, and marking a node "central" no longer re-arranges every other node — only the chosen node moves, exactly like a real desktop mind-mapping app.
- The top bar stays dark even in the light theme, for a consistent branded header.
- Fixed a hover bug in list-based sections (e.g. Study Tools' feature lists) where content dropped to reduced opacity and effectively disappeared on hover/selection; hovered/selected rows now stay fully visible.

# Latest UI / UX changes

- Light workspace now uses white as the primary app surface while the left navigator stays dark.
- Settings now has working Light/Dark theme controls with immediate application plus Save Preferences persistence.
- Reminder notifications respect the four selected reminder/notification settings and expose a notification bell, in-app toast notifications, history, and a test-selected-reminders action in Settings.
- Mind Map now lets any existing node become the active/central focus, add branches to any selected node, double-click to center, keep sibling branches separated, and preserve node positions/focus per user.
- Quiz answer choices are rendered as large selectable option cards with letter badges, clearer question headers, progress counts, and correct/incorrect feedback.
- Daily Quote remains on the Dashboard as the API response-handling example; the old API Demo section is removed.
- Native tab/date-picker focus rectangles remain visually quiet rather than showing the old purple focus treatment.
