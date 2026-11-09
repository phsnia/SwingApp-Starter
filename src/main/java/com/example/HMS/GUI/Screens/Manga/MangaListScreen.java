package com.example.HMS.GUI.Screens.Manga;

import com.example.HMS.GUI.Components.DataTable.Column;
import com.example.HMS.GUI.Components.DataTable.SelectionMode;
import com.example.HMS.GUI.Components.DataTable.SwingDataTable;
import com.example.HMS.GUI.Theme;
import com.example.HMS.GUI.UI;
import com.example.HMS.Manga.dto.MangaRequest;
import com.example.HMS.Manga.dto.MangaResponse;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * Manga list with search, sorting, paging and CRUD. Database work runs on a
 * SwingWorker; results are applied on the EDT in {@code done()}.
 */
public final class MangaListScreen extends JPanel {
    private final UI ui;
    private final Window owner;
    private final MangaDataSource dataSource;
    private final SwingDataTable<MangaResponse> table;
    private final JLabel status = new JLabel(" ");

    public MangaListScreen(UI ui, Window owner, MangaDataSource dataSource) {
        super(new BorderLayout(0, Theme.GAP));
        this.ui = ui;
        this.owner = owner;
        this.dataSource = dataSource;
        setOpaque(false);

        table = ui.table();
        table.addColumn(Column.of("Title", String.class, MangaResponse::title).width(240))
                .addColumn(Column.of("Slug", String.class, MangaResponse::slug).width(160))
                .addColumn(Column.of("Author", String.class, MangaResponse::author).width(140))
                .addColumn(Column.of("Artist", String.class, MangaResponse::artist).width(140))
                .addColumn(Column.of("Status", String.class, MangaResponse::status).width(100))
                .addColumn(Column.of("Type", String.class, MangaResponse::type).width(90))
                .addColumn(Column.of("Year", Short.class, MangaResponse::releaseYear).width(70));
        table.setSelectionMode(SelectionMode.SINGLE);
        table.setPageSize(10);
        table.setCrudEnabled(true);
        table.onDoubleClick(this::editRow);
        table.setCreateHandler(this::createRow);
        table.setEditHandler(this::editRow);
        table.setDeleteHandler(this::deleteRow);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Manga library");
        title.setFont(Theme.TITLE_FONT);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refresh());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(refresh);
        header.add(title, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        status.setForeground(Theme.MUTED_TEXT);

        add(header, BorderLayout.NORTH);
        add(table.peer(), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        refresh();
    }

    public void refresh() {
        setBusy("Loading manga...");
        new SwingWorker<List<MangaResponse>, Void>() {
            @Override
            protected List<MangaResponse> doInBackground() {
                return dataSource.findAll();
            }

            @Override
            protected void done() {
                try {
                    List<MangaResponse> rows = get();
                    table.setItems(rows);
                    setBusy(rows.size() + " manga loaded");
                } catch (Exception ex) {
                    fail("Could not load manga", ex);
                }
            }
        }.execute();
    }

    private Optional<MangaResponse> createRow() {
        Optional<MangaRequest> request = MangaFormDialog.showCreate(owner, ui);
        request.ifPresent(this::persistCreate);
        // The table is refreshed from the database after the save completes.
        return Optional.empty();
    }

    private Optional<MangaResponse> editRow(MangaResponse existing) {
        Optional<MangaRequest> request = MangaFormDialog.showEdit(owner, ui, existing);
        request.ifPresent(value -> persistUpdate(existing.id(), value));
        return Optional.empty();
    }

    private void deleteRow(MangaResponse existing) {
        persistDelete(existing);
    }

    private void persistCreate(MangaRequest request) {
        setBusy("Saving new manga...");
        new SwingWorker<MangaResponse, Void>() {
            @Override
            protected MangaResponse doInBackground() {
                return dataSource.create(request);
            }

            @Override
            protected void done() {
                try {
                    MangaResponse created = get();
                    setBusy("Created " + created.title());
                    refresh();
                } catch (Exception ex) {
                    fail("Could not create manga", ex);
                }
            }
        }.execute();
    }

    private void persistUpdate(String id, MangaRequest request) {
        setBusy("Saving changes...");
        new SwingWorker<MangaResponse, Void>() {
            @Override
            protected MangaResponse doInBackground() {
                return dataSource.update(id, request);
            }

            @Override
            protected void done() {
                try {
                    MangaResponse updated = get();
                    setBusy("Updated " + updated.title());
                    refresh();
                } catch (Exception ex) {
                    fail("Could not update manga", ex);
                }
            }
        }.execute();
    }

    private void persistDelete(MangaResponse existing) {
        setBusy("Deleting " + existing.title() + "...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                dataSource.delete(existing.id());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    setBusy("Deleted " + existing.title());
                    refresh();
                } catch (Exception ex) {
                    fail("Could not delete manga", ex);
                    refresh();
                }
            }
        }.execute();
    }

    private void setBusy(String message) {
        status.setText(message);
    }

    private void fail(String action, Exception ex) {
        Throwable cause = unwrap(ex);
        String message = cause.getMessage() == null || cause.getMessage().isBlank()
                ? cause.getClass().getSimpleName()
                : cause.getMessage();
        setBusy(action + ": " + message);
        JOptionPane.showMessageDialog(this, message, action, JOptionPane.ERROR_MESSAGE);
    }

    private static Throwable unwrap(Throwable ex) {
        if (ex instanceof ExecutionException || ex instanceof CompletionException) {
            return ex.getCause() == null ? ex : unwrap(ex.getCause());
        }
        return ex;
    }
}
