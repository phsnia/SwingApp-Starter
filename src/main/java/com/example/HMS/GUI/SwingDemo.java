package com.example.HMS.GUI;

import com.example.HMS.GUI.Components.Core.SwingApp;
import com.example.HMS.GUI.Components.Core.Theme;
import com.example.HMS.GUI.Components.Core.UI;
import com.example.HMS.GUI.Components.DataTable.Column;
import com.example.HMS.GUI.Components.DataTable.SwingDataTable;
import com.example.HMS.GUI.Components.DragDrop.SwingFileDropZone;
import com.example.HMS.GUI.Components.DropMenu.SwingDropMenu;
import com.example.HMS.GUI.Components.MenuList.SwingMenuList;
import com.example.HMS.GUI.Components.SideBar.Dock;
import com.example.HMS.GUI.Components.SideBar.SwingSideBar;
import com.example.HMS.GUI.Components.TextBox.SwingTextBox;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
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
        sidebar.setSpacing(8);
//        sidebar.setDock(Dock.RIGHT);
        CardLayout cards = new CardLayout();
        JPanel pages = new JPanel(cards);
        pages.setOpaque(false);

        String[] names = {"Text boxes", "Drop menu", "Menu list", "Data table", "File drop"};
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
        pages.add(fileDropDemo(ui), names[4]);

        app.sidebar(sidebar);
        app.setContent(pages);
        app.setStatus("Demo mode - no database connection");
        app.size(1000, 640);
        app.show();
    }

    private static DemoPage page(String title) {
        return new DemoPage(title);
    }

    /**
     * Demo page whose rows keep their preferred size. The body is pinned to the
     * top and scrolled if needed, so fields are not stretched over the remaining
     * page height.
     */
    private static final class DemoPage extends JPanel {
        private final JPanel body = new JPanel();

        private DemoPage(String title) {
            super(new BorderLayout());
            setBackground(Theme.SURFACE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.BORDER),
                    BorderFactory.createEmptyBorder(16, 16, 16, 16)));

            JLabel heading = new JLabel(title);
            heading.setFont(Theme.FONT_BOLD);
            add(heading, BorderLayout.NORTH);

            body.setOpaque(false);
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

            JScrollPane scroll = new JScrollPane(body,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setBorder(null);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            add(scroll, BorderLayout.CENTER);
        }

        /** Adds a left-aligned row that respects the component's preferred size. */
        private DemoPage addField(JComponent component) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
            row.add(component);
            // BoxLayout distributes extra height up to each child's maximum, so
            // cap the row at its preferred height instead of letting it stretch.
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            body.add(row);
            return this;
        }
    }

    private static DemoPage textBoxDemo(UI ui, SwingApp app) {
        DemoPage panel = page("TextBox states");
        JLabel events = new JLabel("Events appear here");

        SwingTextBox normal = ui.textbox().setPrompt("Your name").setSize(320, 32);
        SwingTextBox disabled = ui.textbox("Disabled value").setEnabled(false).setSize(320, 32);
        SwingTextBox error = ui.textbox("bad@").setError("Enter a valid email").setSize(320, 32);
        SwingTextBox password = ui.passwordBox().setPrompt("Password").setSize(320, 32);
        SwingTextBox limited = ui.textbox().setPrompt("Max 8 characters").setMaxLength(8).setSize(320, 32);
        SwingTextBox submit = ui.textbox().setPrompt("Press Enter")
                .onSubmit(value -> events.setText("Submitted: " + value))
                .onChange(value -> events.setText("Typing: " + value))
                .setSize(320, 32);

        panel.addField(normal.peer());
        panel.addField(disabled.peer());
        panel.addField(error.peer());
        panel.addField(password.peer());
        panel.addField(limited.peer());
        panel.addField(submit.peer());
        panel.addField(events);

        JLabel values = new JLabel("getText never returns the prompt");
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setOpaque(false);
        javax.swing.JButton read = new javax.swing.JButton("Read values");
        read.addActionListener(e -> values.setText(
                "normal='" + normal.getText() + "' password='" + password.getText() + "'"));
        actions.add(read);
        panel.addField(values);
        panel.addField(actions);
        return panel;
    }

    private static DemoPage dropMenuDemo(UI ui) {
        DemoPage panel = page("Drop menu");
        JLabel selected = new JLabel("No selection");
        SwingDropMenu menu = ui.dropMenu("Ongoing", "Completed", "Hiatus");
        menu.setPlaceholder("Choose a status...");
        menu.onSelect(value -> selected.setText("Selected: " + value));
        menu.peer().setPreferredSize(new Dimension(320, 32));

        SwingDropMenu countries = ui.dropMenu("Vietnam", "Japan", "France");
        countries.peer().setPreferredSize(new Dimension(320, 32));

        javax.swing.JButton replace = new javax.swing.JButton("Replace items (no stale event)");
        replace.addActionListener(e -> menu.setItems("Draft", "Review", "Published"));
        javax.swing.JButton disable = new javax.swing.JButton("Toggle enabled");
        disable.addActionListener(e -> menu.setEnabled(!menu.peer().isEnabled()));

        panel.addField(menu.peer());
        panel.addField(countries.peer());
        panel.addField(selected);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setOpaque(false);
        actions.add(replace);
        actions.add(disable);
        panel.addField(actions);
        return panel;
    }

    private static DemoPage menuListDemo(UI ui) {
        DemoPage panel = page("Menu list");
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
        panel.addField(actions);
        panel.addField(result);
        return panel;
    }

    private static DemoPage fileDropDemo(UI ui) {
        DemoPage panel = page("File drop");
        JLabel status = new JLabel("Drop local files or an image onto the target");

        SwingFileDropZone zone = (SwingFileDropZone) ui.fileDropZone();
        zone.setPrompt("Drop local files here")
                .onFilesDropped(paths -> status.setText(
                        "Accepted " + paths.size()
                                + " file(s); preview uses the first decodable image: " + paths))
                .onDropRejected(reason -> status.setText("Rejected: " + reason));

        CircleButton removePreview = new CircleButton();
        removePreview.addActionListener(e -> {
            zone.clearPreview();
            status.setText("Preview cleared");
        });

        JPanel imageArea = new PreviewOverlay(zone, removePreview);
        imageArea.setPreferredSize(new Dimension(520, 240));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setOpaque(false);
        javax.swing.JButton toggle = new javax.swing.JButton("Toggle enabled");
        toggle.addActionListener(e -> {
            boolean enable = !zone.peer().isEnabled();
            zone.setEnabled(enable);
            status.setText("Drop zone " + (enable ? "enabled" : "disabled"));
        });
        actions.add(toggle);

        panel.addField(imageArea);
        panel.addField(status);
        panel.addField(actions);
        return panel;
    }

    /**
     * Demo-only overlay that keeps the drop target full-size and moves the clear
     * button to the top-right of the drawn preview. Preview updates revalidate
     * the target, so this layout runs again when the image appears or clears.
     */
    private static final class PreviewOverlay extends JPanel {
        private static final int BUTTON_INSET = 6;

        private final SwingFileDropZone zone;
        private final JPanel target;
        private final CircleButton remove;

        private PreviewOverlay(SwingFileDropZone zone, CircleButton remove) {
            super(null);
            this.zone = zone;
            this.target = zone.peer();
            this.remove = remove;
            setOpaque(false);
            add(target);
            add(remove);
            // Index 0 is the top of Swing's z-order.
            setComponentZOrder(remove, 0);
            remove.setVisible(false);
            zone.onPreviewChanged(this::updateRemoveButton);
        }

        @Override
        public void doLayout() {
            target.setBounds(0, 0, getWidth(), getHeight());
            updateRemoveButton();
        }

        private void updateRemoveButton() {
            Rectangle preview = zone.previewBounds();
            boolean visible = preview.width > 0 && preview.height > 0;
            remove.setVisible(visible);
            if (visible) {
                int size = CircleButton.SIZE;
                int x = Math.max(preview.x,
                        preview.x + preview.width - size - BUTTON_INSET);
                int y = preview.y + BUTTON_INSET;
                remove.setBounds(x, y, size, size);
            }
            remove.repaint();
            repaint();
        }
    }

    /**
     * Small translucent circular X button drawn over the top-right of the drop
     * target. It only paints itself, so it stays legible on any image.
     */
    private static final class CircleButton extends JButton {
        private static final int SIZE = 26;

        private CircleButton() {
            setPreferredSize(new Dimension(SIZE, SIZE));
            setMinimumSize(new Dimension(SIZE, SIZE));
            setMaximumSize(new Dimension(SIZE, SIZE));
            setToolTipText("Remove preview");
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int diameter = Math.min(getWidth(), getHeight());
                int x = (getWidth() - diameter) / 2;
                int y = (getHeight() - diameter) / 2;
                g2.setColor(getModel().isRollover()
                        ? new Color(190, 40, 40, 210)
                        : new Color(0, 0, 0, 150));
                g2.fillOval(x, y, diameter, diameter);

                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(Math.max(1.5f, diameter / 10f),
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int pad = Math.max(5, diameter / 4);
                g2.drawLine(x + pad, y + pad, x + diameter - pad, y + diameter - pad);
                g2.drawLine(x + diameter - pad, y + pad, x + pad, y + diameter - pad);
            } finally {
                g2.dispose();
            }
        }
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
