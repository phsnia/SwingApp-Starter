package com.example.HMS.GUI.Components.MenuList;

import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import java.awt.Component;
import java.awt.Container;

/**
 * Default {@link MenuList} implementation: a button that opens a
 * {@link JPopupMenu}.
 */
public class SwingMenuList implements MenuList<JButton> {
    private final JButton trigger;
    private final JPopupMenu popup = new JPopupMenu();

    public SwingMenuList(String label) {
        trigger = new JButton(label);
        trigger.addActionListener(e -> {
            if (popup.isVisible()) {
                popup.setVisible(false);
            } else {
                popup.show(trigger, 0, trigger.getHeight());
            }
        });
    }

    @Override
    public SwingMenuList add(String path, Runnable action) {
        return add(path, action, true);
    }

    @Override
    public SwingMenuList add(String path, Runnable action, boolean enabled) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path is required");
        }
        String[] parts = path.split("/");
        Container parent = popup;
        for (int i = 0; i < parts.length - 1; i++) {
            parent = findOrCreate(parent, parts[i].trim());
        }
        JMenuItem leaf = new JMenuItem(parts[parts.length - 1].trim());
        leaf.setEnabled(enabled);
        if (action != null) {
            leaf.addActionListener(e -> {
                popup.setVisible(false);
                action.run();
            });
        }
        parent.add(leaf);
        return this;
    }

    @Override
    public SwingMenuList addSeparator() {
        popup.add(new JSeparator());
        return this;
    }

    @Override
    public SwingMenuList remove(String path) {
        if (path == null || path.isBlank()) {
            return this;
        }
        String[] parts = path.split("/");
        Container parent = popup;
        for (int i = 0; i < parts.length - 1; i++) {
            Container next = findMenu(parent, parts[i].trim());
            if (next == null) {
                return this;
            }
            parent = next;
        }
        Component child = findChild(parent, parts[parts.length - 1].trim());
        if (child != null) {
            parent.remove(child);
            popup.revalidate();
            popup.repaint();
        }
        return this;
    }

    @Override
    public SwingMenuList setEnabled(boolean enabled) {
        trigger.setEnabled(enabled);
        return this;
    }

    @Override
    public SwingMenuList clear() {
        popup.removeAll();
        popup.revalidate();
        popup.repaint();
        return this;
    }

    @Override
    public JButton peer() {
        return trigger;
    }

    JPopupMenu popup() {
        return popup;
    }

    private static Container findOrCreate(Container parent, String text) {
        Container existing = findMenu(parent, text);
        if (existing != null) {
            return existing;
        }
        JMenu created = new JMenu(text);
        parent.add(created);
        return created;
    }

    private static Container findMenu(Container parent, String text) {
        for (Component component : children(parent)) {
            if (component instanceof JMenu menu && text.equals(menu.getText())) {
                return menu;
            }
        }
        return null;
    }

    private static Component findChild(Container parent, String text) {
        for (Component component : children(parent)) {
            if (component instanceof JMenuItem item && text.equals(item.getText())) {
                return component;
            }
            if (component instanceof JSeparator) {
                continue;
            }
        }
        return null;
    }

    private static Component[] children(Container parent) {
        return parent instanceof JMenu menu
                ? menu.getMenuComponents()
                : parent.getComponents();
    }
}
