# Milestone 5A-1 Change Log

## Modified

- `src/main/resources/com/revisionassistant/fxml/MainView.fxml`
  - Replaced the old generic sidebar with the persistent application shell.
  - Added branded sidebar, grouped navigation, top header, page title/subtitle, and profile placeholder.
  - Preserved all existing navigation actions.

- `src/main/java/com/revisionassistant/controller/MainController.java`
  - Preserved all existing view-loading routes.
  - Added page title/subtitle updates.
  - Added a reusable short FadeTransition for central page changes.
  - No business logic, services, DAOs, or database access added.

- `src/main/resources/com/revisionassistant/css/style.css`
  - Replaced the old blue/gray theme with the Milestone 5A-1 design system.
  - Added reusable tokens and styles for surfaces, navigation, buttons, inputs, cards, badges,
    progress, focus, hover, success/warning/error states, and typography.

- `README.md`
  - Added Milestone 5A-1 design, shell, transition, and verification notes.

## Backend preservation

No changes were made to SQLite, DAOs, services, algorithms, models, DTOs, JSON/Jackson code,
API demonstration code, or concurrency code.
