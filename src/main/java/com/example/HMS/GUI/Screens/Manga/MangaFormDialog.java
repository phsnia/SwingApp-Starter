package com.example.HMS.GUI.Screens.Manga;

import com.example.HMS.GUI.Components.DropMenu.SwingDropMenu;
import com.example.HMS.GUI.Components.TextBox.SwingTextBox;
import com.example.HMS.GUI.Components.Core.UI;
import com.example.HMS.Manga.dto.MangaRequest;
import com.example.HMS.Manga.dto.MangaResponse;

import java.awt.Window;
import java.util.Locale;
import java.util.Optional;

/**
 * Create/edit form for {@link MangaRequest}.
 */
public final class MangaFormDialog {
    private static final String[] STATUSES = {"ongoing", "completed", "hiatus", "cancelled"};
    private static final String[] TYPES = {"manga", "manhwa", "manhua", "comic"};

    private MangaFormDialog() {
    }

    public static Optional<MangaRequest> showCreate(Window owner, UI ui) {
        return show(owner, ui, null);
    }

    public static Optional<MangaRequest> showEdit(Window owner, UI ui, MangaResponse existing) {
        return show(owner, ui, existing);
    }

    private static Optional<MangaRequest> show(Window owner, UI ui, MangaResponse existing) {
        SwingTextBox title = ui.textbox(existing == null ? "" : existing.title());
        SwingTextBox slug = ui.textbox(existing == null ? "" : existing.slug());
        SwingTextBox author = ui.textbox(existing == null ? "" : existing.author());
        SwingTextBox artist = ui.textbox(existing == null ? "" : existing.artist());
        SwingTextBox year = ui.textbox(existing == null || existing.releaseYear() == null
                ? "" : String.valueOf(existing.releaseYear()));
        SwingDropMenu status = ui.dropMenu(STATUSES)
                .setPlaceholder("Select status")
                .select(indexOf(STATUSES, existing == null ? null : existing.status()));
        SwingDropMenu type = ui.dropMenu(TYPES)
                .setPlaceholder("Select type")
                .select(indexOf(TYPES, existing == null ? null : existing.type()));

        String dialogTitle = existing == null ? "Add manga" : "Edit manga";

        return ui.<MangaRequest>form(owner, dialogTitle)
                .addText("Title *", title)
                .addText("Slug", slug)
                .addText("Author", author)
                .addText("Artist", artist)
                .addDrop("Status", status)
                .addDrop("Type", type)
                .addText("Release year", year)
                .validate(() -> {
                    if (title.getText().isBlank()) {
                        return "Title is required.";
                    }
                    if (!year.getText().isBlank() && parseYear(year.getText()) == null) {
                        return "Release year must be a number.";
                    }
                    if (status.selected() == null) {
                        return "Choose a status.";
                    }
                    if (type.selected() == null) {
                        return "Choose a type.";
                    }
                    return null;
                })
                .result(() -> Optional.of(new MangaRequest(
                        title.getText().trim(),
                        blankToNull(slug.getText()),
                        existing == null ? null : existing.description(),
                        existing == null ? null : existing.coverUrl(),
                        blankToNull(author.getText()),
                        blankToNull(artist.getText()),
                        status.selected(),
                        type.selected(),
                        year.getText().isBlank() ? null : parseYear(year.getText()))))
                .show();
    }

    private static int indexOf(String[] values, String value) {
        if (value == null) {
            return -1;
        }
        String needle = value.toLowerCase(Locale.ROOT);
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(needle)) {
                return i;
            }
        }
        return -1;
    }

    private static Short parseYear(String text) {
        try {
            return Short.valueOf(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
