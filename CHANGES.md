# UI fixes (quick access, sidebar rail, date pickers, rounded cards)

- Quick Access cards now shrink to fit the panel (tighter gap, narrower cards, folded deck also clamped), so hovering no longer pushes cards out of the window on small screens.
- Collapsed sidebar: every nav icon sits in the same fixed, centred slot so none are cut off; Log out is removed from the screen while collapsed and returns on expand; resizing the window no longer widens a collapsed rail.
- Date pickers: removed the inner purple-bordered box (the editor is now borderless in every state, selection highlight is soft) and raised text contrast in the light theme.
- Study Planner and Exam panels/cards use a larger corner radius. Also fixed planner cards adding "mission-card mission-completed" as one invalid class, so completed/overdue styling now applies.

# UI overhaul, pass 3 — progress ring, dashboard reorg, Quick Access card stack

Same verification caveat as passes 1–2: read through carefully, not rendered. Please check this batch before I move on to spec sections 7–8.

- **Overall Progress ring**: the stat card's icon slot is now a real circular progress indicator (background track `Circle` + progress `Arc`, both `managed="false"` to avoid the StackPane-recentering wobble an animated Arc causes) with the percentage centered inside it, animated on refresh via the same Timeline technique the old (now-removed) donut used, just at stat-icon scale. The existing `overallProgressValue`/"Overall progress" text next to it is unchanged.
- **Reorganized**: Subject Progress now sits beside Today's Tasks (was beside Continue Studying); a new row below holds Continue Studying beside Quick Access (was Today's Tasks beside Quick Actions). Pure layout swap — no data or behavior changes to either section.
- **Quick Access → stacked cards**: rebuilt from a plain vertical list into a real overlapping card stack in a `Pane` (children positioned by absolute `layoutY`, staggered ~16px, so they visually overlap like a fanned deck). At rest, each card's label is `managed=false`/invisible — only the icon renders, sized to just the icon. Hovering a card reveals its label (fade-in) and brings it to front (`toFront()`); cards stacked *above* the hovered one (earlier in the list) slide right via `TranslateTransition` to get out of the way; cards below stay put. Same 5 actions, same click targets, only the presentation changed. Added a `Tooltip` per card (name shown at rest) since the icon alone doesn't label itself.
  - **One spec line I want to flag rather than silently guess on**: point 3 says "the topmost/active card should display the 'Continue Studying' content," but point 2 explicitly makes Continue Studying and Quick Access two separate side-by-side sections. I built Quick Access as its own 5-card stack (Add Subject / Revision Task / Flashcards / Start Quiz / Add Exam) with no default-open "Continue Studying" card among them, since that's the only reading consistent with the rest of the spec ("keep existing Quick Access functionality unchanged" — those 5 actions, not 6). If you meant something else by that line, tell me and I'll adjust.

# UI overhaul, pass 2 — Dashboard hero band + Quick Actions

Continuing the overhaul, same caveat as pass 1: verified by reading, not by rendering — please rebuild and look before I go further.

- **Removed the "Overall Progress" donut entirely** (ring + arc + "X of Y topics completed" block), not just hidden — it was redundant with the "Overall progress" stat card and Study Path. This also removed `overallProgressArc`/`overallProgressRingValue`/`progressSummaryLabel` and the `animateProgress()` method from `DashboardController`, since nothing else used them. "Upcoming Exams" (the donut's former grid sibling) is now its own full-width section.
- **Hero band**: greeting, the 5 stat cards, and the Daily Quote are now grouped inside one `.dashboard-hero-band` — a background tint + padding, not a bordered/shadowed card, so the stat cards and quote card inside it don't end up nested inside another card.
- **Stat card icons**: the 5 typed glyphs (◎ ✓ ◷ □ ◈) replaced with real vector icons (same `Circle`/`Rectangle`/`Polygon`/`Line` primitive approach as the sidebar), tinted per card via the existing `stat-icon-purple/blue/orange/pink/teal` color roles.
- **Daily Quote redesigned**: a large quote-mark glyph replaces the old plain "Daily Quote" label (the quote itself now signals what the card is), and the "Fetched from the daily quote API" text moved from a floating top-right corner label into a small footer caption inside the card.
- **Quick Actions redesigned**: the 2-column button grid is now a stacked list of 5 rows, built in `DashboardController` (not FXML) since the hover behavior needs a `TranslateTransition` per row — each row's icon sits mostly clipped off its left edge at rest and slides into view on hover (~160ms). Same 5 actions (Add Subject / Revision Task / Flashcards / Start Quiz / Add Exam), same navigation targets, restyled only.
- Caught and fixed a real bug while verifying this pass: the new stat-card icons use `Polyline` twice, which had no `<?import?>` in `DashboardView.fxml` — would have been a hard `LoadException` at runtime. Added the import.

# UI overhaul, pass 1 — app chrome + Study Path progress bars

Working through the UI overhaul spec in the order it suggests (isolated/low-risk pieces first), since I can't render or screenshot JavaFX in this environment — everything below was verified by reading the FXML/CSS/Java, not by looking at it run. A real `mvn clean javafx:run` + visual check is the next step before continuing.

- **Sidebar nav icons**: every typed Unicode glyph (⌂ ▦ ◇ ✓ □ ◷ ◈ ▤ ? ✦ ⏱ ⬡ 🔗 ⚙) is replaced with a real vector icon built from JavaFX's own shape primitives (`Circle`/`Rectangle`/`Polygon`/`Line`/`Polyline`), not hand-authored SVG path strings — this avoids the risk of a subtly malformed path with no way to visually catch it here. Icons are tinted via a shared `.nav-icon`/`.nav-icon-stroke` class pair that already follows the same hover/active states as the button text.
- **Top bar**: removed the redundant "Revision Assistant" label next to the bell (duplicated the sidebar brand + OS window title). Added a hamburger toggle (also built from primitives) that animates the sidebar's width closed/open via a `Timeline` (~180ms, ease-out) instead of an instant show/hide. Added a thin accent-colored rule next to the page title for a bit more "app" presence, reusing the title/subtitle styling that was already appropriately bold/muted.
- **My Study Path progress bars — real bug found and fixed**: `.path-overall-bar` and `.path-subject-bar` only styled the filled `.bar` region, never the `.track` (the unfilled background) — unlike `.quiz-progress-bar` elsewhere in the same file, which correctly styles both. Without an explicit track color, JavaFX falls back to a default track that can wash out against the surrounding card, so at low/zero progress the bar reads as empty — matching the reported symptom exactly. The Java-side binding (`overallProgressBar.setProgress(...)`, `new ProgressBar(fraction)`) was already correct. Fixed by styling both regions explicitly and bumping height (8px→12px overall, 6px→10px per-subject) for legibility.

**Not yet done** (this is a large, multi-page spec): the dashboard hero band/stat card icons/quick actions redesign, and the purpose-specific relayout of every other page (Subjects, Topics, Study Planner, Exams, Study Sessions, Flashcards, Quiz, Study Tools, Mind Map, Resources). Continuing page-by-page next, per the spec's own suggested order.

# Quiz → weak topic → existing recommendation, and a Pomodoro dialog fix

- Quiz results now surface a "Needs Review" panel right under the score when `TopicMasteryService` — the same engine Study Path and Dashboard already use, no second mastery system — already flags a topic the quiz covered as weak. Works for both a topic-scoped quiz and an "All topics" quiz, since it reads each question's own topic rather than the attempt's single topic field. Each weak topic shown has a "Study Topic →" button that jumps straight to My Study Path, where that same topic is now the one being recommended for the same reason shown in the quiz panel.
- Added one lookup method to `TopicMasteryService`, `getInsightForTopic(topicId)` — a thin wrapper around the existing `getInsights()` computation, not a new scoring path.
- **Pomodoro fix**: the "log this session?" dialog wasn't setting an owner window or modality (every other dialog in this app does), so it could open without a proper window relationship to the main stage and go unnoticed. It now calls `initOwner`/`initModality` like the rest of the app's dialogs, and the surrounding catch was broadened from `SQLException` only to also catch and log any other runtime error instead of letting it vanish silently on the FX thread.

# Connected study workflow (Quiz/Exam/Task → Study Path/Dashboard, Pomodoro → Study Sessions)

- New `TopicMasteryService` is the one place that turns existing quiz attempts, study sessions, tasks, exam↔topic links and topic prerequisites into a ranked "what to study next, and why" recommendation. No new tables, no duplicated data — it only reads what `QuizService`, `StudySessionService`, `TaskService`, `ExamService` and `TopicDependencyService` already store.
- **My Study Path**'s "current" topic is now this recommendation instead of "first incomplete topic in the list" — it accounts for weak quiz scores, an upcoming exam covering the topic, an unmet prerequisite, or an overdue linked task, and shows the reason under the topic.
- **Dashboard**'s "Continue Studying" card now shows the same recommendation (with its reason) as the primary suggestion, falling back to the nearest task deadline, an unfinished subject, or an upcoming exam only when there's no topic left to recommend.
- **Pomodoro**: finishing a focus session now offers a compact one-step dialog (subject + optional topic — duration is already known) to log it as a real `StudySession`, so Pomodoro time shows up in the same study history as everything else instead of vanishing when the timer resets.
- `QuizService` gained one small read method, `getAllAttempts()`, reused by `TopicMasteryService` instead of it querying the database on its own.

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
