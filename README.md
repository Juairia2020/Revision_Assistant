# Revision Assistant

A JavaFX + SQLite desktop app for planning revision. Milestone 1 covered
Subjects and Topics; Milestone 2 adds Tasks, Exams, Study Sessions and a
data-driven Dashboard.

## Requirements

- JDK 26
- IntelliJ IDEA (Community or Ultimate)
- Internet access the first time you open the project, so Maven can download
  the JavaFX and SQLite JDBC dependencies

## Run it

1. Open the project folder in IntelliJ IDEA (`File > Open`, pick this folder).
2. Wait for Maven to finish importing (bottom-right progress bar).
3. Set the Project SDK to JDK 26: `File > Project Structure > Project > SDK`.
4. Open the Maven tool window (right-hand sidebar) and run
   `revision-assistant > Plugins > javafx > javafx:run`.

`revision_assistant.db` is created automatically the first time the app runs,
in the project's working directory.

See the full write-up (including a testing checklist) for more detail.
