package com.example.HMS.GUI.Components.DragDrop;

import javax.swing.JComponent;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Visible target that receives local files dropped from the desktop.
 *
 * <p>The component only validates and reports paths. It never opens, copies,
 * moves, or uploads the dropped files; that remains the screen's job.</p>
 *
 * @param <P> Swing peer type exposed by {@link #peer()}
 */
public interface FileDropZone<P extends JComponent> {

    /**
     * Registers the handler called with an immutable list of normalized absolute
     * paths. A drop is all-or-nothing: every entry must be an existing regular
     * file.
     */
    FileDropZone<P> onFilesDropped(Consumer<List<Path>> handler);

    /**
     * Registers the handler called with a short, user-displayable reason when a
     * drop is unsupported or invalid.
     */
    FileDropZone<P> onDropRejected(Consumer<String> handler);

    /** Changes the visible instruction; it does not change which files are accepted. */
    FileDropZone<P> setPrompt(String prompt);

    /** Enables or disables the target. A disabled target accepts no drops. */
    FileDropZone<P> setEnabled(boolean enabled);

    /**
     * Removes the current image preview and cancels any preview that is still
     * loading. It does not change or forget reported dropped files.
     */
    FileDropZone<P> clearPreview();

    P peer();
}
