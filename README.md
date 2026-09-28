# Revision Assistant

A desktop study management and revision assistant built with **JavaFX, SQLite, and Java**. It helps students organise subjects and topics, plan tasks, follow prerequisite-aware study paths, schedule and record study sessions, prepare for exams, practise with quizzes and flashcards, and track their progress from one place.

---

## Features

| Module | What it does |
|---|---|
| **Authentication** | Local registration and login with password hashing, session handling, logout, and optional remember-me login |
| **Dashboard** | Central overview of progress, pending work, upcoming exams, study activity, today's tasks, subject progress, quiz performance, difficult flashcards, missed quiz questions, and a context-aware "continue" recommendation |
| **Subjects** | Create, edit, delete, select, and colour-code subjects |
| **Topics** | Manage syllabus topics under subjects, track completion, and associate tasks, study sessions, flashcards, and quiz questions with topics |
| **Study Path** | Visual subject → topic learning path with automatic progress calculation, prerequisite-aware ordering, locked topics, current-topic recommendations, and direct study-session logging |
| **Topic Mastery** | Combines topic completion, quiz performance, study history, overdue tasks, upcoming exams, and prerequisite status to identify which incomplete topic deserves attention next |
| **Topic Dependencies** | Create prerequisite relationships between topics, inspect direct/all prerequisites and dependents, generate a dependency-based study order, and detect cycles |
| **Tasks** | Create planned work with subject/topic, title, estimated duration, priority, deadline, and status |
| **Study Planner** | Selects pending Tasks that best fit a given amount of available time using 0/1 Knapsack or Sum of Subsets; priority, deadline urgency, and upcoming-exam relevance affect the priority-based strategy |
| **Exams** | Store exams with dates and associated topics; exam progress is derived from the completion of its topics |
| **Study Sessions** | Record actual study time for a subject/topic, optionally associate a session with a Task, and automatically update the linked Task's progress from the session duration |
| **Flashcards** | Create, edit, delete, filter, and study topic-linked flashcards; mark cards as difficult and track revision status; supports JSON import |
| **Quiz** | Manage topic-linked MCQs, import questions from JSON, take timed quizzes, score attempts, store individual answers, view attempt history, calculate average performance, and identify frequently missed questions |
| **Pomodoro** | Persistent focus/short-break/long-break timer with configurable durations and completed-session counting |
| **Mind Maps** | Create and save interactive study maps with roots, branches, draggable nodes, selectable focus nodes, connections, and persistent positions |
| **Resources** | Save and organise study links by title, URL, description, category, and optional subject |
| **Notifications** | Study, exam, achievement, and streak-related reminders controlled by user preferences |
| **Settings** | Light/dark appearance, font-size preferences, notification settings, and Pomodoro duration settings |

---

## How the Study System Works

Revision Assistant keeps several concepts separate so that planning, learning, and time tracking do not become one giant pile of vaguely related objects.

### Core concepts

- **Subject** — a course or academic area.
- **Topic** — a learning objective within a subject.
- **Task** — a piece of planned work that needs to be completed.
- **Study Planner** — selects useful Tasks for a specified amount of available time.
- **Study Session** — a recorded block of actual study time. A session belongs to a subject/topic and can optionally be linked to a Task.
- **Study Path** — the learning roadmap through Topics, ordered using prerequisite relationships and informed by current study signals.
- **Quiz** — provides evidence about topic understanding.
- **Flashcards** — support active recall and revision.

A topic can therefore have many study sessions:

```
Subject
  └── Topic
       ├── Task 1
       ├── Task 2
       ├── Study Session 1
       ├── Study Session 2
       ├── Quiz Attempts
       └── Flashcards
```

The planner plans **Tasks**; it does not replace the Study Path or turn Topics into Tasks.

### Study Path decision flow

The Study Path uses existing application data to determine useful next topics. Its topic insights consider:

- whether the topic is completed;
- quiz attempts and average score;
- whether the topic has actually been studied;
- incomplete prerequisites;
- overdue Tasks;
- upcoming exams associated with the topic.

Topics blocked by incomplete prerequisites are not selected as the next study recommendation.

---

## Study Planner

The existing Study Planner works on **pending Tasks**.

The user supplies available study time and chooses one of two strategies:

### Priority-Based

Uses **0/1 Knapsack** to maximise the combined value of selected Tasks while staying within the available time.

Task value considers:

- priority;
- deadline urgency;
- whether its subject has an exam within the upcoming relevance window.

### Maximize Time Used

Uses **Sum of Subsets** to find a combination of Tasks that uses as much of the available time as possible, without considering task importance.

This makes the planner an actual algorithmic component of the application rather than a simple sorted task list.

---

## Study Sessions

Study Sessions represent actual study activity.

A session stores:

- subject;
- optional topic;
- optional Task;
- date;
- duration;
- notes.

A session may be created independently for a Topic or associated with a Task. When a session is linked to a Task, its duration updates the Task's status:

- shorter than the Task's estimated duration → **In Progress**;
- meeting or exceeding the estimated duration → **Completed**.

This allows the system to connect planned work with actual study without treating a Task and a Study Session as the same thing.

---

## Exams and Progress

Exams can be associated with multiple Topics.

Exam progress is derived from the completion state of those Topics rather than requiring the user to enter a percentage manually.

For subjects:

```
subject progress =
completed topics / total topics
```

Overall progress is similarly derived from all available topics.

---

## Quizzes and Mastery

Quiz questions can be associated with a Subject and optionally a Topic.

The quiz system supports:

- question management;
- A/B/C/D multiple-choice questions;
- JSON import;
- subject/topic filtering;
- timed quiz sessions;
- automatic submission when the timer expires;
- stored attempts;
- stored individual answers;
- average score calculation;
- frequently missed-question analysis.

Quiz performance is also used by the Topic Mastery system as one of several signals for deciding which incomplete topic deserves attention.

---

## Flashcards

Flashcards can be associated with a Subject and optional Topic.

Each card supports:

- front/back content;
- difficult/not-difficult status;
- revision status;
- subject/topic filtering;
- study mode with card flipping;
- JSON import.

The Dashboard can surface difficult cards and cards that still need revision.

---

## Topic Dependencies

The Study Tools module provides a topic dependency graph.

Users can:

- add prerequisite relationships;
- remove relationships;
- inspect direct prerequisites;
- inspect all prerequisites;
- inspect dependents;
- detect dependency cycles;
- generate a study order.

The Study Path uses this dependency information so that prerequisite relationships are reflected in the learning sequence.

---

## Pomodoro Timer

The Pomodoro tool provides:

- configurable work duration;
- configurable short break;
- configurable long break;
- work/short-break/long-break states;
- completed focus-session counting;
- persistent timer state while navigating between views.

Pomodoro is a focus timer rather than a replacement for the Study Session system.

---

## Mind Maps

The Mind Map tool provides a persistent visual workspace for organising concepts.

Users can:

- create named maps;
- create a root topic;
- add branches;
- select nodes;
- move nodes freely;
- centre the map on a selected node;
- save/load maps;
- delete saved maps.

Node positions and parent relationships are persisted.

---

## Resources

The Resources module stores useful study material with:

- title;
- URL;
- description;
- category;
- optional Subject.

Resources can be filtered and opened directly from the application.

---

## Dashboard

The Dashboard combines information from the application's major systems, including:

- overall and subject progress;
- pending Tasks;
- today's Tasks;
- upcoming Exams;
- study activity;
- recent Study Sessions;
- flashcard revision information;
- difficult flashcards;
- quiz performance;
- frequently missed questions;
- completed Topics;
- a context-aware continuation/recommendation item.

This gives the Dashboard a role beyond displaying static counters: it provides a summary of what has happened and what currently deserves attention.

---

## Technology Stack

- **Java 21 (LTS)**
- **JavaFX 21.0.2** — UI with FXML and CSS
- **SQLite** — embedded local database
- **SQLite JDBC 3.45.3.0**
- **Jackson 2.17.1** — JSON import/deserialization
- **Maven** — build and dependency management

---

## Architecture

The project follows a layered structure:

```
src/main/java/com/revisionassistant/
├── Main.java                   Application entry point
├── algorithm/                  Knapsack, SumOfSubsets, BFS, DFS,
│                               Topological Sort, shortest-path algorithms, etc.
├── api/                        API client/configuration classes
├── controller/                 JavaFX controllers
├── dao/                        SQLite data-access objects
├── database/                   Database initialization and connection handling
├── dto/                        Data-transfer objects
├── model/                      Domain models
├── navigation/                 Application navigation
├── security/                   Password hashing and remember-me support
├── service/                    Business logic and feature integration
├── session/                    Current-user session state
└── util/                       Shared UI/utilities
```

The general application flow is:

```
Controller
    ↓
Service
    ↓
DAO
    ↓
SQLite
```

Algorithms are kept in the algorithm layer and are used by services such as the Study Planner and dependency/study-order features.

---

## Database

SQLite is created automatically in the application's working directory on first launch.

The main persisted entities include:

```
users
subjects
topics
tasks
exams
exam_topics
study_sessions
flashcards
quiz_questions
quiz_attempts
quiz_attempt_answers
topic_dependencies
mind_maps
mind_map_nodes
resources
remember_tokens
```

Progress is derived from stored application data rather than manually entered percentages.

---

## Getting Started

### Prerequisites

- JDK 21 or later
- Maven 3.8+

### Run

```bash
# Clone or extract the project
cd Revision_Assistant

# Run with Maven
mvn javafx:run

# Or build the project
mvn package
```

The SQLite database is created automatically when the application starts.

---

## Main Screens

1. **Login / Register** — account creation and authentication
2. **Dashboard** — progress, tasks, exams, study activity, quizzes, flashcards, and recommendations
3. **Subjects & Topics** — subject and syllabus management
4. **Tasks** — planned work management
5. **Study Planner** — algorithmic Task selection based on available time
6. **Exams** — exam and topic association
7. **Study Sessions** — study-time records
8. **My Study Path** — prerequisite-aware topic roadmap
9. **Flashcards** — flashcard library and study mode
10. **Quiz** — question management and timed quiz mode
11. **Study Tools** — topic dependencies and planner
12. **Pomodoro** — focus timer
13. **Mind Maps** — visual concept mapping
14. **Resources** — study-link organiser
15. **Settings** — appearance, notifications, and timer preferences

---

## Project Status

Revision Assistant is a local-first desktop study application. Its core workflow combines:

```
Subjects & Topics
       ↓
Study Path + Dependencies
       ↓
Tasks → Study Planner
       ↓
Study Sessions
       ↓
Quizzes + Flashcards
       ↓
Progress + Topic Insights
       ↓
Dashboard
```

The application is designed so that the individual study tools share the same underlying subjects, topics, tasks, exams, sessions, and assessment data instead of functioning as completely separate utilities.
