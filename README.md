# Revision Assistant

A desktop study assistant built with **Java 21, JavaFX, SQLite, and Maven**. Revision Assistant helps students organise their subjects and syllabus topics, plan study time, track academic progress, practise with flashcards and quizzes, and manage study resources in one application.

---

## Features

| Section | What it does |
|---|---|
| **Dashboard** | Shows overall progress, today's tasks, upcoming exams, study activity, quick actions, and a randomly fetched motivational quote |
| **Subjects** | Create and manage subjects with colour coding |
| **Topics** | Organise syllabus topics under subjects and mark topics as completed |
| **Study Planner** | Create tasks with priority, deadlines, and estimated time; uses optimisation algorithms to select study work |
| **Exams** | Record exams and deadlines; exam progress is derived from topic completion |
| **Study Sessions** | Record focused study time against subjects and topics |
| **My Study Path** | Visual milestone journey generated from actual topic data, showing completed, current, and upcoming topics |
| **Flashcards** | Create, edit, study, filter, import, and generate flashcards with an animated flip interface |
| **Quiz** | Manage MCQ questions, import questions, generate question sets, take one-question-at-a-time quizzes, receive per-question feedback, and view results |
| **Study Tools** | Topic dependency graph plus algorithm-based planning and graph utilities |
| **Pomodoro Timer** | Configurable focus, short-break, and long-break cycles with session counting |
| **Mind Map** | Interactive drag-and-drop canvas with connected nodes and Bezier connection lines |
| **Resources** | Store and organise study links by subject and category |
| **Settings** | Manage appearance, notifications, and Pomodoro preferences |
| **Login / Registration** | Local user accounts with PBKDF2-SHA256 password hashing and optional remember-me login |

---

## Technology Stack

- **Java 21 (LTS)**
- **JavaFX 21.0.2** — desktop UI using FXML and CSS
- **SQLite 3.45.3.0** — embedded local database
- **Jackson 2.17.1** — JSON parsing and import/deserialisation
- **Maven** — dependency and build management
- **Java HttpClient** — external REST API communication for the Dashboard quote
- **Git/GitHub** — source control and project development

---

## Architecture

The project follows a layered structure separating the user interface, business logic, persistence, and data models.

```
src/main/java/com/revisionassistant/
├── Main.java                   Application entry point
├── algorithm/                  Algorithm implementations
│                               (BFS, DFS, Topological Sort, Knapsack,
│                                SumOfSubsets, Shortest Path, etc.)
├── api/                        API-related classes and response handling
├── controller/                 JavaFX controllers for application screens
├── dao/                        Data-access objects for SQLite persistence
├── database/                   Database connection and schema management
├── dto/                        Data-transfer objects for imported data
├── model/                      Application domain models
├── navigation/                 Scene/stage navigation
├── security/                   Password hashing and remember-me storage
├── service/                    Business logic and application services
├── session/                    Current logged-in user/session state
└── util/                       Shared UI and utility classes
```

### Application flow

```
JavaFX View (FXML)
       ↓
Controller
       ↓
Service / Business Logic
       ↓
DAO
       ↓
SQLite Database
```

External quote requests use a separate flow:

```
Dashboard
    ↓
DailyQuoteService
    ↓
Java HttpClient
    ↓
ZenQuotes REST API
    ↓
JSON response
    ↓
Jackson ObjectMapper
    ↓
Quote + Author
    ↓
Dashboard
```

The quote request is performed on a background JavaFX task so network communication does not block the user interface.

---

## Database

The application uses a local SQLite database. The database file `revision_assistant.db` is created in the working directory when the application initialises the database.

### Main entities

```
users
subjects
topics
tasks
exams
study_sessions
flashcards
quiz_questions
quiz_attempts
quiz_attempt_answers
topic_dependencies
remember_tokens
```

### Automatic progress calculation

Progress is derived from completed topics rather than entered manually.

```
subject_progress =
    completed_topics / total_topics

overall_progress =
    total_completed_topics / total_topics
```

This allows the Dashboard, Exams, and Study Path features to reflect changes to the syllabus automatically.

---

## Algorithms

The project includes several algorithm implementations used by the study-planning and study-tools features:

- **Breadth-First Search (BFS)**
- **Depth-First Search (DFS)**
- **Topological Sort**
- **Cycle Detection**
- **Shortest Path**
- **Knapsack**
- **Sum of Subsets**

The Study Planner uses optimisation algorithms to select useful study tasks under time constraints, while the dependency features use graph algorithms to work with relationships between topics.

---

## JSON Import and Generated Study Material

Revision Assistant supports structured study-material workflows using JSON.

### Import

Jackson is used to parse JSON data for:

- Flashcards
- Quiz questions
- Other feature payloads represented by project DTOs

The application validates imported data and provides dedicated import dialogs for flashcards and questions.

### Generated material

The application also contains dedicated interfaces for generated:

- Flashcards
- Quiz questions

This keeps generated material separate from the normal study libraries while still allowing it to be reviewed and used in the application.

---

## Daily Quote API

The Dashboard includes an external REST API integration.

The application sends an HTTP **GET** request to:

```
https://zenquotes.io/api/random
```

The response is JSON. Jackson's `ObjectMapper` parses the response and extracts:

- `q` → quote text
- `a` → author

If the API request fails, the Dashboard displays a local fallback quote instead of leaving the quote card empty.

---

## Security and Login

User passwords are **not stored as plain text**.

The application uses:

- **PBKDF2WithHmacSHA256**
- Random salt per password
- 210,000 PBKDF2 iterations
- 256-bit derived keys
- Constant-time hash comparison

The application also supports a device-local **Remember Me** feature. The remember-me token is stored separately from the database and is excluded from version control.

---

## Main Screens

1. **Login / Register** — local account creation, login, password hashing, and remember-me support
2. **Dashboard** — progress overview, tasks, exams, activity, quick actions, and API quote
3. **Subjects** — subject management and colour coding
4. **Topics** — syllabus topic management and completion tracking
5. **Study Planner** — task management and optimisation-based planning
6. **Exams** — exam dates and progress
7. **Study Sessions** — focused study-time tracking
8. **My Study Path** — data-driven visual study progression
9. **Flashcards** — flashcard library and study mode
10. **Quiz** — question management and quiz mode
11. **Study Tools** — dependency graph and algorithm utilities
12. **Pomodoro** — configurable focus/break timer
13. **Mind Map** — interactive visual topic mapping
14. **Resources** — study-link organiser
15. **Settings** — application preferences

---

## How to Run

### Prerequisites

- **JDK 21 or later**
- **Maven 3.8+**
- Internet connection is recommended for the Dashboard's external quote API feature

### Run with Maven

Clone or download the repository, then open a terminal in the project directory:

```bash
git clone https://github.com/Juairia2020/Revision_Assistant.git
cd Revision_Assistant

mvn clean compile
mvn javafx:run
```

### Build the project

```bash
mvn clean package
```

Maven automatically regenerates the `target/` directory during compilation/build. Generated Maven output is intentionally excluded from Git.

---

## Project Structure

```
Revision_Assistant/
├── .gitignore
├── CHANGES.md
├── README.md
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       └── resources/
└── revision_assistant.db
```

The `target/` directory is generated locally by Maven and is not part of the source repository.

---

## Design Goals

Revision Assistant is designed around a few principles:

- **Data-driven progress** — progress comes from actual topic completion
- **Separation of concerns** — controllers, services, DAOs, models, and database code have distinct responsibilities
- **Practical study workflow** — planning, studying, practising, and tracking are connected
- **Algorithmic components** — graph and optimisation algorithms are integrated into useful application features
- **Offline-first core** — the main study-management features use the local SQLite database
- **Optional external integration** — the Dashboard quote uses a REST API but has a local fallback when the API is unavailable

---

## License

This project is developed as an academic/software-development project.
