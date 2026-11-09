package com.example.HMS.GUI.Components.SideBar;

import javax.swing.Icon;
import javax.swing.JComponent;

/**
 * Navigation rail with configurable layout.
 *
 * @param <T> Swing peer type exposed by {@link #peer()}
 */
public interface SideBar<T extends JComponent> {

    SideBar<T> setWidth(double width);

    SideBar<T> setSpacing(double spacing);

    SideBar<T> setDock(Dock dock);

    Dock dock();

    SideBar<T> setArrangement(Arrangement arrangement);

    Arrangement arrangement();

    SideBar<T> setItemAlignment(Alignment alignment);

    Alignment itemAlignment();

    SideBar<T> setItemWidth(ItemWidth width);

    ItemWidth itemWidth();

    SideBar<T> addItem(String label, Runnable action);

    SideBar<T> addItem(String label, Icon icon, Runnable action);

    /** Adds a non-clickable group heading. */
    SideBar<T> addSection(String title);

    /** Highlights the item at {@code index}; {@code -1} clears the highlight. */
    SideBar<T> setActive(int index);

    int activeIndex();

    SideBar<T> clear();

    int size();

    T peer();
}
