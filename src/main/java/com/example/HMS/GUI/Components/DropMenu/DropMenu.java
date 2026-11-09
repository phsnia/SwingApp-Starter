package com.example.HMS.GUI.Components.DropMenu;

import javax.swing.JComponent;
import java.util.function.Consumer;

/**
 * Reusable drop-down / select control.
 *
 * <p>An optional placeholder item can be shown without ever being returned as
 * a real selection: {@link #selected()} returns {@code null} while it is
 * visible.</p>
 *
 * @param <T> Swing peer type exposed by {@link #peer()}
 */
public interface DropMenu<T extends JComponent> {

    DropMenu<T> setItems(String... items);

    /** Selects a real item by its zero-based index (placeholder excluded). */
    DropMenu<T> select(int index);

    /** @return the selected real item, or {@code null} for placeholder/no selection */
    String selected();

    /** @return the selected real item index, or {@code -1} for placeholder/no selection */
    int selectedIndex();

    /** @return number of real items, excluding any placeholder */
    int count();

    DropMenu<T> clearSelection();

    DropMenu<T> setEnabled(boolean enabled);

    /** Adds a leading hint item that is never returned by {@link #selected()}. */
    DropMenu<T> setPlaceholder(String text);

    DropMenu<T> onSelect(Consumer<String> fn);

    T peer();
}
