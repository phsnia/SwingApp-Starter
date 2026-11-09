package com.example.HMS.GUI.Components.DropMenu;

import javax.swing.JComboBox;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Default {@link DropMenu} implementation backed by {@link JComboBox}.
 */
public class SwingDropMenu implements DropMenu<JComboBox<String>> {
    private final JComboBox<String> box;
    private final List<Consumer<String>> listeners = new ArrayList<>();
    private final List<String> items = new ArrayList<>();

    private String placeholder;
    private boolean updating;

    public SwingDropMenu(String... items) {
        box = new JComboBox<>();
        this.items.addAll(Arrays.asList(items));
        box.addActionListener(e -> {
            if (updating) {
                return;
            }
            String value = selected();
            if (value != null) {
                for (Consumer<String> listener : listeners) {
                    listener.accept(value);
                }
            }
        });
        applyItems();
    }

    @Override
    public SwingDropMenu setItems(String... items) {
        this.items.clear();
        if (items != null) {
            this.items.addAll(Arrays.asList(items));
        }
        applyItems();
        return this;
    }

    @Override
    public SwingDropMenu select(int index) {
        if (index < 0 || index >= items.size()) {
            clearSelection();
            return this;
        }
        box.setSelectedIndex(index + offset());
        return this;
    }

    @Override
    public String selected() {
        int index = box.getSelectedIndex();
        if (index < 0 || index < offset()) {
            return null;
        }
        return box.getItemAt(index);
    }

    @Override
    public int selectedIndex() {
        int index = box.getSelectedIndex();
        if (index < offset()) {
            return -1;
        }
        return index - offset();
    }

    @Override
    public int count() {
        return items.size();
    }

    @Override
    public SwingDropMenu clearSelection() {
        updating = true;
        try {
            box.setSelectedIndex(placeholder == null ? -1 : 0);
        } finally {
            updating = false;
        }
        return this;
    }

    @Override
    public SwingDropMenu setEnabled(boolean enabled) {
        box.setEnabled(enabled);
        return this;
    }

    @Override
    public SwingDropMenu setPlaceholder(String text) {
        placeholder = text;
        applyItems();
        return this;
    }

    @Override
    public SwingDropMenu onSelect(Consumer<String> fn) {
        listeners.add(Objects.requireNonNull(fn, "fn"));
        return this;
    }

    @Override
    public JComboBox<String> peer() {
        return box;
    }

    private int offset() {
        return placeholder == null ? 0 : 1;
    }

    private void applyItems() {
        updating = true;
        try {
            box.removeAllItems();
            if (placeholder != null) {
                box.addItem(placeholder);
            }
            for (String item : items) {
                box.addItem(item);
            }
            if (placeholder != null) {
                box.setSelectedIndex(0);
            } else if (!items.isEmpty()) {
                box.setSelectedIndex(0);
            } else {
                box.setSelectedIndex(-1);
            }
        } finally {
            updating = false;
        }
    }
}
