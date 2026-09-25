# Revision Assistant — Milestone 4B-2

Milestone 4B-2 builds on 4B-1 without redesigning the JSON import workflow.

## Primary AI-content workflow

The application does **not** require an AI-provider API key. Generate structured JSON with any external AI tool, save it as `.json`, and import it through the Flashcards or Quiz screen.

```text
Study material
    ↓
Any external AI tool
    ↓
Structured JSON
    ↓
.json file
    ↓
Revision Assistant
    ↓
Import JSON
    ↓
Jackson DTO
    ↓
Validation
    ↓
Preview
    ↓
User confirms
    ↓
Existing Service
    ↓
DAO
    ↓
SQLite
```

## Milestone 4B-2 API demonstration

The Study Tools → API Demo tab contains a small public API demonstration using:

`https://jsonplaceholder.typicode.com/todos/1`

No API key, account, environment variable, model name, or backend configuration is required.

The demonstration performs:

```text
JavaFX button
    ↓
JavaFX Task
    ↓
Background thread
    ↓
Java HttpClient
    ↓
HTTP response
    ↓
Jackson ObjectMapper
    ↓
ApiDemoResponseDTO
    ↓
JavaFX Application Thread
    ↓
Displayed DTO values
```

The service handles network failures, timeouts, non-2xx HTTP responses, empty responses, invalid JSON, and unexpected response structures with concise user-facing messages.

## Concurrency

The API request is performed in a JavaFX `Task` on a daemon background thread. The initiating button is disabled while the task runs, a progress indicator is shown, and a Cancel button is available.

Flashcard and Quiz JSON deserialization/validation also run in JavaFX `Task`s on daemon background threads. Preview dialogs and all JavaFX control updates happen only after the task completes on the JavaFX Application Thread.

Database writes remain in the existing service/DAO architecture and occur only after the user confirms the preview.

## Run

Requirements:

- JDK 26
- Maven
- Network access is only needed for the optional API demonstration

Run:

```bash
mvn clean javafx:run
```

The application starts without any AI API configuration.

## JSON examples

### Flashcards

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

### Quiz

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

## Milestone 4B-2 test checklist

- [ ] Application starts with no AI API key or configuration.
- [ ] API Demo successful response is displayed.
- [ ] API/network failure produces a concise error/status and leaves the rest of the app usable.
- [ ] HTTP error is handled without a stack trace in the UI.
- [ ] Invalid/empty/unexpected API response is handled.
- [ ] API request keeps the JavaFX UI responsive.
- [ ] API Cancel button stops/cancels the JavaFX task when applicable.
- [ ] Valid flashcard JSON imports correctly.
- [ ] Valid quiz JSON imports correctly.
- [ ] Larger JSON import processing occurs in a background task.
- [ ] Import buttons are disabled during processing and restored afterward.
- [ ] Invalid JSON does not change the database.
- [ ] Preview still supports selection and cancellation.
- [ ] Only confirmed/selected imported items are persisted.
- [ ] Existing manual flashcard functionality still works.
- [ ] Existing manual quiz functionality still works.
- [ ] Existing Milestone 1–4A functionality remains available.

## Project structure additions

```text
src/main/java/com/revisionassistant/
├── controller/
│   ├── FlashcardController.java      # JSON import Task orchestration
│   ├── QuizController.java           # JSON import Task orchestration
│   └── StudyToolsController.java     # API demo Task orchestration
├── dto/
│   └── ApiDemoResponseDTO.java       # Public API response DTO
└── service/
    ├── ApiDemoException.java         # User-facing API failure type
    └── ApiDemoService.java           # HTTP + Jackson API processing
```

No database schema changes are introduced by Milestone 4B-2.
