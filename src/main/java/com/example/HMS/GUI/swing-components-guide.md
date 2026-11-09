# Swing Component Library — Developer Guide

What this is, how it fits together, and how to use every component without
reading the implementation.

The library lives in `src/main/java/com/example/HMS/GUI`. Screens are built from
small interfaces (`TextBox`, `DropMenu`, `MenuList`, `SideBar`, `DataTable`,
`FileDropZone`) and
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
| `com.example.HMS.GUI` | `SwingLauncher`, `SwingDemo`, this guide |
| `…GUI.Components.Core` | `UI` factory, `SwingApp` shell, `Theme` |
| `…GUI.Components.TextBox` | `TextBox<T>` interface, `SwingTextBox` |
| `…GUI.Components.DropMenu` | `DropMenu<T>`, `SwingDropMenu` |
| `…GUI.Components.MenuList` | `MenuList<T>`, `SwingMenuList` |
| `…GUI.Components.SideBar` | `SideBar<T>`, `SwingSideBar`, `Dock`, `Arrangement`, `Alignment`, `ItemWidth` |
| `…GUI.Components.DragDrop` | `FileDropZone<P>`, `SwingFileDropZone` |
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
| `fileDropZone()` | `FileDropZone<JPanel>` | File-drop target with image preview |
| `table()` | `SwingDataTable<T>` | Typed data table |
| `form(Window owner, String title)` | `FormDialog<T>` | Modal form |

Screens may keep the concrete type for fluent chaining, or declare the interface
at the boundary when portability matters:

```java
TextBox<JTextField> box = ui.textbox();
DataTable<MangaResponse, JPanel> table = ui.table();
SideBar<JScrollPane> nav = ui.sidebar();
FileDropZone<JPanel> drop = ui.fileDropZone();
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
app.changeDock(sidebar, Dock.RIGHT); // move an already-added sidebar
app.setStatus("Signed in");

app.center();
app.show();                        // EDT-safe
```

### 4.1 API

| Method | Description |
| --- | --- |
| `new SwingApp(String title)` | Builds the frame, installs `Theme`, starts at 960×600 with a 640×420 minimum. |
| `size(int w, int h)` | Sets the frame size. |
| `minSize(int w, int h)` | Sets the minimum frame size. |
| `title(String)` | Updates the frame title and the header label. |
| `setContent(JComponent)` | Replaces the content area; header, sidebar, and status bar stay. |
| `content()` | Returns the content panel. |
| `setStatus(String)` | Updates the status bar; `null` becomes an empty string. |
| `addHeaderAction(JComponent)` | Adds a component to the right side of the header. |
| `sidebar(SideBar<T>)` | Adds the rail at the side returned by `SideBar.dock()` (`WEST`/`EAST`). |
| `changeDock(SideBar<T>, Dock)` | Updates `dock()` and moves an already-added rail to `WEST`/`EAST`. |
| `sidebar(JComponent)` / `sidebar(JComponent, boolean right)` | Adds a raw component to the left or right edge. |
| `add(...)` overloads | Adds a text box, drop menu, menu list, table, or raw component to the vertical stack. The `zone` overloads add to the shell's `BorderLayout` zones instead. |
| `center()` | Centers the frame on screen; safe to call from any thread. |
| `show()` | Makes the frame visible; safe to call from any thread. |
| `frame()` | Returns the underlying `JFrame`. |

Fluent methods return `this`. Constants: `DEFAULT_WIDTH`, `DEFAULT_HEIGHT`,
`MIN_WIDTH`, `MIN_HEIGHT`.

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

### 5.1 API

| Method | Description |
| --- | --- |
| `setPosition(double x, double y)` | Moves the peer to pixel coordinates. Normal layout code should let the layout manager position it instead. |
| `setSize(double w, double h)` | Sets the peer's preferred width/height. Minimum width becomes `0`; minimum height stays `h`. |
| `getText()` | Returns the current text (`""` when empty), never the painted prompt. |
| `setText(String)` | Replaces the text; `null` is treated as `""`. |
| `clear()` | Clears the text. |
| `setPrompt(String)` | Sets the paint-only placeholder. |
| `setEditable(boolean)` | Allows or blocks user editing. |
| `setEnabled(boolean)` | Enables or disables the field. |
| `setError(String)` | Adds a red border and tooltip; `null` or blank clears the error. |
| `setMaxLength(int)` | Limits the document length; `0` removes the limit. Negative values throw `IllegalArgumentException`. |
| `password(boolean)` | Masks or unmasks the text. |
| `onChange(Consumer<String>)` | Registers a listener for typing, paste, `setText`, and `clear`. |
| `onSubmit(Consumer<String>)` | Registers the Enter-key handler. |
| `focus()` | Requests focus in the window. |
| `peer()` | Returns the underlying `JTextField` (a `JPasswordField` internally). |

Fluent methods return `this` (`SwingTextBox`).

Notes:

- `setMaxLength(0)` removes the limit.
- `onChange` fires for typing, paste, `setText`, and `clear`.
- `onSubmit` is the Enter key.
- `peer()` returns the underlying `JTextField` (`JPasswordField` internally).
- `setSize(w, h)` only sets the peer's **preferred** size (minimum width `0`).
  A `GridLayout` or `BorderLayout.CENTER` parent will still stretch it; see
  [Layout managers and sizing](#9-layout-managers-and-sizing).
- Fields start with 24 columns, so they have a usable natural width in
  preferred-size layouts. Use a consistent `setSize(320, 32)` in forms when you
  want every field to line up.

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

### 6.1 API

| Method | Description |
| --- | --- |
| `setItems(String... items)` | Replaces the real items. Selection resets to the placeholder or the first item; `onSelect` is not fired. |
| `select(int index)` | Selects a zero-based real item. Out-of-range values clear the selection. |
| `selected()` | Returns the selected real item, or `null` for the placeholder/no selection. |
| `selectedIndex()` | Returns the selected real item index, or `-1` for the placeholder/no selection. |
| `count()` | Returns the number of real items, excluding the placeholder. |
| `clearSelection()` | Shows the placeholder if one exists, otherwise clears the selection. |
| `setEnabled(boolean)` | Enables or disables the combo box. |
| `setPlaceholder(String)` | Adds a leading hint item that is never returned by `selected()`. |
| `onSelect(Consumer<String>)` | Registers a listener that fires only for real selection changes. |
| `peer()` | Returns the underlying `JComboBox<String>`. |

Fluent methods return `this` (`SwingDropMenu`).

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

### 7.1 API

| Method | Description |
| --- | --- |
| `add(String path, Runnable action)` | Adds an enabled item. Slash-separated segments create or reuse submenus. |
| `add(String path, Runnable action, boolean enabled)` | Adds an item that may start disabled. |
| `addSeparator()` | Adds a separator to the root menu. |
| `remove(String path)` | Removes the item or submenu at the path; missing paths are ignored. |
| `setEnabled(boolean)` | Enables or disables the trigger button. |
| `clear()` | Removes every menu item and separator. |
| `peer()` | Returns the trigger `JButton`. |

Fluent methods return `this` (`SwingMenuList`).

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
app.changeDock(nav, Dock.RIGHT);        // move an already-added rail
```

### 8.1 API

| Method | Description |
| --- | --- |
| `setWidth(double width)` | Sets the rail's preferred/minimum width; default `160`. |
| `setSpacing(double spacing)` | Sets the gap between items; default `4`. |
| `setDock(Dock dock)` | Validates and stores `LEFT`/`RIGHT`. This only updates configuration; it does not move the component. |
| `dock()` | Returns the stored dock side. |
| `setArrangement(Arrangement arrangement)` | Sets `GRID`, `BOX_Y`, `BOX_X`, or `FLOW`. |
| `arrangement()` | Returns the current arrangement. |
| `setItemAlignment(Alignment alignment)` | Sets `LEFT`, `CENTER`, or `RIGHT`. |
| `itemAlignment()` | Returns the current alignment. |
| `setItemWidth(ItemWidth width)` | Sets `FILL` or `CONTENT`. |
| `itemWidth()` | Returns the current item-width mode. |
| `addItem(String label, Runnable action)` | Adds a button; the action runs when it is clicked. |
| `addItem(String label, Icon icon, Runnable action)` | Adds a button with an optional icon. |
| `addSection(String title)` | Adds a non-clickable heading; it does not count toward `size()`. |
| `setActive(int index)` | Highlights the item at the zero-based index; `-1` or out-of-range clears it. |
| `activeIndex()` | Returns the highlighted index, or `-1`. |
| `clear()` | Removes all items and sections. |
| `size()` | Returns the number of items, excluding sections. |
| `peer()` | Returns the `JScrollPane` that contains the rail. |

Enums:

| Enum | Values |
| --- | --- |
| `Dock` | `LEFT`, `RIGHT` |
| `Arrangement` | `GRID`, `BOX_Y`, `BOX_X`, `FLOW` |
| `Alignment` | `LEFT`, `CENTER`, `RIGHT` |
| `ItemWidth` | `FILL`, `CONTENT` |

Fluent methods return `this` (`SwingSideBar`).

Notes:

- `addSection` adds a heading and does not count toward `size()`.
- `setDock` only stores the preference. Use `app.changeDock(nav, dock)` to move
  an already-added rail; it updates both `dock()` and the parent layout.
- `setActive(-1)` clears the highlight; out-of-range values also clear it.
- Items overflow vertically inside a `JScrollPane`; `peer()` returns that scroll
  pane.
- The rail content tracks the viewport **width** but not its **height**, so
  `Arrangement.GRID` rows keep their preferred height. A short list stays at the
  top; a long list scrolls instead of spreading items over the window.
- Getters: `dock()`, `arrangement()`, `itemAlignment()`, `itemWidth()`,
  `activeIndex()`, `size()`.

---

## 9. `FileDropZone` — file drop target

**What it is:** a visible panel that accepts local files dropped from the desktop.
It validates the drop, reports normalized absolute paths, and shows a best-effort
preview of the first decodable image in the drop. It never copies, moves, or
uploads the files.

```java
FileDropZone<JPanel> zone = ui.fileDropZone()
        .setPrompt("Drop manga files here")
        .onFilesDropped(paths -> status.setText("Accepted: " + paths))
        .onDropRejected(reason -> status.setText("Rejected: " + reason));

JPanel target = zone.peer();
target.setPreferredSize(new Dimension(480, 220));
page.add(target, BorderLayout.CENTER);

zone.setEnabled(false); // stops accepting drops
zone.clearPreview();    // removes the image and cancels a pending decode
```

### 9.1 API

| Method | Description |
| --- | --- |
| `onFilesDropped(Consumer<List<Path>>)` | Registers the success handler. It receives one immutable list of normalized absolute paths; a drop is all-or-nothing. |
| `onDropRejected(Consumer<String>)` | Registers the rejection handler. It receives a short, user-displayable reason. |
| `setPrompt(String)` | Changes the visible instruction. It does not change accepted file types. |
| `setEnabled(boolean)` | Enables or disables the target. A disabled target invokes neither callback. |
| `clearPreview()` | Removes the current image preview and cancels a decode that is still running. Reported dropped paths are unchanged. |
| `peer()` | Returns the `JPanel` to add to a layout. |

Fluent methods return `this` (`SwingFileDropZone`).

`SwingFileDropZone` also exposes two preview helpers for screens that draw
controls over the image:

- `previewBounds()` returns the drawn image area in `peer()` coordinates, or an
  empty rectangle when no image is shown.
- `onPreviewChanged(Runnable)` runs on the EDT whenever that area changes, so an
  overlay can reposition or hide itself. The demo uses both to place a circular X
  button at the image's top-right.

Drop rules:

- Only `DataFlavor.javaFileListFlavor` is accepted.
- Every entry must be an existing regular file. Directories, missing files, or
  a mixed list reject the whole drop.
- Unsupported flavors, empty lists, and unreadable transfers call
  `onDropRejected` and never `onFilesDropped`.
- For an accepted drop, the zone uses `ImageIO` to find the first decodable
  image and shows it scaled proportionally to the target. It does not rely on
  file extensions.
- A non-image or corrupt image is still a valid file drop. It stays in the
  reported path list but does not produce a preview; the prompt returns.
- A later accepted drop replaces the preview. Only the newest drop may update
  it, so a slow earlier decode cannot overwrite a newer image.
- Image decoding runs off the EDT; preview updates and callbacks are always
  delivered on the EDT. Move additional file processing to a worker thread.
- `clearPreview()` removes the image and cancels a decode that is still running.
  A screen that wants a discard control owns that button; the demo overlays a
  small circular X on the top-right of the target.
- The zone keeps no dropped paths and only reads candidate images for the
  preview.

---

## 10. Layout managers and sizing

**What it is:** a container's `LayoutManager` decides where each child goes and
how big it is. `getPreferredSize()`, `getMinimumSize()`, and `getMaximumSize()`
are hints; the layout may ignore them. Choose the layout that gives the behavior
you want instead of resizing components by hand.

### 10.1 Common layouts

| Layout | Sizing behavior | Use for |
| --- | --- | --- |
| `BorderLayout` | `NORTH`/`SOUTH` use preferred height and full width; `WEST`/`EAST` use preferred width and full height; `CENTER` takes the remaining space | Shell, sidebar + content, a table that should fill the page |
| `GridLayout` | Equal cells; preferred size is ignored | Uniform button grids |
| `FlowLayout` | Children keep their preferred size and wrap | Toolbars, action rows, natural-width fields |
| `BoxLayout` | One row/column; grows children up to their maximum size | Vertical stacks |
| `GridBagLayout` | Constraint-based; `weightx`/`weighty` share extra space, `fill` stretches the child | Label/field forms |
| `CardLayout` | Shows one child at a time | Screen switching |

### 10.2 Sizing rules

```java
component.setPreferredSize(new Dimension(320, 32)); // natural size
component.setMinimumSize(new Dimension(120, 32));   // smallest allowed
component.setMaximumSize(new Dimension(320, 32));   // largest allowed
```

- `GridLayout` and `BorderLayout.CENTER` override preferred size.
- `BoxLayout` grows children up to their maximum size; set a maximum to stop it.
- Call `revalidate()` after changing size hints on a visible screen.

### 10.3 Practices

**Forms — keep fields at their natural width**

```java
JPanel body = new JPanel();
body.setOpaque(false);
body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
row.setOpaque(false);
row.add(ui.textbox().setPrompt("Title").setSize(320, 32).peer());
body.add(row);

// Pin the form to the top so rows are not spread over the page height.
JPanel top = new JPanel(new BorderLayout());
top.setOpaque(false);
top.add(body, BorderLayout.NORTH);
page.add(top, BorderLayout.CENTER);
```

**Stop growth in a `BoxLayout`**

```java
JTextField field = ui.textbox().setPrompt("Title").peer();
field.setMaximumSize(new Dimension(320, field.getPreferredSize().height));
```

**Label/field forms**

Use `GridBagLayout`. Give the input column `weightx = 1` and
`fill = HORIZONTAL` to grow with the window, or `weightx = 0` and
`fill = NONE` to keep it at its preferred width. `FormDialog` uses the growing
form.

**Fill the space on purpose**

Tables and lists belong in `BorderLayout.CENTER`; that is what makes them use
the remaining room.

**Sidebar**

`SideBar` manages its own layout. Keep the defaults for a full-width rail, or
use:

```java
nav.setItemWidth(ItemWidth.CONTENT);   // natural button width
nav.setArrangement(Arrangement.BOX_Y); // natural row height
```

### 10.4 Component sizing helpers

| API | Effect |
| --- | --- |
| `TextBox.setSize(w, h)` | Sets the peer's preferred size (minimum height `h`, minimum width `0`). Use it for consistent form widths; it is still only a hint. |
| `SideBar.setWidth(w)` | Fixes the rail's preferred/minimum width; height comes from the parent. |
| `SideBar.setItemWidth(FILL / CONTENT)` | `FILL` spans the rail; `CONTENT` keeps label/icon width. |
| `SideBar.setArrangement(GRID / BOX_Y / BOX_X / FLOW)` | `GRID` uses equal cells; `BOX_Y`/`BOX_X` use natural size along the stack; `FLOW` wraps. |
| `SwingApp.changeDock(bar, dock)` | Moves an already-added rail to `WEST`/`EAST` and updates `dock()`. |
| `SwingApp.add(component)` | Adds to a vertical `BoxLayout` stack; cap the width with `setMaximumSize` or a wrapper row. |
| `FormDialog.addField(label, input)` | Adds a horizontally filling `GridBagLayout` row. |

---

## 11. `DataTable` — typed table with search, sort, paging, CRUD

**What it is:** an `AbstractTableModel`-backed table for a list of row objects.
It provides columns, selection, search, sorting, paging, and optional
Add/Edit/Delete handling.

### 11.1 Columns

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

### 11.2 Data and selection

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

### 11.3 Search, sort, paging

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

### 11.4 CRUD

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

### 11.5 Parent it

```java
panel.add(table.peer(), BorderLayout.CENTER);
```

`peer()` is a `JPanel` containing the search toolbar, the table, and the pager.

### 11.6 API reference

| Method | Description |
| --- | --- |
| `addColumn(Column<T, ?> column)` | Adds a prebuilt column. |
| `addColumn(String title, Class<V> type, Function<T,V> getter)` | Adds a column from a title, value type, and getter. |
| `setItems(List<T> items)` | Replaces all rows and resets to page 0. |
| `addItem(T item)` | Appends one row. |
| `removeItem(int index)` | Removes the row at the data index. |
| `updateItem(int index, T item)` | Replaces the row at the data index. |
| `clear()` | Removes all rows. |
| `items()` | Returns all rows. |
| `itemAt(int index)` | Returns one row. |
| `count()` | Returns the total row count. |
| `filteredCount()` | Returns the row count after the current search filter. |
| `onSelect(Consumer<T>)` | Registers a row-selection handler. |
| `onDoubleClick(Consumer<T>)` | Registers a double-click handler. |
| `setCreateHandler(Supplier<Optional<T>>)` | Runs on Add; a present value is appended. |
| `setEditHandler(Function<T, Optional<T>>)` | Runs on Edit; a present value replaces the row. |
| `setDeleteHandler(Consumer<T>)` | Runs after delete confirmation, before the local row is removed. |
| `setCrudEnabled(boolean)` | Shows or hides the Add/Edit/Delete toolbar and row menu. |
| `setSelectionMode(SelectionMode)` | Sets `NONE`, `SINGLE`, or `MULTIPLE`. |
| `setSortable(boolean)` | Enables header-click sorting. |
| `setSearchText(String)` | Filters rows using every column's display text. |
| `setPageSize(int)` | Sets rows per page; must be positive. Resets to page 0. |
| `setPageIndex(int)` | Selects a zero-based page; negative values throw, too-high values clamp on refresh. |
| `nextPage()` / `previousPage()` | Moves one page. |
| `currentPage()` | Returns the zero-based page index. |
| `pageCount()` | Returns the total number of pages. |
| `peer()` | Returns the `JPanel` containing toolbar, table, and pager. |

`SwingDataTable<T>` adds:

| Method | Description |
| --- | --- |
| `sortBy(int columnIndex, boolean ascending)` | Sorts by a column from code or tests. |
| `sortColumn()` | Returns the active sort column, or `-1`. |
| `sortAscending()` | Returns the current sort direction. |

`Column<T,V>`:

| Method | Description |
| --- | --- |
| `Column.of(title, type, getter)` / `new Column<>(...)` | Creates a column. All three arguments are required. |
| `formatter(Function<? super V,String>)` | Display text; also used by search. |
| `comparator(Comparator<? super V>)` | Sort order; defaults to `Comparable`, then case-insensitive strings. |
| `renderer(TableCellRenderer)` | Custom cell rendering. |
| `editor(TableCellEditor)` | Custom cell editing; cells are read-only by default. |
| `width(int)` | Preferred column width; must be positive. |
| `title()`, `valueType()` | Column metadata. |
| `valueOf(T item)` | Reads the cell value. |
| `format(Object value)` | Formats a value; `null` becomes `""`. |
| `compare(T first, T second)` | Compares two rows; `null` values sort first. |

`SelectionMode`: `NONE`, `SINGLE`, `MULTIPLE`.

---

## 12. `FormDialog` — modal forms

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

### 12.1 API

| Method | Description |
| --- | --- |
| `FormDialog.create(Window owner, String title)` | Creates a dialog. `ui.form(owner, title)` is the preferred factory. |
| `addText(String label, TextBox<? extends JComponent> input)` | Adds a label/text-box row. |
| `addDrop(String label, DropMenu<? extends JComponent> input)` | Adds a label/drop-menu row. |
| `addField(String label, JComponent input)` | Adds a label/custom-component row. |
| `validate(Supplier<String> validator)` | Registers a validator that returns an error message, or `null`/blank when valid. |
| `result(Supplier<Optional<T>> supplier)` | Registers the result builder. A present value closes the dialog; an empty value keeps it open. |
| `show()` | Shows the dialog modally and returns the result; cancel returns `Optional.empty()`. |

Fluent methods return `this` except `show()`.

Notes:

- `addText`, `addDrop`, and `addField` add one row each.
- `validate` returns an error message or `null`/blank when valid.
- `result` builds the value on OK. Returning `Optional.empty()` keeps the dialog
  open and shows a generic message.
- `show()` is modal and must be called on the EDT.
- Assign to a typed `FormDialog<T>` variable before chaining; this avoids
  needing an explicit type witness on `ui.form(...)`.
- The input column uses `GridBagLayout` with `weightx = 1` and
  `fill = HORIZONTAL`, so fields follow the dialog width. For a form that keeps
  natural field widths, build a custom panel instead (see
  [Layout managers and sizing](#9-layout-managers-and-sizing)).

---

## 13. Screens and Spring integration

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

### 13.1 API

| Class | Public API |
| --- | --- |
| `LoginScreen` | `LoginScreen(UI ui)`, `onSubmit(Consumer<Credentials>)`, record `Credentials(String email, String password)` |
| `MainScreen` | `MainScreen(UI ui)`, `add(String label, JComponent screen)`, `select(int index)`, `sidebar()` |
| `MangaListScreen` | `MangaListScreen(UI ui, Window owner, MangaDataSource dataSource)`, `refresh()` |
| `MangaFormDialog` | `showCreate(Window owner, UI ui)`, `showEdit(Window owner, UI ui, MangaResponse existing)` |
| `MangaDataSource` | `findAll()`, `create(MangaRequest)`, `update(String id, MangaRequest)`, `delete(String id)` |

`add(...)` returns `this`; `refresh()` reloads in a `SwingWorker`; the form
methods return `Optional<MangaRequest>`.

---

## 14. Threading rules

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

## 15. Theme and styling

`Theme.install()` applies the system look and feel plus consistent colors, fonts,
and table defaults. `SwingApp` calls it automatically. Available constants
include `BACKGROUND`, `SURFACE`, `BORDER`, `TEXT`, `MUTED_TEXT`, `ACCENT`,
`DANGER`, `FONT`, `FONT_BOLD`, `TITLE_FONT`, `GAP`, and `PAD`.

```java
label.setFont(Theme.FONT_BOLD);
panel.setBorder(Theme.cardBorder());
panel.setBackground(Theme.SURFACE);
```

| API | Description |
| --- | --- |
| `Theme.install()` | Applies the system look and feel plus the shared UI defaults. `SwingApp` calls it automatically. |
| `Theme.cardBorder()` | Returns a line border plus `PAD` padding. |
| `Theme.padded()` | Returns an empty border with `PAD` on all sides. |
| `Theme.apply(Window)` | Sets the theme font on the window. |

Colors: `BACKGROUND`, `SURFACE`, `BORDER`, `TEXT`, `MUTED_TEXT`, `ACCENT`,
`DANGER`. Fonts: `FONT`, `FONT_BOLD`, `TITLE_FONT`. Spacing: `GAP` (8),
`PAD` (16).

There is no FlatLaf dependency; the theme is a small UIManager customization.

---

## 16. Testing components

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

## 17. Cookbook

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

## 18. Running and troubleshooting

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

## 19. Conventions

- Create components through `UI`; screens should not instantiate `SwingXxx`.
- Keep fluent mutators returning `this` / the interface.
- Use `peer()` to parent a component; do not reach inside it.
- Inject behavior through handlers; components contain no business logic.
- Use `Optional.empty()` from a create/edit handler when persistence is async.
- Keep UI work on the EDT and I/O in `SwingWorker`.
- Package names stay as-is: `HMS`, `GUI`, `Components`.
