package com.example.HMS.GUI.Components.MenuList;

import javax.swing.JComponent;

/**
 * Button trigger with a hierarchical popup menu.
 *
 * @param <T> Swing peer type exposed by {@link #peer()}
 */
public interface MenuList<T extends JComponent> {

    /**
     * Adds an item using slash-separated path syntax, e.g. {@code "File/New"}.
     * Intermediate menus are created/reused automatically.
     */
    MenuList<T> add(String path, Runnable action);

    /** Adds an item that may start disabled. */
    MenuList<T> add(String path, Runnable action, boolean enabled);

    MenuList<T> addSeparator();

    /** Removes the item or submenu at the given path. Missing paths are ignored. */
    MenuList<T> remove(String path);

    MenuList<T> setEnabled(boolean enabled);

    MenuList<T> clear();

    T peer();
}
