# HMS — Manga Manager

Spring Boot REST API plus a Java Swing desktop client built from a small,
interface-first component library.

## Documentation

- [Swing component library — developer guide](docs/swing-components-guide.md) —
  what each component is, its API, and usage examples.
- [Swing component implementation plan](docs/swing-components-plan.md) — phased
  build plan and current status.
- [Basic setup](docs/basic-setup.md)
- [SQL-to-model workflow](docs/sql-to-model-workflow.md)

## Run

```bash
./grad.sh ui-demo     # Swing component showcase (no database)
./grad.sh ui          # Spring + Swing desktop app (needs MySQL/.env)
./grad.sh run         # REST API only
./grad.sh test        # Headless tests
./grad.sh build       # Full build
```

Configuration is read from environment variables (or `.env`); see
`.env.example` for the database settings.

## Desktop client

The desktop app boots the Spring context in the same JVM, opens a login screen,
and then the Manga library screen:

- Login: `com.example.HMS.GUI.Screens.LoginScreen`
- Navigation: `com.example.HMS.GUI.Screens.MainScreen`
- Manga CRUD: `com.example.HMS.GUI.Screens.Manga.MangaListScreen`
- Entry point: `com.example.HMS.GUI.SwingLauncher`

Component examples live in `com.example.HMS.GUI.SwingDemo` and the
[developer guide](docs/swing-components-guide.md).
