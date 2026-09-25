# Revision Assistant — Milestone 4B-1

Milestone 4B-1 makes JSON-file import the primary AI-content workflow. The application does not require an AI-provider API key, environment variable, model name, or `config.properties` file.

## Workflow

Study material → any external AI tool → structured JSON → `.json` file → Revision Assistant → Import JSON → Jackson DTO → validation → preview → user confirmation → existing Service → DAO → SQLite.

The existing manual Flashcard and Quiz workflows remain available.

## Running

Requirements:
- JDK 26
- Maven
- JavaFX and SQLite dependencies are provided by Maven.

From the project root:

```text
mvn clean javafx:run
```

No API key configuration is required.

## Flashcard JSON

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

## Quiz JSON

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

The topic must already exist under the selected subject. Importing a JSON file never writes to the database until the user confirms the preview.

## Validation

Flashcards require a topic, a non-empty `flashcards` array, and non-empty question/answer values.

Quiz imports require a topic, a non-empty `questions` array, exactly four non-empty distinct options, and a `correctAnswer` that matches one option or is `A`, `B`, `C`, or `D`.

## Scope

This milestone intentionally does not implement in-app AI generation, concurrency, or the later API demonstration.
