# Revision Assistant — Milestone 4B-3

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
