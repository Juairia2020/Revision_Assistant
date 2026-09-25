# Milestone 5A-2 — Login, Registration & User Session

## Scope

Added a small local authentication layer to the Milestone 5A-1 JavaFX application without changing existing study functionality.

## New files

- `src/main/java/com/revisionassistant/model/User.java`
- `src/main/java/com/revisionassistant/dao/UserDAO.java`
- `src/main/java/com/revisionassistant/service/UserService.java`
- `src/main/java/com/revisionassistant/security/PasswordHasher.java`
- `src/main/java/com/revisionassistant/session/CurrentUser.java`
- `src/main/java/com/revisionassistant/navigation/AppNavigator.java`
- `src/main/java/com/revisionassistant/controller/LoginController.java`
- `src/main/java/com/revisionassistant/controller/RegistrationController.java`
- `src/main/resources/com/revisionassistant/fxml/LoginView.fxml`
- `src/main/resources/com/revisionassistant/fxml/RegistrationView.fxml`

## Modified files

- `src/main/java/com/revisionassistant/Main.java` — database initialization remains first; startup now opens LoginView.
- `src/main/java/com/revisionassistant/database/DatabaseManager.java` — creates the new `users` table with `IF NOT EXISTS`.
- `src/main/java/com/revisionassistant/controller/MainController.java` — shows the logged-in user's name/email, protects the shell when no session exists, and adds logout.
- `src/main/resources/com/revisionassistant/fxml/MainView.fxml` — adds the current-user display and Log out action.
- `src/main/resources/com/revisionassistant/css/style.css` — adds reusable authentication form styles.
- `README.md` — documents the authentication architecture and database migration.

## Database migration

The existing SQLite database is reused. Startup now additionally executes:

```sql
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    email TEXT NOT NULL COLLATE NOCASE UNIQUE,
    password_hash TEXT NOT NULL
);
```

No existing table is dropped, altered, or reset. Existing study data remains intact.

## Password handling

Passwords are never stored as plaintext. The JDK's `PBKDF2WithHmacSHA256` is used with a random 16-byte salt, 210,000 iterations, and a 256-bit derived key. The stored representation contains the algorithm marker, iteration count, salt, and derived key.

## Session handling

`CurrentUser` is an in-memory session. It stores only user identity information (ID, name, email), not the password hash. Closing the application clears the in-memory session naturally, so the next launch requires login.

## Navigation

```text
Application start
    -> Database initialization
    -> Login
       -> Register -> create account -> Main shell
       -> Login -> Main shell
    -> Logout -> Login
```

## Constraints respected

- No cloud authentication.
- No OAuth/JWT/external authentication provider.
- No password reset system.
- No new backend feature unrelated to authentication.
- Existing DAOs/services/algorithms/JSON/API/concurrency remain in place.
