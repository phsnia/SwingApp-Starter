package com.example.HMS.GUI.Components.DataTable;

import javax.swing.JComponent;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Table of typed items with columns, selection, search, sorting, paging and
 * optional CRUD handling.
 *
 * @param <T> row item type
 * @param <P> Swing peer type exposed by {@link #peer()}
 */
public interface DataTable<T, P extends JComponent> {

    DataTable<T, P> addColumn(Column<T, ?> column);

    <V> DataTable<T, P> addColumn(String title, Class<V> type, Function<T, V> getter);

    DataTable<T, P> setItems(List<T> items);

    DataTable<T, P> addItem(T item);

    DataTable<T, P> removeItem(int index);

    DataTable<T, P> updateItem(int index, T item);

    DataTable<T, P> clear();

    List<T> items();

    T itemAt(int index);

    int count();

    int filteredCount();

    DataTable<T, P> onSelect(Consumer<T> handler);

    DataTable<T, P> onDoubleClick(Consumer<T> handler);

    DataTable<T, P> setCreateHandler(Supplier<Optional<T>> handler);

    DataTable<T, P> setEditHandler(Function<T, Optional<T>> handler);

    /**
     * Called after the user confirms deletion, before the row is removed from
     * the local model. Screens use it to persist the deletion.
     */
    DataTable<T, P> setDeleteHandler(Consumer<T> handler);

    DataTable<T, P> setCrudEnabled(boolean enabled);

    DataTable<T, P> setSelectionMode(SelectionMode mode);

    DataTable<T, P> setSortable(boolean sortable);

    DataTable<T, P> setSearchText(String text);

    DataTable<T, P> setPageSize(int size);

    DataTable<T, P> setPageIndex(int index);

    DataTable<T, P> nextPage();

    DataTable<T, P> previousPage();

    int currentPage();

    int pageCount();

    P peer();
}
