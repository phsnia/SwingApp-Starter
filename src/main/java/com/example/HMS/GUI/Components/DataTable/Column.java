package com.example.HMS.GUI.Components.DataTable;

import java.util.Comparator;
import java.util.function.Function;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 * Describes one table column: title, value type, getter, and optional
 * formatting, comparison, rendering, editing, and preferred width.
 *
 * @param <T> row item type
 * @param <V> cell value type
 */
public final class Column<T, V> {
    private final String title;
    private final Class<V> valueType;
    private final Function<T, V> getter;

    private Function<? super V, String> formatter;
    private Comparator<? super V> comparator;
    private TableCellRenderer renderer;
    private TableCellEditor editor;
    private int preferredWidth = 120;

    public Column(String title, Class<V> valueType, Function<T, V> getter) {
        if (title == null || valueType == null || getter == null) {
            throw new IllegalArgumentException("title, valueType and getter are required");
        }
        this.title = title;
        this.valueType = valueType;
        this.getter = getter;
    }

    public static <T, V> Column<T, V> of(String title, Class<V> type, Function<T, V> getter) {
        return new Column<>(title, type, getter);
    }

    public Column<T, V> formatter(Function<? super V, String> formatter) {
        this.formatter = formatter;
        return this;
    }

    public Column<T, V> comparator(Comparator<? super V> comparator) {
        this.comparator = comparator;
        return this;
    }

    public Column<T, V> renderer(TableCellRenderer renderer) {
        this.renderer = renderer;
        return this;
    }

    public Column<T, V> editor(TableCellEditor editor) {
        this.editor = editor;
        return this;
    }

    public Column<T, V> width(int width) {
        if (width < 1) {
            throw new IllegalArgumentException("width must be positive");
        }
        this.preferredWidth = width;
        return this;
    }

    public String title() {
        return title;
    }

    public Class<V> valueType() {
        return valueType;
    }

    public V valueOf(T item) {
        return getter.apply(item);
    }

    public String format(Object value) {
        if (value == null) {
            return "";
        }
        V typedValue = valueType.cast(value);
        if (formatter != null) {
            String result = formatter.apply(typedValue);
            return result == null ? "" : result;
        }
        return String.valueOf(value);
    }

    public int compare(T first, T second) {
        V a = valueOf(first);
        V b = valueOf(second);
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        if (comparator != null) {
            return comparator.compare(a, b);
        }
        if (a instanceof Comparable<?>) {
            @SuppressWarnings("unchecked")
            Comparable<Object> comparable = (Comparable<Object>) a;
            return comparable.compareTo(b);
        }
        return String.CASE_INSENSITIVE_ORDER.compare(String.valueOf(a), String.valueOf(b));
    }

    TableCellRenderer cellRenderer() {
        return renderer;
    }

    TableCellEditor cellEditor() {
        return editor;
    }

    int preferredWidth() {
        return preferredWidth;
    }
}