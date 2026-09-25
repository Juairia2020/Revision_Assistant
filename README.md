# Revision Assistant — Milestone 5B-1

Milestone 5B-1 redesigns the Dashboard into the visual study overview while preserving the completed Milestone 5A-3 architecture, account scoping, feature tour, JSON import, API demonstration, concurrency, and existing CRUD functionality.

## 5B-1 Dashboard implementation

The dashboard now uses existing services and data to provide:

- personalized morning/afternoon/evening greeting
- overall progress calculated from completed topics
- remaining revision tasks
- study time for the current seven-day period
- upcoming exam count
- completed topic count
- circular overall-progress visualization
- upcoming exam cards with preparation calculated from the exam subject's completed topics
- seven-day study activity chart from existing `StudySession` records
- subject progress bars based on completed topics
- Continue Studying action using existing tasks/topics/exams
- quick navigation actions for subjects, planner, flashcards, quiz, and exams
- empty states for missing subjects, exams, and study sessions
- short fade/progress animations that do not introduce a new background execution system

No database schema, DAO, JSON, API, algorithm, or concurrency redesign was added for the dashboard. The existing service layer remains the source of application data.

### Automatic progress rules

- **Overall progress:** completed topics ÷ all topics. If there are no topics, progress is 0%.
- **Subject progress:** completed topics for that subject ÷ total topics for that subject.
- **Exam preparation:** completed topics for the exam's subject ÷ total topics for that subject. The existing manual `Exam.progress` value is not used to display dashboard preparation.
- **Study activity:** study-session duration is grouped by date for the last seven days, including today.
- **Continue Studying:** uses existing incomplete tasks first, then an incomplete subject/topic area, then the next upcoming exam. No new recommendation algorithm is introduced.

### Milestone 5B-1 testing checklist

- [ ] Start with no subjects and verify the dashboard shows useful empty states.
- [ ] Add subjects/topics and verify overall and subject progress use real completion data.
- [ ] Complete a topic and verify the relevant progress changes.
- [ ] Add a study session and verify the activity chart changes.
- [ ] Add an exam and verify its preparation follows the subject's topic completion.
- [ ] Add/complete a task and verify the remaining-task card and Continue Studying action.
- [ ] Use each dashboard quick action and verify normal navigation.
- [ ] Resize the application and verify the dashboard remains vertically scrollable.
- [ ] Replay the Feature Tour and verify Next, Previous, Skip Tour, and Start Studying still work.
- [ ] Verify login/logout, onboarding, Subjects, Topics, Planner, Exams, Study Sessions, Flashcards, Quiz, Study Tools, JSON import, API demo, algorithms, and concurrency remain intact.

---

Final integration, cleanup, regression-test checklist, and viva documentation for the Revision Assistant project.

## 1. Final workflow

The primary AI-content workflow is intentionally provider-independent. The application does **not** call an AI provider and does **not** require an AI API key.

```text
Study material
    ↓
Any AI tool
    ↓
Required JSON
    ↓
Save as .json
    ↓
Revision Assistant
    ↓
Import JSON
    ↓
Jackson deserialization
    ↓
DTO
    ↓
Validation
    ↓
Preview / selection
    ↓
User confirms
    ↓
Existing Service
    ↓
DAO
    ↓
SQLite
```

The imported data is saved only after the user confirms the preview. Existing manual flashcard and quiz entry continues to use the same services and database.

### Copy Prompt

The **Copy Prompt** button copies a small schema-specific prompt to the clipboard. A user may paste that prompt into any AI tool and ask it to produce compatible JSON. This is a convenience feature; it is not an API integration.

## 2. JSON format

### Flashcard JSON

```json
{
  "topic": "Dijkstra's Algorithm",
  "flashcards": [
    {
      "question": "What is Dijkstra's algorithm used for?",
      "answer": "Finding shortest paths from a source vertex."
    }
  ]
}
```

Validation requires a non-empty `topic`, a non-empty `flashcards` array, and non-empty `question` and `answer` values for every card.

### Quiz JSON

```json
{
  "topic": "Dijkstra's Algorithm",
  "questions": [
    {
      "question": "Which structure is commonly used in Dijkstra's algorithm?",
      "options": [
        "Stack",
        "Priority Queue",
        "Linked List",
        "Hash Table"
      ],
      "correctAnswer": "Priority Queue"
    }
  ]
}
```

Validation requires a non-empty `topic`, a non-empty `questions` array, exactly four distinct non-empty options, and a `correctAnswer` that matches one of the four options. The validator also derives the corresponding A/B/C/D option letter for the existing quiz model.

### Jackson and DTOs

Jackson's `ObjectMapper` reads the `.json` file and deserializes it directly into `FlashcardImportDTO` or `QuizImportDTO`, which contain `ImportedFlashcardDTO` or `ImportedQuizQuestionDTO` items. No manual JSON string parsing is used.

The DTOs are temporary data-transfer objects used for import, validation, and preview. They do not write directly to SQLite. After confirmation, the existing `FlashcardService` or `QuizService` performs the normal service → DAO → SQLite persistence flow.

## 3. Public API demonstration

The Study Tools → API Demo tab is a deliberately small demonstration of HTTP, JSON, Jackson, and JavaFX integration.

**Endpoint:** `https://jsonplaceholder.typicode.com/todos/1`

No API key, account, environment variable, model name, or project configuration is required.

```text
JavaFX button
    ↓
JavaFX Task
    ↓
Daemon background thread
    ↓
Java HttpClient
    ↓
HTTP response
    ↓
JSON text
    ↓
Jackson ObjectMapper
    ↓
ApiDemoResponseDTO
    ↓
JavaFX Application Thread
    ↓
Displayed DTO values
```

`ApiDemoService` handles network failures, timeouts, non-2xx HTTP responses, empty responses, malformed JSON, and missing/unexpected DTO fields. The API demo is isolated from the normal CRUD, import, planner, and study flows.

## 4. Concurrency

JavaFX controls must be accessed on the **JavaFX Application Thread**. Blocking work such as HTTP requests and potentially large JSON deserialization is therefore performed inside JavaFX `Task` objects running on daemon worker threads.

For API loading:

- The Load button is disabled while a request is active.
- A Cancel button and progress indicator show the active state.
- Success, failure, and cancellation handlers restore the controls.
- `Task` completion handlers run on the JavaFX Application Thread, so UI updates are safe.
- The worker thread is daemonized and terminates when its task finishes.
- Cancellation does not modify the database.

For JSON imports:

- The file is read, deserialized, and validated in a background `Task`.
- Import controls are disabled while processing.
- The preview dialog opens only after the background task succeeds.
- Database writes remain in the existing service/DAO layer and happen only after user confirmation.

The project does not add a permanent executor, thread pool, or background service that would need application shutdown management.

## 5. Final architecture

```text
FXML
  ↓
Controller
  ↓
Service
  ├── JSON import → Jackson → DTO → validation → preview
  ├── API demo → HttpClient → JSON → Jackson → DTO
  └── Existing business services
          ↓
        DAO
          ↓
       SQLite
```

The controllers coordinate UI state. Services contain import, API, business, and persistence logic. DAOs remain responsible for database access. No database schema redesign was introduced for Milestone 4B-3.

## 6. Project structure

```text
src/main/java/com/revisionassistant/
├── algorithm/          # Graph and planning algorithms
├── controller/         # JavaFX controllers
├── dao/                # SQLite data-access objects
├── database/           # Database connection/initialisation
├── dto/                # JSON/API data-transfer objects
├── model/              # Existing domain models
├── service/            # Business, import, planner and API-demo services
└── Main.java

src/main/resources/com/revisionassistant/
├── css/style.css
└── fxml/               # Application views and import preview dialogs

revision_assistant.db  # Existing SQLite database; no schema replacement
pom.xml                # JDK 26 + JavaFX + SQLite + Jackson dependencies
README.md
```

## 7. Dependencies

Only the required external dependencies are used:

| Dependency | Version | Purpose |
|---|---:|---|
| JavaFX Controls | 26.0.2 | JavaFX UI controls |
| JavaFX FXML | 26.0.2 | FXML views/controllers |
| SQLite JDBC | 3.53.2.1 | SQLite database access |
| Jackson Databind | 2.19.2 | JSON deserialization |

The public API demo uses Java's built-in `java.net.http.HttpClient`; no extra HTTP library is required.

Build target: **JDK 26** (`maven.compiler.release=26`).

## 8. Viva preparation

### Why is JSON used?

JSON is a simple, portable text format. It lets the user generate structured study content with any AI tool and move that content into the application without coupling the application to one AI provider.

### What does Jackson do?

Jackson converts JSON text into Java objects. In this project, `ObjectMapper.readValue(...)` performs the deserialization into DTO classes.

### What is deserialization?

Deserialization is the process of converting serialized data, here JSON text, back into structured Java objects.

### What is a DTO?

A Data Transfer Object is a simple object used to carry data between parts of a system. The import DTOs represent the JSON structure before the data is accepted by the existing application services.

### How is an API response handled?

`ApiDemoService` creates an HTTP GET request, sends it with Java `HttpClient`, checks the HTTP response, verifies that the body is non-empty, and passes the JSON body to Jackson. Jackson creates an `ApiDemoResponseDTO`, which is returned to the controller for display.

### Why should the API request not run on the JavaFX Application Thread?

An HTTP request can wait on the network. If it blocks the JavaFX Application Thread, the window cannot repaint or respond to input. Running the request in a background `Task` keeps the UI responsive.

### How is Task used?

A JavaFX `Task<T>` defines the background operation in its `call()` method. The worker thread runs the task, while `setOnSucceeded`, `setOnFailed`, and `setOnCancelled` handlers return control to safe UI updates.

### How does imported data reach SQLite?

The controller starts the background JSON import. Jackson creates DTOs, validation checks the DTO contents, and a preview lets the user select and confirm records. The controller then calls the existing `FlashcardService` or `QuizService`, which uses the existing DAO to insert records into SQLite.

## 9. Final regression checklist

### Milestones 1–3

- [ ] Subject CRUD
- [ ] Topic CRUD
- [ ] Topic dependencies and algorithms
- [ ] Study Planner
- [ ] Exams
- [ ] Study sessions
- [ ] Dashboard
- [ ] Flashcards
- [ ] Quiz
- [ ] Frequently forgotten / difficult content
- [ ] Existing database data remains available

### Milestone 4

- [ ] Flashcard JSON import
- [ ] Quiz JSON import
- [ ] Jackson deserialization
- [ ] DTO validation
- [ ] Preview and selection
- [ ] Confirm/cancel behavior
- [ ] Public API demonstration
- [ ] Network/timeout/HTTP/JSON/structure error handling
- [ ] Background API processing
- [ ] Background JSON processing
- [ ] Loading indicators and button state
- [ ] API cancellation
- [ ] No AI API key required
- [ ] Manual flashcard and quiz functionality still works

### Final checks

- [ ] Start application with no AI-provider configuration
- [ ] Import a valid flashcard JSON file and confirm records appear in SQLite
- [ ] Import a valid quiz JSON file and confirm questions appear in SQLite
- [ ] Try malformed JSON and confirm no database change
- [ ] Try an invalid quiz with the wrong number of options and confirm validation stops the import
- [ ] Cancel an import preview without saving
- [ ] Load the public API sample and verify DTO values are displayed
- [ ] Verify the API tab does not block normal navigation
- [ ] Verify API errors restore the controls
- [ ] Verify the project compiles and runs under JDK 26 with Maven

## 10. Milestone 4B-3 cleanup

The following obsolete provider-specific AI integration was removed because the current architecture no longer uses it:

- `src/main/java/com/revisionassistant/api/ApiClient.java` — old provider-specific HTTP client requiring an API key.
- `src/main/java/com/revisionassistant/api/ApiConfig.java` — old environment/configuration reader for AI credentials.
- `src/main/java/com/revisionassistant/api/ApiException.java` — exception type belonging only to the removed provider client.
- `src/main/java/com/revisionassistant/api/ApiResponse.java` — provider-specific response model.
- `src/main/java/com/revisionassistant/service/ApiService.java` — old AI-generation service built around the removed provider client.
- `GeneratedFlashcardDTO.java` and `GeneratedQuestionDTO.java` — preview DTOs for the removed direct-AI workflow.
- `GeneratedFlashcardsDialogController.java` and `GeneratedQuestionsDialogController.java` — unused dialogs for that removed workflow.
- `GeneratedFlashcardsDialog.fxml` and `GeneratedQuestionsDialog.fxml` — FXML views for the removed workflow.

No existing CRUD, study, planner, import, preview, or SQLite functionality was removed.

## 11. Verification note

The project was reviewed statically and the source/resource references were checked during Milestone 4B-3 cleanup. The supplied build environment used for this review has JDK 21 and does not have Maven installed, so a full Maven/JDK 26 compile and live JavaFX regression run could not be executed here. The project remains configured for JDK 26, and the final checklist above should be run on a machine with JDK 26 and Maven before submission.

## Milestone 5A-1 — Visual Design System & Application Shell

Milestone 5A-1 redesigns only the JavaFX presentation shell around the existing application.
The SQLite database, DAOs, services, algorithms, JSON/Jackson import flow, API demonstration,
and concurrency implementation are preserved.

### Visual design system

The shared `style.css` now provides reusable design tokens and styles for:

- application background and surfaces
- sidebar/navigation and active states
- primary, secondary, danger, success, warning, and muted text
- cards, section headers, badges, empty states, inputs, tables, and progress indicators
- hover and focus states

The visual language uses a restrained green/teal accent, warm neutral surfaces, clear typography,
subtle borders, and limited corner rounding. It intentionally avoids a large icon dependency.
Navigation symbols are lightweight Unicode characters.

### Application shell

`MainView.fxml` now provides a persistent shell with:

- branded sidebar
- workspace and practice navigation groups
- active navigation state
- top page title/subtitle area
- local application/profile placeholder (not an account system)
- responsive center content area

The existing screens remain the navigated content. No new backend feature is introduced.

### Navigation transition

`MainController` keeps the existing FXML-to-view navigation and adds one reusable 180 ms fade
when the central view changes. The transition is UI-only and does not create a background thread.

### Milestone 5A-1 verification

- All FXML files parse as well-formed XML.
- Every view path used by `MainController` exists.
- Existing navigation handlers remain present.
- No DAO, service, algorithm, model, database, JSON, API, or concurrency source files were changed.
- The project remains configured for JDK 26.

A live JavaFX/Maven build should still be run on the submission machine with JDK 26 because the
available build environment used for static verification does not provide Maven/JDK 26.

## Milestone 5A-2 — Local Authentication

Revision Assistant now starts at a local login screen. New users can create an account and are taken into the existing application shell after successful registration.

### Authentication architecture

```text
LoginView / RegistrationView
        |
        v
LoginController / RegistrationController
        |
        v
UserService
        |
        v
UserDAO
        |
        v
DatabaseManager
        |
        v
SQLite users table
```

The authenticated identity is held by the application-level `CurrentUser` session:

```text
UserService
    -> CurrentUser.set(user)
    -> MainController reads CurrentUser
    -> Log out calls CurrentUser.clear()
```

Controllers do not execute SQL and do not implement password hashing themselves.

### Users table

The database initializer adds the following table with `CREATE TABLE IF NOT EXISTS`:

```sql
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    email TEXT NOT NULL COLLATE NOCASE UNIQUE,
    password_hash TEXT NOT NULL
);
```

This is an additive migration. Existing subjects, topics, tasks, exams, study sessions, flashcards, quiz data, and dependency data are not deleted or changed.

### Password handling

Passwords are hashed with the standard JDK `PBKDF2WithHmacSHA256` implementation. Each password receives a random 16-byte salt and is derived with 210,000 iterations and a 256-bit key. The application stores only the resulting password representation, never the original password.

For a viva:

- **Hashing:** transforms a password into a one-way password representation.
- **Salt:** random data stored with the hash so identical passwords do not produce identical stored values.
- **PBKDF2:** a standard password-based key derivation algorithm designed to make password guessing more expensive.
- **Verification:** the entered password is derived using the stored parameters and compared with the stored derived value.

### Session behaviour

`CurrentUser` is intentionally in memory only. It stores ID, name, and email for the current session and does not retain the password hash. There is no "remember me" feature. Closing the application therefore requires login again.

### Authentication validation

Registration checks:

- required name
- required email
- basic email format
- password length of at least 8 characters
- matching confirmation password
- duplicate email

Login checks:

- required email
- required password
- correct stored password representation
- database failures with a user-friendly message

### 5A-2 regression intent

The authentication layer does not associate existing study records with users. This is deliberate for this milestone: existing Milestone 1–4 data is preserved without a large schema migration. User-specific data ownership can be considered in a later milestone if required.

## Milestone 5A-3 — First-Launch Feature Tour

Milestone 5A-3 adds a reusable first-launch onboarding tour without changing the study backend. New registrations open the existing main shell with a nine-step overlay explaining Dashboard, Subjects & Topics, Study Planner, Flashcards, Quiz, Exams, and the existing AI/JSON import workflow. The overlay supports Previous, Next, Skip Tour, and Start Studying and shows `Step N of 9`.

Onboarding completion is stored in the existing `users` table through a small `onboarding_completed` column. The migration treats users created before this milestone as already onboarded, so existing users are not interrupted by a first-launch tour. The same tour can be replayed later using **Take Feature Tour** in the sidebar.

No new backend feature, external dependency, API, JSON workflow, or study functionality was introduced.

### Milestone 5A-3 correction: first-launch tour and per-account study data

The 5A-3 correction pass makes the account boundary real for the local desktop database. Study tables now store a `user_id`, and DAO operations are scoped to the currently authenticated user. Existing legacy study records are preserved and assigned to the original/oldest account; when a legacy database has no users yet, the first newly registered account claims those records. Later accounts start with an empty study workspace.

The first-launch tour is also scheduled after the main scene is attached and its overlay is explicitly raised above the application shell. The sidebar replay action uses the same reusable tour.


## UI polish and responsiveness
The current build uses the refreshed purple/teal visual theme, application icon, responsive shell sizing, and the revised Study Planner task form. Tasks require a subject and title before they can be added; invalid estimated-time input is handled in the form instead of failing silently.
