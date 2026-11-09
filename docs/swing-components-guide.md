# Swing Component Library — Developer Guide

What this is, how it fits together, and how to use every component without
reading the implementation.

The library lives in `src/main/java/com/example/HMS/GUI`. Screens are built from
small interfaces (`TextBox`, `DropMenu`, `MenuList`, `SideBar`, `DataTable`) and
Swing implementations (`SwingTextBox`, `SwingDropMenu`, …). Create components
through `UI` so screens never call `new SwingXxx()` directly.

---

## 1. Quick start

A minimal screen hosted in the app shell:

```java
UI ui = new UI();

SwingApp app = ui.app("HMS");
JPanel page = new JPanel(new BorderLayout());

SwingTextBox search = ui.textbox().setPrompt("Search…");
SwingDataTable<MangaResponse> table = ui.table();
table.addColumn(Column.of("Title", String.class, MangaResponse::title).width(240))
     .addColumn(Column.of("Year", Short.class, MangaResponse::releaseYear).width(80));
table.setItems(rows);

page.add(search.peer(), BorderLayout.NORTH);
page.add(table.peer(), BorderLayout.CENTER);

app.setContent(page);
app.setStatus("Ready");
app.show();                 // shows on the EDT
```

Everything that changes Swing state belongs on the event dispatch thread (EDT).
`SwingApp.show()` and `SwingApp.center()` already hop to the EDT for you.

---

## 2. Package map

| Package | Contents |
| --- | --- |
| `com.example.HMS.GUI` | `UI` factory, `SwingApp` shell, `Theme`, `SwingLauncher`, `SwingDemo` |
| `…GUI.Components.TextBox` | `TextBox<T>` interface |
| `…GUI.Components.TextBox.impl` | `SwingTextBox` |
| `…GUI.Components.DropMenu` | `DropMenu<T>`, `SwingDropMenu` |
| `…GUI.Components.MenuList` | `MenuList<T>`, `SwingMenuList` |
| `…GUI.Components.SideBar` | `SideBar<T>`, `SwingSideBar`, `Dock`, `Arrangement`, `Alignment`, `ItemWidth` |
| `…GUI.Components.DataTable` | `DataTable<T,P>`, `SwingDataTable<T>`, `Column<T,V>`, `SelectionMode` |
| `…GUI.Components.Form` | `FormDialog<T>` |
| `…GUI.Screens` | `LoginScreen`, `MainScreen` |
| `…GUI.Screens.Manga` | `MangaListScreen`, `MangaFormDialog`, `MangaDataSource`, `ServiceMangaDataSource` |

---

## 3. `UI` — the only factory you need

```java
UI ui = new UI();
```

| Method | Returns | Use |
| --- | --- | --- |
| `app(String title)` | `SwingApp` | Window shell with header/content/status |
| `textbox()` / `textbox(String text)` | `SwingTextBox` | Single-line input |
| `passwordBox()` | `SwingTextBox` | Masked input |
| `dropMenu(String... items)` | `SwingDropMenu` | Select / combo box |
| `menuList(String label)` | `SwingMenuList` | Button + hierarchical popup |
| `sidebar()` | `SwingSideBar` | Navigation rail |
| `table()` | `SwingDataTable<T>` | Typed data table |
| `form(Window owner, String title)` | `FormDialog<T>` | Modal form |

Screens may keep the concrete type for fluent chaining, or declare the interface
at the boundary when portability matters:

```java
TextBox<JTextField> box = ui.textbox();
DataTable<MangaResponse, JPanel> table = ui.table();
SideBar<JScrollPane> nav = ui.sidebar();
```

Every component exposes its Swing peer through `peer()`. Parent it like any
`JComponent`; do not reach into the implementation for layout or state.

---

## 4. `SwingApp` — application shell

**What it is:** a `JFrame` with a title header, a swappable content area, and a
status bar.

```java
SwingApp app = ui.app("HMS");
app.size(1000, 640);
app.minSize(640, 420);
app.title("HMS — Manga Manager");

app.setContent(myScreen);          // replaces the content area
app.addHeaderAction(ui.menuList("Account")); // optional header buttons
app.sidebar(sidebar);              // docks LEFT/RIGHT using SideBar.dock()
app.setStatus("Signed in");

app.center();
app.show();                        // EDT-safe
```

Useful members: `content()`, `frame()`, `setStatus(String)`, `setContent(JComponent)`,
`add(JComponent)` (vertical stack for quick demos), `show()`, `center()`.

---

## 5. `TextBox` — text input

**What it is:** a single-line input with a visual prompt, validation styling,
max length, password mode, and change/submit callbacks. The prompt is painted
only; it never enters the document, so `getText()` never returns it.

```java
SwingTextBox email = ui.textbox()
        .setPrompt("you@example.com")
        .setMaxLength(120)
        .onChange(value -> System.out.println("typing: " + value))
        .onSubmit(this::submit);      // Enter key

email.setError("Email is required"); // red border + tooltip
email.setError(null);                // clear error

email.setEnabled(false);
email.setEditable(true);
email.focus();

String value = email.getText();      // "" when empty, never the prompt
email.clear();
```

Password:

```java
SwingTextBox password = ui.passwordBox();       // or ui.textbox().password(true)
```

API summary: `setText`, `getText`, `clear`, `setPrompt`, `setEditable`,
`setEnabled`, `setError`, `setMaxLength`, `password`, `onChange`, `onSubmit`,
`focus`, `setPosition`, `setSize`, `peer()`.

Notes:

- `setMaxLength(0)` removes the limit.
- `onChange` fires for typing, paste, `setText`, and `clear`.
- `onSubmit` is the Enter key.
- `peer()` returns the underlying `JTextField` (`JPasswordField` internally).

---

## 6. `DropMenu` — select control

**What it is:** a reusable combo box. It can show a placeholder that is never
returned as a real value, and it suppresses selection events while items are
being replaced.

```java
SwingDropMenu status = ui.dropMenu("ongoing", "completed", "hiatus")
        .setPlaceholder("Select status…")
        .onSelect(value -> System.out.println("chosen: " + value));

status.select(1);            // select "completed"
String value = status.selected();   // null while placeholder is shown
int index = status.selectedIndex(); // -1 while placeholder is shown
int count = status.count();         // 3 real items

status.setItems("draft", "published"); // does NOT fire a stale onSelect
status.clearSelection();
status.setEnabled(false);
```

Notes:

- `setItems(...)` resets to the first real item, or the placeholder if present.
- `onSelect` fires only for real item changes, once per change.
- `peer()` returns `JComboBox<String>`.
- A generic `DropMenu<V>` is not implemented; use `Column` formatting or a
  wrapper if a screen ever needs non-string values.

---

## 7. `MenuList` — hierarchical popup menu

**What it is:** a button that opens a popup menu. Build menus with slash paths;
shared prefixes reuse the same submenu.

```java
SwingMenuList actions = ui.menuList("Actions")
        .add("File/New", this::newFile)
        .add("File/Open", this::openFile)
        .add("File/Recent/Last week", this::openRecent)
        .addSeparator()
        .add("Edit/Undo", this::undo)
        .add("Help/About", this::about, false); // disabled item

actions.remove("File/Open");
actions.clear();
actions.setEnabled(false);
app.addHeaderAction(actions.peer());
```

Notes:

- Path segments are trimmed: `"File / New"` becomes `File → New`.
- Actions close the popup before running.
- `add(path, action, false)` creates a disabled item.
- Checkable menu items are not implemented yet.

---

## 8. `SideBar` — navigation rail

**What it is:** a scrollable column/flow of navigation buttons with an active
highlight. Add it through `SwingApp.sidebar(...)` or `MainScreen`.

```java
SwingSideBar nav = ui.sidebar()
        .setWidth(190)
        .setDock(Dock.LEFT)                 // LEFT or RIGHT
        .setArrangement(Arrangement.GRID)   // GRID, BOX_Y, BOX_X, FLOW
        .setItemAlignment(Alignment.LEFT)   // LEFT, CENTER, RIGHT
        .setItemWidth(ItemWidth.FILL)       // FILL or CONTENT
        .setSpacing(6);

nav.addSection("Library");
nav.addItem("Manga", () -> showManga());
nav.addItem("Settings", settingsIcon, () -> showSettings()); // optional Icon
nav.setActive(0);                       // highlight item 0

app.sidebar(nav);                       // dock position comes from nav.dock()
```

Notes:

- `addSection` adds a heading and does not count toward `size()`.
- `setActive(-1)` clears the highlight; out-of-range values also clear it.
- Items overflow vertically inside a `JScrollPane`; `peer()` returns that scroll
  pane.
- Getters: `dock()`, `arrangement()`, `itemAlignment()`, `itemWidth()`,
  `activeIndex()`, `size()`.

---

## 9. `DataTable` — typed table with search, sort, paging, CRUD

**What it is:** an `AbstractTableModel`-backed table for a list of row objects.
It provides columns, selection, search, sorting, paging, and optional
Add/Edit/Delete handling.

### 9.1 Columns

`Column<T,V>` describes how to read and display a property of a row.

```java
SwingDataTable<MangaResponse> table = ui.table();

table.addColumn(Column.of("Title", String.class, MangaResponse::title)
                        .width(240))
     .addColumn(Column.of("Status", String.class, MangaResponse::status)
                        .formatter(value -> value == null ? "—" : value.toUpperCase())
                        .width(110))
     .addColumn(Column.of("Year", Short.class, MangaResponse::releaseYear)
                        .comparator(Comparator.nullsLast(Short::compare))
                        .width(70));
```

`Column` options:

| Method | Purpose |
| --- | --- |
| `formatter(Function<V,String>)` | Display text; also used by search |
| `comparator(Comparator<V>)` | Sort order; defaults to `Comparable`, then strings |
| `renderer(TableCellRenderer)` | Custom cell rendering |
| `editor(TableCellEditor)` | Custom cell editing (cells are read-only by default) |
| `width(int)` | Preferred column width |

There is also the shorthand `addColumn(title, type, getter)`.

### 9.2 Data and selection

```java
table.setItems(rows);          // replaces all rows, resets to page 0
table.addItem(row);
table.updateItem(index, row);  // index is the data index, not the visible row
table.removeItem(index);
table.clear();

List<MangaResponse> all = table.items();
MangaResponse one = table.itemAt(0);
int total = table.count();
int visible = table.filteredCount();

table.setSelectionMode(SelectionMode.SINGLE); // NONE, SINGLE, MULTIPLE
table.onSelect(row -> status.setText(row.title()));
table.onDoubleClick(this::editRow);
```

Selection is cleared whenever the visible page is rebuilt. That keeps callbacks
predictable after search/sort/page/refresh.

### 9.3 Search, sort, paging

```java
table.setSearchText("piece");  // filters across every Column.formatter value
table.setSortable(true);

table.setPageSize(10);         // footer also offers 5 / 10 / 20
table.setPageIndex(2);
table.nextPage();
table.previousPage();

int page = table.currentPage();  // zero-based
int pages = table.pageCount();
```

Users sort by clicking a column header; the active column shows ▲/▼. For code
and tests, use:

```java
table.sortBy(columnIndex, true);   // ascending
table.sortBy(columnIndex, false);  // descending
```

Page index is clamped automatically when filtering or deletion shrinks the
result set.

### 9.4 CRUD

Enable the built-in toolbar and row context menu:

```java
table.setCrudEnabled(true);

table.setCreateHandler(() -> {
    Optional<MangaResponse> created = openCreateDialog();
    return created;              // present: table appends it
});

table.setEditHandler(existing -> {
    Optional<MangaResponse> edited = openEditDialog(existing);
    return edited;               // present: table replaces the row
});

table.setDeleteHandler(row -> {
    repository.delete(row.id()); // called after the user confirms
});
```

Contracts:

- `setCreateHandler(Supplier<Optional<T>>)` — return the new row to append it
  immediately, or `Optional.empty()` when saving is asynchronous.
- `setEditHandler(Function<T, Optional<T>>)` — return the edited row to replace
  it immediately, or empty for asynchronous saves.
- `setDeleteHandler(Consumer<T>)` — runs after confirmation, before the local
  row is removed. Use it to persist the deletion.
- Buttons enable/disable themselves from the current selection.
- The delete confirmation dialog is built in.

For asynchronous persistence, return `Optional.empty()` and reload with
`table.setItems(...)` in the `SwingWorker.done()` (see `MangaListScreen`).

### 9.5 Parent it

```java
panel.add(table.peer(), BorderLayout.CENTER);
```

`peer()` is a `JPanel` containing the search toolbar, the table, and the pager.

---

## 10. `FormDialog` — modal forms

**What it is:** a small form builder with label/input rows, validation errors,
OK/Cancel, Enter=OK, and Esc=Cancel. It returns `Optional<T>`.

```java
SwingTextBox title = ui.textbox(existing == null ? "" : existing.title());
SwingTextBox year = ui.textbox(existing == null ? "" : String.valueOf(existing.releaseYear()));
SwingDropMenu status = ui.dropMenu("ongoing", "completed", "hiatus")
        .setPlaceholder("Select status")
        .select(0);

FormDialog<MangaRequest> dialog = ui.form(ownerWindow, "Manga")
        .addText("Title *", title)
        .addText("Year", year)
        .addDrop("Status", status)
        .validate(() -> {
            if (title.getText().isBlank()) {
                return "Title is required.";
            }
            if (!year.getText().isBlank() && parseYear(year.getText()) == null) {
                return "Year must be a number.";
            }
            return null;
        })
        .result(() -> Optional.of(new MangaRequest(
                title.getText().trim(),
                null, null, null, null, null,
                status.selected(),
                "manga",
                year.getText().isBlank() ? null : parseYear(year.getText()))));

Optional<MangaRequest> result = dialog.show();
```

Notes:

- `addText`, `addDrop`, and `addField` add one row each.
- `validate` returns an error message or `null`/blank when valid.
- `result` builds the value on OK. Returning `Optional.empty()` keeps the dialog
  open and shows a generic message.
- `show()` is modal and must be called on the EDT.
- Assign to a typed `FormDialog<T>` variable before chaining; this avoids
  needing an explicit type witness on `ui.form(...)`.

---

## 11. Screens and Spring integration

### `LoginScreen`

```java
LoginScreen login = new LoginScreen(ui);
login.onSubmit(credentials -> showMain(credentials));
```

Validates email format and a password length of at least 4 characters. It owns
no authentication backend; the callback is the integration point.

### `MainScreen`

```java
MainScreen main = new MainScreen(ui);
main.add("Manga", new MangaListScreen(ui, app.frame(), dataSource));
main.add("Settings", settingsPanel);
app.setContent(main);
```

Provides a sidebar plus a `CardLayout` content panel. `add(label, screen)` adds a
nav item and screen; `select(index)` switches programmatically.

### `MangaListScreen`

```java
MangaListScreen screen = new MangaListScreen(ui, app.frame(), dataSource);
screen.refresh();   // reloads in a SwingWorker
```

Handles load/create/update/delete, status messages, duplicate-slug errors, and
client-side search/sort/paging. It never blocks the EDT.

### `MangaFormDialog`

```java
Optional<MangaRequest> create = MangaFormDialog.showCreate(owner, ui);
Optional<MangaRequest> edit = MangaFormDialog.showEdit(owner, ui, existing);
```

Fields: title, slug, author, artist, status, type, release year. Preserves
description/cover URL on edit even though they are not edited.

### `MangaDataSource`

The UI depends on this seam, not on Spring directly:

```java
public interface MangaDataSource {
    List<MangaResponse> findAll();
    MangaResponse create(MangaRequest request);
    MangaResponse update(String id, MangaRequest request);
    void delete(String id);
}
```

`ServiceMangaDataSource` adapts `MangaService` (Option A: same JVM). Swap in a
fake for tests or a REST client for another backend.

### Launcher

`SwingLauncher` boots the Spring context with `WebApplicationType.NONE`, gets
`MangaService`, wraps it in `ServiceMangaDataSource`, shows the login screen, and
switches to `MainScreen` after submit.

---

## 12. Threading rules

1. Create and mutate Swing components on the EDT.
2. Never call the database or network on the EDT.
3. Use `SwingWorker` for I/O and apply results in `done()`.

```java
new SwingWorker<List<MangaResponse>, Void>() {
    @Override
    protected List<MangaResponse> doInBackground() {
        return dataSource.findAll();
    }

    @Override
    protected void done() {
        try {
            table.setItems(get());
            status.setText("Loaded");
        } catch (Exception ex) {
            showError(ex);
        }
    }
}.execute();
```

`SwingWorker.done()` runs on the EDT, so table updates there are safe.

---

## 13. Theme and styling

`Theme.install()` applies the system look and feel plus consistent colors, fonts,
and table defaults. `SwingApp` calls it automatically. Available constants
include `BACKGROUND`, `SURFACE`, `BORDER`, `TEXT`, `MUTED_TEXT`, `ACCENT`,
`DANGER`, `FONT`, `FONT_BOLD`, `TITLE_FONT`, `GAP`, and `PAD`.

```java
label.setFont(Theme.FONT_BOLD);
panel.setBorder(Theme.cardBorder());
panel.setBackground(Theme.SURFACE);
```

There is no FlatLaf dependency; the theme is a small UIManager customization.

---

## 14. Testing components

Tests run headless. Do not create `JFrame` or `JDialog` in tests; instantiate
components and run mutations on the EDT:

```java
SwingUtilities.invokeAndWait(() -> {
    SwingTextBox box = new SwingTextBox().setPrompt("Email");
    box.setText("a@b.com");
    assertThat(box.getText()).isEqualTo("a@b.com");
});
```

Existing examples:

- `SwingTextBoxTest` — prompt, max length, password, callbacks.
- `SwingDropMenuTest` — placeholder, stale-event suppression.
- `SwingMenuListTest` — shared prefixes, remove, separators.
- `SwingSideBarTest` — sections, active state, layout options.
- `SwingDataTableTest` — columns, search, sort, paging, clamping, selection.
- `ServiceMangaDataSourceTest` — delegation to `MangaService`.
- `UiSmokeTest` — factories and screen construction without a display.

---

## 15. Cookbook

### Add a new screen to the desktop app

1. Create a `JPanel` subclass under `…GUI.Screens`.
2. Build controls with `UI` and parent `peer()` components.
3. Register it in `MainScreen`:

```java
main.add("Reports", new ReportsScreen(ui));
```

4. Use `SwingWorker` for any I/O.

### Add a CRUD table screen

1. Define a data source interface (like `MangaDataSource`).
2. Build the table with `ui.table()` and `Column.of(...)`.
3. `setItems` after loading.
4. Wire `setCreateHandler`, `setEditHandler`, `setDeleteHandler`.
5. Use `FormDialog` for the create/edit form.
6. Reload in `SwingWorker.done()` when persistence is asynchronous.

### Add a form field

Build the input with `UI`, then add it to `FormDialog`:

```java
SwingTextBox isbn = ui.textbox();
dialog.addText("ISBN", isbn);
```

Read `isbn.getText()` inside `result(...)`.

---

## 16. Running and troubleshooting

```bash
./grad.sh ui-demo     # component showcase, no database
./grad.sh ui          # Spring + login + Manga CRUD (needs MySQL/.env)
./grad.sh test        # headless tests
./grad.sh build       # full build
```

| Symptom | Cause / fix |
| --- | --- |
| `HeadlessException` | A frame/dialog was created without a display. Use the demo command or add `-Djava.awt.headless=false` on a desktop. |
| Spring context fails at startup | Check MySQL and `.env` (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`). The launcher shows the root cause. |
| `getText()` looks empty after setting a prompt | Correct: prompts are paint-only and never become values. |
| Search/sort seems to reset | `setItems` resets to page 0; reapply `setSearchText`/`sortBy` after a reload if needed. |
| Delete handler and row state disagree | `setDeleteHandler` runs before the local row is removed; reload after an asynchronous delete. |

---

## 17. Conventions

- Create components through `UI`; screens should not instantiate `SwingXxx`.
- Keep fluent mutators returning `this` / the interface.
- Use `peer()` to parent a component; do not reach inside it.
- Inject behavior through handlers; components contain no business logic.
- Use `Optional.empty()` from a create/edit handler when persistence is async.
- Keep UI work on the EDT and I/O in `SwingWorker`.
- Package names stay as-is: `HMS`, `GUI`, `Components`.
