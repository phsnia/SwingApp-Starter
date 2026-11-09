# Java Swing Component Plan — Simple to Complex

Phased implementation plan for the `com.example.HMS.GUI` component library and the
screens built from it.

Order: each phase depends only on earlier phases, starting with the smallest
building blocks and ending with full screens.

---

## Current state (audit)

| File | Status |
| --- | --- |
| `GUI/SwingApp.java` | Frame shell exists, but imports/component interfaces are missing |
| `GUI/UI.java` | Factory exists, but `SwingTextBox` is not imported and only textbox is exposed |
| `Components/TextBox/TextBox.java` | Interface exists |
| `Components/TextBox/impl/SwingTextBox.java` | Implementation exists but **has no package declaration** |
| `Components/DropMenu/SwingDropMenu.java` | Implements missing `DropMenu` |
| `Components/MenuList/SwingMenuList.java` | Implements missing `MenuList` |
| `Components/SideBar/SwingSideBar.java` | Implements missing `SideBar`, `Dock`, `Arrangement`, `Alignment`, `ItemWidth` |
| `Components/DataTable/Column.java` | Exists |
| `Components/DataTable/SelectionMode.java` | Exists |
| `Components/DataTable/SwingDataTable.java` | Implements missing `DataTable` |
| `Components/Core/Core.md` | Empty placeholder |

**Blocking problem:** `./gradlew compileJava` fails with `cannot find symbol` for
the missing interfaces/enums. Phase 0 fixes that before any feature work.

---

## Component dependency graph

```
Core (enums, base helpers)
   │
   ├── TextBox ─────────────┐
   ├── DropMenu             │
   ├── MenuList             │
   ├── SideBar              │
   └── DataTable ───────────┤
                            ▼
                    App shell / UI factory
                            │
                            ▼
                     Screens (Login, Manga)
```

DataTable needs `Column`, `SelectionMode`, and a table model. Screens need all
of the above.

---

## Phase 0 — Baseline compilation (smallest)

**Goal:** `./gradlew compileJava` passes with the existing implementations.

### Tasks

- [ ] Create `Components/SideBar/Dock.java` with `LEFT`, `RIGHT`.
- [ ] Create `Components/SideBar/Arrangement.java` with `GRID`, `BOX_Y`, `BOX_X`, `FLOW`.
- [ ] Create `Components/SideBar/Alignment.java` with `LEFT`, `CENTER`, `RIGHT`.
- [ ] Create `Components/SideBar/ItemWidth.java` with `FILL`, `CONTENT`.
- [ ] Create `Components/SideBar/SideBar.java` interface matching the methods
      `SwingSideBar` already overrides.
- [ ] Create `Components/DropMenu/DropMenu.java` interface matching
      `SwingDropMenu`.
- [ ] Create `Components/MenuList/MenuList.java` interface matching
      `SwingMenuList`.
- [ ] Create `Components/DataTable/DataTable.java` interface matching
      `SwingDataTable` (columns, items, CRUD handlers, selection, search,
      sorting, paging, `peer()`).
- [ ] Add `package com.example.HMS.GUI.Components.TextBox.impl;` to
      `SwingTextBox.java`.
- [ ] Add imports to `SwingApp.java` for `SideBar`, `Dock`, `TextBox`, `DropMenu`,
      `MenuList`, `DataTable`.
- [ ] Add imports to `UI.java` for `SwingTextBox` (and later the other impls).
- [ ] Delete the unused `.md` placeholders or leave them for per-component notes.

### Verify

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`.

**Complexity:** 1/10 — mechanical interface extraction.

---

## Phase 1 — App shell and UI factory (simple)

**Goal:** One place creates every component, and the frame is ready to host
screens.

### Tasks

- [ ] Expand `UI` with fluent factories:
      `app(title)`, `textbox()`, `dropMenu(...)`, `menuList(label)`,
      `sidebar()`, `table()`.
- [ ] Add to `SwingApp`:
      - a header/title area,
      - a content area that can swap screens,
      - a footer/status bar,
      - `setContent(JComponent)`,
      - `center()` / `show()` on the EDT.
- [ ] Add a `Main`/`SwingLauncher` class that opens a demo frame.
- [ ] Add a Gradle `runUi` task (JavaExec) or `./grad.sh ui` alias that runs the
      Swing app without starting Spring Boot.
- [ ] Verify the frame opens and closes cleanly.

### Verify

```bash
./grad.sh ui
```

Expected: empty themed window with header/content/status areas.

**Complexity:** 2/10.

---

## Phase 2 — TextBox (simple)

**Goal:** A stable text input used by every form.

### Tasks

- [ ] Fix package and naming (`SwingTextBox`).
- [ ] Keep the fluent API: `setText`, `getText`, `clear`, `setPrompt`,
      `setEditable`, `onChange`, `focus`.
- [ ] Fix the placeholder implementation: use a proper hint that never becomes
      the actual value and never leaks into `getText()`.
- [ ] Add disabled/error visual states (`setEnabled`, `setError(String)`).
- [ ] Add `maxLength` support via `DocumentFilter`.
- [ ] Add a password variant (`SwingPasswordBox`) or a `password(boolean)` flag.
- [ ] Handle Enter key: `onSubmit(Consumer<String>)`.
- [ ] Expose the underlying `JTextField` only through `peer()`.

### Verify

- Demo form with normal, prompt, disabled, error, and password fields.
- Typing and clearing never returns the prompt text from `getText()`.

**Complexity:** 2/10.

---

## Phase 3 — DropMenu (simple)

**Goal:** A reusable dropdown/select.

### Tasks

- [ ] Define `DropMenu<T extends JComponent>` with `setItems`, `select`,
      `selected`, `onSelect`, `peer`.
- [ ] Keep `SwingDropMenu` string-based for now.
- [ ] Add `setEnabled`, `clearSelection`, `selectedIndex`, `count`.
- [ ] Add an optional placeholder item that is not returned as a real selection.
- [ ] Ensure `onSelect` does not fire while the model is being replaced.
- [ ] Add a generic `SwingDropMenu<V>` variant only if a screen needs non-string
      values (defer if not needed).

### Verify

- Demo: changing the selection fires once; programmatic `setItems` does not
  fire a stale selection event.

**Complexity:** 2/10.

---

## Phase 4 — MenuList (simple → medium)

**Goal:** Button + hierarchical popup menu, usable for actions and navigation.

### Tasks

- [ ] Define `MenuList<T extends JComponent>`.
- [ ] Keep `add("File/New", action)` path syntax; trim each segment.
- [ ] Add `addSeparator()`, `add(path, action, enabled)`, `remove(path)`,
      `clear`.
- [ ] Close the popup after an action runs.
- [ ] Support disabled items and optional checkable items (later).
- [ ] Reuse an existing `JMenu` when the same path prefix is added twice.

### Verify

- Demo: nested menu, duplicate prefixes merge, actions fire once, separators
  render.

**Complexity:** 3/10.

---

## Phase 5 — SideBar (medium)

**Goal:** Navigation rail with configurable layout.

### Tasks

- [ ] Define `SideBar<T extends JComponent>` and the four enums.
- [ ] Implement all existing options: width, spacing, dock, arrangement,
      alignment, item width.
- [ ] Add selection/active state (`setActive(int)`, highlight selected item).
- [ ] Add optional icons and grouped sections.
- [ ] Add a scroll pane fallback when items exceed the height.
- [ ] Keep `dock()`/`arrangement()`/`itemAlignment()`/`itemWidth()` getters.
- [ ] Recompute layout on every mutator (already partially done).

### Verify

- Demo: LEFT/RIGHT, GRID/BOX/FLOW, FILL/CONTENT, active item, overflow scroll.

**Complexity:** 4/10.

---

## Phase 6 — DataTable part 1: core table (complex)

**Goal:** Data + columns + selection working before adding features.

### Tasks

- [ ] Define `DataTable<T, P extends JComponent>`.
- [ ] Keep `Column<T,V>` (`title`, `valueType`, `getter`, formatter,
      comparator, renderer, editor, width).
- [ ] Implement `AbstractTableModel` over the current page:
      row count, column count/name/class, value lookup.
- [ ] Implement `setItems`, `addItem`, `updateItem`, `removeItem`, `clear`,
      `items`, `itemAt`, `count`.
- [ ] Implement `onSelect` and `onDoubleClick`.
- [ ] Implement `setSelectionMode(NONE/SINGLE/MULTIPLE)`.
- [ ] Add a demo with a `Manga`-like record (id, title, status, year).

### Verify

- Demo table shows rows and columns.
- Selection callback returns the correct item.
- Double-click returns the correct item.

**Complexity:** 5/10.

---

## Phase 7 — DataTable part 2: search, sort, paging

**Goal:** Large datasets stay usable.

### Tasks

- [ ] Search box filters across all columns via `Column.format`.
- [ ] Header click toggles ascending/descending; show the sort indicator.
- [ ] Page size selector (`5/10/20`), first/prev/next/last, page slots with
      `...`.
- [ ] Clamp `pageIndex` when filtering reduces the page count.
- [ ] Keep selection cleared or restored predictably after refresh.
- [ ] Expose `setSearchText`, `setPageSize`, `setPageIndex`, `nextPage`,
      `previousPage`, `currentPage`, `pageCount`, `filteredCount`.

### Verify

- Search + sort + paging combine correctly.
- Deleting the last row of the last page moves to the previous page.

**Complexity:** 6/10.

---

## Phase 8 — DataTable part 3: CRUD UX (complex)

**Goal:** Add/Edit/Delete from the table without custom glue per screen.

### Tasks

- [ ] Toolbar Add button + row context menu Edit/Delete.
- [ ] `setCreateHandler(Supplier<Optional<T>>)`,
      `setEditHandler(Function<T, Optional<T>>)`, `setCrudEnabled`.
- [ ] Enable/disable buttons based on selection.
- [ ] Confirmation dialog before delete.
- [ ] Build a reusable `FormDialog<T>` component:
      - field rows (label + TextBox/DropMenu),
      - validation errors,
      - OK/Cancel,
      - returns `Optional<T>`.
- [ ] Reuse `FormDialog` in the create/edit handlers.

### Verify

- Add opens a form and appends the item.
- Edit prefills and replaces the item.
- Delete confirms and removes the item.
- Cancel changes nothing.

**Complexity:** 7/10.

---

## Phase 9 — Screens and integration (most complex)

**Goal:** Real screens on top of the component library.

### Tasks

- [ ] `LoginScreen`: email/password fields, validation, submit.
- [ ] `MainScreen`: sidebar navigation + swappable content panel.
- [ ] `MangaListScreen`: `DataTable` with Manga columns, search, paging, CRUD.
- [ ] `MangaFormDialog`: title, slug, author, artist, status, type, year.
- [ ] Decide data source:
      - **Option A (simplest):** run the Spring context in the same JVM and call
        `MangaService` directly.
      - **Option B:** call the REST API (`/api/manga`) with `HttpClient`.
      - Prefer A for the desktop app; keep the REST API for the web client.
- [ ] Threading rules:
      - UI changes on the EDT only.
      - DB calls in `SwingWorker`, results applied on `done()`.
      - Show a busy/status message during calls.
- [ ] Map `MangaResponse` → table rows and `MangaRequest` ← form values.
- [ ] Handle errors: not found, duplicate slug, validation messages.

### Verify

- Login → Manga list → create/edit/delete against MySQL.
- No UI freeze during database calls.
- Duplicate slug shows a readable error.

**Complexity:** 9/10.

---

## Phase 10 — Theme, tests, packaging

**Goal:** Make it presentable and maintainable.

### Tasks

- [ ] Add FlatLaf (or a small custom L&F) and a central `Theme` class for
      colors, fonts, spacing, and borders.
- [ ] Add icons (SVG/PNG) and high-DPI friendly sizing.
- [ ] Add keyboard shortcuts: Enter=submit, Esc=cancel, Ctrl+F=search.
- [ ] Tests:
      - headless-friendly component tests on the EDT,
      - `DataTable` logic tests (search/sort/paging) without showing a window,
      - optional AssertJ Swing for UI smoke tests.
- [ ] Add `./grad.sh ui` and `./grad.sh build` to CI.
- [ ] Optional: `jpackage` for a native installer.

### Verify

- `./gradlew test` passes headless.
- App looks consistent at 100% and 150% scaling.

**Complexity:** 6/10 (spread across all components).

---

## Phase summary

| Phase | Component | Complexity | Depends on |
| --- | --- | --- | --- |
| 0 | Baseline interfaces | 1 | — |
| 1 | App shell + UI factory | 2 | 0 |
| 2 | TextBox | 2 | 0, 1 |
| 3 | DropMenu | 2 | 0, 1 |
| 4 | MenuList | 3 | 0, 1 |
| 5 | SideBar | 4 | 0, 1 |
| 6 | DataTable core | 5 | 0, 1 |
| 7 | DataTable search/sort/page | 6 | 6 |
| 8 | DataTable CRUD + FormDialog | 7 | 6, 7, 2, 3 |
| 9 | Screens + integration | 9 | 1–8 |
| 10 | Theme, tests, packaging | 6 | all |

Suggested order to demo early: `0 → 1 → 2 → 3 → 6 → 7 → 8 → 4 → 5 → 9 → 10`.
MenuList and SideBar can wait until navigation is needed.

---

## Conventions to keep

- **Interface + implementation per component**, e.g. `TextBox<T>` →
  `SwingTextBox`. Screens depend on the interface.
- **Fluent mutators** returning `this`.
- **`peer()`** exposes the underlying Swing component; screens avoid touching
  Swing internals directly.
- **EDT discipline:** create and mutate UI on the event dispatch thread.
- **No business logic in components.** Handlers are injected by the screen.
- **Package names stay as-is** (`HMS`, `GUI`, `Components`) to match existing
  code; renaming is a separate refactor.

## Definition of done per phase

1. `./gradlew compileJava` passes.
2. The phase's demo works.
3. New behavior has at least one test or a documented manual check.
4. No component reaches into another component's internals.

---

## Implementation status (Nov 2026)

See [`swing-components-guide.md`](swing-components-guide.md) for per-component
usage, API summaries, threading rules, and a cookbook.

Phases 0–10 are implemented in `src/main/java/com/example/HMS/GUI`.

| Area | What landed |
| --- | --- |
| Phase 0 | Interfaces/enums: `SideBar`, `Dock`, `Arrangement`, `Alignment`, `ItemWidth`, `DropMenu`, `MenuList`, `DataTable`; package declaration fixed; imports added. `compileJava` is green. |
| Phase 1 | `UI` factories, header/content/footer shell, `setContent`, `center`/`show`, `SwingLauncher`, `SwingDemo`, `runUi`/`runUiDemo` tasks and `./grad.sh ui` / `ui-demo`. |
| Phase 2 | Prompt is paint-only (never in the document), `setError`, `setEnabled`, `setMaxLength` via `DocumentFilter`, `password(boolean)`, `onSubmit`, `peer()`. |
| Phase 3 | Placeholder that is never returned, guarded `onSelect`, `setEnabled`, `clearSelection`, `selectedIndex`, `count`. |
| Phase 4 | `add(path, action, enabled)`, `addSeparator`, `remove`, popup closes after actions, shared prefix menus. |
| Phase 5 | LEFT/RIGHT, GRID/BOX_X/BOX_Y/FLOW, FILL/CONTENT, active highlight, icons, sections, scroll-pane overflow. |
| Phase 6 | Typed `DataTable` / `Column`, `AbstractTableModel` over the current page, CRUD list methods, selection modes, select/double-click callbacks. |
| Phase 7 | Formatted search, sort toggle with header indicator, 5/10/20 page sizes, first/prev/next/last, `...` slots, clamping, predictable selection reset. |
| Phase 8 | Add toolbar + row context menu, create/edit/delete handlers, enabled-state rules, delete confirmation, reusable `FormDialog`. |
| Phase 9 | `LoginScreen`, `MainScreen`, `MangaListScreen`, `MangaFormDialog`, `MangaDataSource` + `ServiceMangaDataSource` (Option A), `SwingWorker` calls, DTO mapping, readable errors. |
| Phase 10 | UIManager `Theme` (no FlatLaf dependency), Enter/Esc/Ctrl+F shortcuts, headless component/table tests, `grad.sh ui`/`ui-demo`. |

Deviations / deferred items:

- No FlatLaf or icon assets; `Theme` uses UIManager defaults and system fonts.
- No `jpackage` installer and no CI workflow file (the `runUi`, `runUiDemo`
  and `test` tasks are ready to wire into CI).
- Generic `DropMenu<V>` was not needed; the string variant plus `Column`
  accessors cover the screens.
- `DataTable` gained `setDeleteHandler` and `sortBy(...)` to make persistence
  and headless sort tests possible.

Manual checks:

```bash
./grad.sh ui-demo   # every component, no database
./grad.sh ui        # Spring context + login + Manga CRUD (needs MySQL/.env)
./grad.sh test      # headless tests, including DataTable search/sort/paging
```
