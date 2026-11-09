package com.example.HMS.GUI.Components.DataTable;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static javax.swing.SwingUtilities.invokeAndWait;
import static org.assertj.core.api.Assertions.assertThat;

class SwingDataTableTest {

    @Test
    void itemsAndColumnsFeedTheTableModel() {
        onEdt(() -> {
            SwingDataTable<Row> table = newTable();
            table.setItems(List.of(new Row("Beta", 2), new Row("Alpha", 1)));

            assertThat(table.count()).isEqualTo(2);
            assertThat(table.itemAt(0)).isEqualTo(new Row("Beta", 2));
            assertThat(table.table().getColumnCount()).isEqualTo(2);
            assertThat(table.table().getRowCount()).isEqualTo(2);
            assertThat(table.table().getValueAt(0, 0)).isEqualTo("Beta");
            assertThat(table.table().getColumnName(1)).isEqualTo("Score");
        });
    }

    @Test
    void searchFiltersAcrossFormattedColumns() {
        onEdt(() -> {
            SwingDataTable<Row> table = newTable();
            table.setItems(List.of(
                    new Row("One Piece", 1),
                    new Row("Berserk", 2),
                    new Row("Monster", 3)));

            table.setSearchText("piece");
            assertThat(table.filteredCount()).isEqualTo(1);
            assertThat(table.table().getRowCount()).isEqualTo(1);
            assertThat(table.table().getValueAt(0, 0)).isEqualTo("One Piece");

            table.setSearchText("");
            assertThat(table.filteredCount()).isEqualTo(3);
        });
    }

    @Test
    void sortingTogglesDirectionAndResetsToTheFirstPage() {
        onEdt(() -> {
            SwingDataTable<Row> table = newTable();
            table.setItems(List.of(
                    new Row("b", 2),
                    new Row("a", 3),
                    new Row("c", 1)));

            table.sortBy(0, true);
            assertThat(table.sortColumn()).isZero();
            assertThat(table.sortAscending()).isTrue();
            assertThat(table.table().getValueAt(0, 0)).isEqualTo("a");

            table.sortBy(0, false);
            assertThat(table.table().getValueAt(0, 0)).isEqualTo("c");

            table.sortBy(1, true);
            assertThat(table.table().getValueAt(0, 1)).isEqualTo(1);
        });
    }

    @Test
    void pagingMovesBetweenSlotsAndClampsWhenDataShrinks() {
        onEdt(() -> {
            SwingDataTable<Row> table = newTable();
            table.setItems(List.of(
                    new Row("1", 1), new Row("2", 2), new Row("3", 3),
                    new Row("4", 4), new Row("5", 5), new Row("6", 6)));

            table.setPageSize(3);
            assertThat(table.pageCount()).isEqualTo(2);
            assertThat(table.currentPage()).isZero();
            assertThat(table.table().getRowCount()).isEqualTo(3);

            table.nextPage();
            assertThat(table.currentPage()).isEqualTo(1);
            table.nextPage();
            assertThat(table.currentPage()).isEqualTo(1);
            table.previousPage();
            assertThat(table.currentPage()).isZero();

            table.setPageSize(6);
            assertThat(table.pageCount()).isEqualTo(1);
        });
    }

    @Test
    void removingRowsClampsTheCurrentPage() {
        onEdt(() -> {
            SwingDataTable<Row> table = newTable();
            table.setItems(List.of(
                    new Row("1", 1), new Row("2", 2), new Row("3", 3),
                    new Row("4", 4), new Row("5", 5), new Row("6", 6)));
            table.setPageSize(3);
            table.setPageIndex(1);
            assertThat(table.currentPage()).isEqualTo(1);

            table.removeItem(5);
            table.removeItem(4);
            table.removeItem(3);
            table.removeItem(2);

            assertThat(table.count()).isEqualTo(2);
            assertThat(table.pageCount()).isEqualTo(1);
            assertThat(table.currentPage()).isZero();
        });
    }

    @Test
    void selectionCallbackReturnsTheClickedItem() {
        onEdt(() -> {
            SwingDataTable<Row> table = newTable();
            Row first = new Row("First", 1);
            table.setItems(List.of(first, new Row("Second", 2)));

            AtomicReference<Row> selected = new AtomicReference<>();
            table.onSelect(selected::set);

            table.table().setRowSelectionInterval(0, 0);

            assertThat(selected.get()).isEqualTo(first);
        });
    }

    @Test
    void columnFormattingAndComparisonAreUsedForDisplayAndSort() {
        Column<Row, String> column = Column.<Row, String>of("Title", String.class, Row::title)
                .formatter(String::toUpperCase);

        assertThat(column.format("abc")).isEqualTo("ABC");
        assertThat(column.format(null)).isEmpty();
        assertThat(column.compare(new Row("a", 0), new Row("b", 0))).isNegative();
    }

    private static SwingDataTable<Row> newTable() {
        SwingDataTable<Row> table = new SwingDataTable<>();
        table.addColumn(Column.of("Title", String.class, Row::title));
        table.addColumn(Column.of("Score", Integer.class, Row::score));
        return table;
    }

    private static void onEdt(Runnable action) {
        try {
            invokeAndWait(action);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(cause);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }
    }

    private record Row(String title, int score) {
    }
}
