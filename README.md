# Revision Assistant

A desktop study assistant built with JavaFX and SQLite. Helps students organise their subjects, topics, and tasks, track progress automatically, and practise with quizzes and flashcards.

---

## Features

| Section | What it does |
|---|---|
| **Dashboard** | Overview of progress, today's tasks, upcoming exams, study activity chart, Daily Quote response card, and quick actions |
| **Subjects** | Create and colour-code subjects (e.g. Mathematics, Physics) |
| **Topics** | Add syllabus topics per subject; mark each one as completed |
| **Study Planner** | Create tasks with priority, deadline, and estimated time; AI-style planner generates an optimal session |
| **Exams** | Record exam dates; progress is calculated automatically from topic completion |
| **Study Sessions** | Log focused study time against a subject/topic |
| **My Study Path** | Visual milestone journey — completed/current/upcoming topics drawn from real data, no manual percentages |
| **Flashcards** | Create cards, flip with animation, filter by subject/status |
| **Quiz** | MCQ quiz with one-question-at-a-time flow, per-question feedback, score summary |
| **Study Tools** | Topic dependency graph and Study Planner (Knapsack/SumOfSubsets algorithms) |
| **Pomodoro Timer** | Focus/short-break/long-break cycle with visual countdown |
| **Mind Map** | Drag-and-drop node canvas, Bezier connection lines |
| **Resources** | Save study links organised by subject and category |
| **Settings** | Appearance, notification, and Pomodoro preferences |

---

## Technology

- **Java 21** (LTS)
- **JavaFX 21** — UI framework (FXML + CSS)
- **SQLite** via `sqlite-jdbc` 3.45 — embedded database, no server required
- **Jackson** 2.17 — JSON import/deserialisation for flashcards and quiz questions
- **Maven** — build and dependency management

---

## Architecture

```
src/main/java/com/revisionassistant/
├── Main.java                   Application entry point
├── algorithm/                  Knapsack, SumOfSubsets, BFS, DFS, TopologicalSort, etc.
├── api/                        ApiClient, ApiConfig, ApiException, ApiResponse
├── controller/                 One controller per view/screen
├── dao/                        Data-access objects (one per entity)
├── database/                   DatabaseManager — schema creation and connection factory
├── dto/                        Data-transfer objects (JSON import and feature payloads)
├── model/                      Plain Java models (Subject, Topic, Task, Exam, …)
├── navigation/                 AppNavigator — stage/scene switching
├── security/                   PasswordHasher, RememberMeStore
├── service/                    Business logic layer (one service per domain)
├── session/                    CurrentUser — singleton session holder
└── util/                       DialogStyler
```

---

## Database

SQLite file `revision_assistant.db` is created automatically in the working directory on first launch.

**Main tables:** `users`, `subjects`, `topics`, `tasks`, `exams`, `study_sessions`, `flashcards`, `quiz_questions`, `quiz_attempts`, `quiz_attempt_answers`, `topic_dependencies`, `remember_tokens`

**Progress calculation:**  
`subject_progress = completed_topics / total_topics`  
`overall_progress = sum(completed_topics) / sum(total_topics)`  

No manual percentage entry — all progress is derived from topic completion.

---

## How to Run

### Prerequisites

- JDK 21 or later
- Maven 3.8+

### Steps

```bash
# Clone or extract the project
cd revision-assistant

# Run directly
mvn javafx:run

# Or build first then run
mvn package
java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml -jar target/revision-assistant.jar
```


## Screens

1. **Login / Register** — local account with bcrypt password hashing and "remember me"
2. **Dashboard** — stats ring, today's tasks, exam countdowns, activity chart
3. **Subjects & Topics** — colour-coded subject cards with per-topic completion
4. **Study Planner** — tasks sorted by priority/deadline; planner uses Knapsack optimisation
5. **My Study Path** — connected milestone path per subject, updated live from topic data
6. **Flashcards** — library view + study mode with flip animation
7. **Quiz** — question library management + one-at-a-time quiz mode
8. **Pomodoro** — 25/5/15 configurable timer with session counting
9. **Mind Map** — free-form drag canvas with Bezier connector lines
10. **Resources** — link organiser by subject and category
11. **Study Tools** — dependency graph and planner algorithm
12. **Settings** — appearance, notifications, Pomodoro durations

---
