package com.example.HMS.GUI;

import com.example.HMS.GUI.Components.DataTable.Column;
import com.example.HMS.GUI.Components.DataTable.SwingDataTable;
import com.example.HMS.GUI.Components.DropMenu.SwingDropMenu;
import com.example.HMS.GUI.Components.MenuList.SwingMenuList;
import com.example.HMS.GUI.Components.SideBar.SwingSideBar;
import com.example.HMS.GUI.Components.TextBox.impl.SwingTextBox;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Manual smoke-test frame for every component. Run with {@code ./grad.sh ui-demo}.
 */
public final class SwingDemo {

    private SwingDemo() {
    }

    public static void main(String[] args) {
        Theme.install();
        SwingUtilities.invokeLater(SwingDemo::build);
    }

    private static void build() {
        UI ui = new UI();
        SwingApp app = ui.app("HMS Component Demo");

        SwingSideBar sidebar = ui.sidebar();
        sidebar.setWidth(180);
        CardLayout cards = new CardLayout();
        JPanel pages = new JPanel(cards);
        pages.setOpaque(false);

        String[] names = {"Text boxes", "Drop menu", "Menu list", "Data table"};
        sidebar.addSection("Components");
        for (int i = 0; i < names.length; i++) {
            final String name = names[i];
            final int index = i;
            sidebar.addItem(name, () -> {
                cards.show(pages, name);
                sidebar.setActive(index);
            });
        }
        sidebar.setActive(0);

        pages.add(textBoxDemo(ui, app), names[0]);
        pages.add(dropMenuDemo(ui), names[1]);
        pages.add(menuListDemo(ui), names[2]);
        pages.add(dataTableDemo(ui, app, names[3]), names[3]);

        app.sidebar(sidebar);
        app.setContent(pages);
        app.setStatus("Demo mode - no database connection");
        app.size(1000, 640);
        app.show();
    }

    private static JPanel page(String title) {
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 8));
        panel.setBackground(Theme.SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        JLabel heading = new JLabel(title);
        heading.setFont(Theme.FONT_BOLD);
        panel.add(heading);
        return panel;
    }

    private static JPanel textBoxDemo(UI ui, SwingApp app) {
        JPanel panel = page("TextBox states");
        JLabel events = new JLabel("Events appear here");

        SwingTextBox normal = ui.textbox().setPrompt("Your name");
        SwingTextBox disabled = ui.textbox("Disabled value").setEnabled(false);
        SwingTextBox error = ui.textbox("bad@").setError("Enter a valid email");
        SwingTextBox password = ui.passwordBox().setPrompt("Password");
        SwingTextBox limited = ui.textbox().setPrompt("Max 8 characters").setMaxLength(8);
        SwingTextBox submit = ui.textbox().setPrompt("Press Enter")
                .onSubmit(value -> events.setText("Submitted: " + value))
                .onChange(value -> events.setText("Typing: " + value));

        panel.add(normal.peer());
        panel.add(disabled.peer());
        panel.add(error.peer());
        panel.add(password.peer());
        panel.add(limited.peer());
        panel.add(submit.peer());
        panel.add(events);

        JLabel values = new JLabel("getText never returns the prompt");
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setOpaque(false);
        javax.swing.JButton read = new javax.swing.JButton("Read values");
        read.addActionListener(e -> values.setText(
                "normal='" + normal.getText() + "' password='" + password.getText() + "'"));
        actions.add(read);
        panel.add(values);
        panel.add(actions);
        return panel;
    }

    private static JPanel dropMenuDemo(UI ui) {
        JPanel panel = page("Drop menu");
        JLabel selected = new JLabel("No selection");
        SwingDropMenu menu = ui.dropMenu("Ongoing", "Completed", "Hiatus")
                .setPlaceholder("Choose a status...")
                .onSelect(value -> selected.setText("Selected: " + value));

        SwingDropMenu countries = ui.dropMenu("Vietnam", "Japan", "France");

        javax.swing.JButton replace = new javax.swing.JButton("Replace items (no stale event)");
        replace.addActionListener(e ->
                menu.setItems("Draft", "Review", "Published"));
        javax.swing.JButton disable = new javax.swing.JButton("Toggle enabled");
        disable.addActionListener(e -> menu.setEnabled(!menu.peer().isEnabled()));

        panel.add(menu.peer());
        panel.add(countries.peer());
        panel.add(selected);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setOpaque(false);
        actions.add(replace);
        actions.add(disable);
        panel.add(actions);
        return panel;
    }

    private static JPanel menuListDemo(UI ui) {
        JPanel panel = page("Menu list");
        JLabel result = new JLabel("No action yet");
        SwingMenuList menu = ui.menuList("Actions")
                .add("File/New", () -> result.setText("File > New"))
                .add("File/Open", () -> result.setText("File > Open"))
                .add("File/Recent/Last week", () -> result.setText("File > Recent > Last week"))
                .addSeparator()
                .add("Edit/Undo", () -> result.setText("Edit > Undo"))
                .add("Edit/Redo", () -> result.setText("Edit > Redo"))
                .add("Help/Disabled item", () -> {
                }, false);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setOpaque(false);
        javax.swing.JButton remove = new javax.swing.JButton("Remove File/Open");
        remove.addActionListener(e -> menu.remove("File/Open"));
        actions.add(menu.peer());
        actions.add(remove);
        panel.add(actions);
        panel.add(result);
        return panel;
    }

    private static JPanel dataTableDemo(UI ui, SwingApp app, String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        SwingDataTable<MangaRow> table = ui.table();
        table.addColumn(Column.of("ID", String.class, MangaRow::id).width(80))
                .addColumn(Column.of("Title", String.class, MangaRow::title).width(260))
                .addColumn(Column.of("Status", String.class, MangaRow::status).width(120))
                .addColumn(Column.of("Year", Short.class, MangaRow::year).width(80))
                .addColumn(Column.of("Author", String.class, MangaRow::author).width(160));

        table.setItems(samples());
        table.setPageSize(5);
        table.setCrudEnabled(true);
        table.setCreateHandler(() -> showForm(ui, app, title, null));
        table.setEditHandler(row -> showForm(ui, app, title, row));

        JLabel status = new JLabel("Select a row");
        table.onSelect(row -> status.setText("Selected: " + row.title()));
        table.onDoubleClick(row -> status.setText("Double-clicked: " + row.title()));

        panel.add(table.peer(), BorderLayout.CENTER);
        panel.add(status, BorderLayout.SOUTH);
        return panel;
    }

    private static Optional<MangaRow> showForm(UI ui, SwingApp app, String title, MangaRow existing) {
        SwingTextBox titleBox = ui.textbox(existing == null ? "" : existing.title());
        SwingTextBox authorBox = ui.textbox(existing == null ? "" : existing.author());
        SwingTextBox yearBox = ui.textbox(existing == null || existing.year() == null
                ? "" : String.valueOf(existing.year()));
        SwingDropMenu statusBox = ui.dropMenu("ongoing", "completed", "hiatus")
                .setPlaceholder("Status")
                .select(existing == null ? 0 : Math.max(0, statusIndex(existing.status())));

        return ui.<MangaRow>form(app.frame(), title)
                .addText("Title", titleBox)
                .addText("Author", authorBox)
                .addDrop("Status", statusBox)
                .addText("Year", yearBox)
                .validate(() -> {
                    if (titleBox.getText().isBlank()) {
                        return "Title is required.";
                    }
                    if (!yearBox.getText().isBlank() && parseYear(yearBox.getText()) == null) {
                        return "Year must be a number.";
                    }
                    return null;
                })
                .result(() -> {
                    Short year = yearBox.getText().isBlank() ? null : parseYear(yearBox.getText());
                    String status = statusBox.selected() == null ? "ongoing" : statusBox.selected();
                    String id = existing == null ? UUID.randomUUID().toString() : existing.id();
                    return Optional.of(new MangaRow(id, titleBox.getText().trim(), status,
                            year, authorBox.getText().trim()));
                })
                .show();
    }

    private static int statusIndex(String status) {
        if (status == null) {
            return -1;
        }
        return switch (status.toLowerCase(Locale.ROOT)) {
            case "ongoing" -> 0;
            case "completed" -> 1;
            case "hiatus" -> 2;
            default -> -1;
        };
    }

    private static Short parseYear(String text) {
        try {
            return Short.valueOf(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static List<MangaRow> samples() {
        String[] titles = {"One Piece", "Berserk", "Vinland Saga", "Monster", "Pluto",
                "20th Century Boys", "Fullmetal Alchemist", "Naruto", "Bleach", "Dorohedoro",
                "Chainsaw Man", "Oyasumi Punpun", "Solanin", "Goodnight Punpun", "Akira",
                "Nausicaa", "Blame!", "Knights of Sidonia", "Vagabond", "Slam Dunk",
                "Real", "Kingdom", "Historie", "Hikaru no Go", "Death Note"};
        String[] statuses = {"ongoing", "completed", "hiatus"};
        List<MangaRow> rows = new ArrayList<>();
        for (int i = 0; i < titles.length; i++) {
            rows.add(new MangaRow(
                    String.valueOf(i + 1),
                    titles[i],
                    statuses[i % statuses.length],
                    (short) (1990 + (i % 35)),
                    "Author " + (i + 1)));
        }
        return rows;
    }

    public record MangaRow(String id, String title, String status, Short year, String author) {
    }
}
