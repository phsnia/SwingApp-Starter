package com.example.HMS.GUI.Components.DataTable;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * Default {@link DataTable} implementation backed by a {@link JTable} and an
 * {@link AbstractTableModel}. It owns search, sorting, paging, and the optional
 * CRUD UI; persistence is injected through handlers.
 *
 * @param <T> row item type
 */
public class SwingDataTable<T> implements DataTable<T, JPanel> {
    private final List<T> data = new ArrayList<>();
    private final List<Column<T, ?>> columns = new ArrayList<>();
    private final List<Integer> filteredSourceIndices = new ArrayList<>();
    private final List<Integer> pageSourceIndices = new ArrayList<>();
    private final List<T> pageItems = new ArrayList<>();

    private final JPanel root = new JPanel(new BorderLayout(5, 5));
    private final JPanel toolbar = new JPanel(new BorderLayout(5, 5));
    private final JPanel crudToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    private final JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    private final JTextField searchField = new JTextField(20);
    private final JComboBox<String> pageSizeBox = new JComboBox<>(new String[]{"5", "10", "20"});
    private final JButton addButton = new JButton("Add");
    private final JPopupMenu rowMenu = new JPopupMenu();
    private final JMenuItem editItem = new JMenuItem("Edit");
    private final JMenuItem deleteItem = new JMenuItem("Delete");

    private final JTable table;
    private final Model model = new Model();

    private Consumer<T> selectHandler;
    private Consumer<T> doubleClickHandler;
    private Supplier<Optional<T>> createHandler;
    private Function<T, Optional<T>> editHandler;
    private Consumer<T> deleteHandler;

    private String searchText = "";
    private int pageSize = 10;
    private int pageIndex = 0;

    private boolean sortable = true;
    private boolean updatingView = false;
    private boolean updatingSearchField = false;
    private boolean updatingPageSize = false;
    private boolean crudEnabled = false;

    private int sortColumn = -1;
    private boolean ascending = true;

    public SwingDataTable() {
        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable source, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(source, value,
                        isSelected, hasFocus, row, column);
                String text = value == null ? "" : value.toString();
                int modelColumn = source.convertColumnIndexToModel(column);
                if (sortable && modelColumn == sortColumn) {
                    text += ascending ? " \u25B2" : " \u25BC";
                }
                label.setText(text);
                label.setHorizontalAlignment(SwingConstants.LEFT);
                return label;
            }
        });

        searchField.setToolTipText("Search...");
        toolbar.add(new JLabel("Search:"), BorderLayout.WEST);
        toolbar.add(searchField, BorderLayout.CENTER);
        crudToolbar.add(addButton);
        crudToolbar.setVisible(false);
        toolbar.add(crudToolbar, BorderLayout.EAST);

        addButton.addActionListener(e -> createItem());
        editItem.addActionListener(e -> editSelectedItem());
        deleteItem.addActionListener(e -> deleteSelectedItem());
        rowMenu.add(editItem);
        rowMenu.add(deleteItem);

        pageSizeBox.setSelectedItem(String.valueOf(pageSize));
        pageSizeBox.addActionListener(e -> {
            if (updatingPageSize) {
                return;
            }
            Object sel = pageSizeBox.getSelectedItem();
            if (sel != null) {
                setPageSize(Integer.parseInt(sel.toString()));
            }
        });

        root.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        root.add(toolbar, BorderLayout.NORTH);
        root.add(new JScrollPane(table), BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(javax.swing.KeyStroke.getKeyStroke("control F"), "focus-search");
        root.getActionMap().put("focus-search", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                searchField.requestFocusInWindow();
                searchField.selectAll();
            }
        });

        searchField.addActionListener(e -> setSearchText(searchField.getText()));
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void changed() {
                if (!updatingSearchField) {
                    setSearchText(searchField.getText());
                }
            }

            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                changed();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                changed();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                changed();
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || updatingView) {
                return;
            }
            int row = table.getSelectedRow();
            if (row >= 0 && row < pageItems.size() && selectHandler != null) {
                selectHandler.accept(pageItems.get(row));
            }
            updateCrudButtons();
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                if (e.getClickCount() != 2) {
                    return;
                }
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0 && row < pageItems.size() && doubleClickHandler != null) {
                    doubleClickHandler.accept(pageItems.get(row));
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }
        });

        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!sortable) {
                    return;
                }
                int viewColumn = table.getColumnModel().getColumnIndexAtX(e.getX());
                if (viewColumn < 0) {
                    return;
                }
                int modelColumn = table.convertColumnIndexToModel(viewColumn);
                if (modelColumn == sortColumn) {
                    ascending = !ascending;
                } else {
                    sortColumn = modelColumn;
                    ascending = true;
                }
                refreshView();
            }
        });

        refreshView();
    }

    @Override
    public SwingDataTable<T> addColumn(Column<T, ?> column) {
        Objects.requireNonNull(column, "column");
        columns.add(column);
        model.fireTableStructureChanged();
        configureColumns();
        refreshView();
        return this;
    }

    @Override
    public <V> SwingDataTable<T> addColumn(String title, Class<V> type, Function<T, V> getter) {
        return addColumn(Column.of(title, type, getter));
    }

    @Override
    public SwingDataTable<T> setItems(List<T> items) {
        data.clear();
        if (items != null) {
            data.addAll(items);
        }
        pageIndex = 0;
        refreshView();
        return this;
    }

    @Override
    public SwingDataTable<T> addItem(T item) {
        data.add(item);
        refreshView();
        return this;
    }

    @Override
    public SwingDataTable<T> removeItem(int index) {
        checkDataIndex(index);
        data.remove(index);
        refreshView();
        return this;
    }

    @Override
    public SwingDataTable<T> updateItem(int index, T item) {
        checkDataIndex(index);
        data.set(index, item);
        refreshView();
        return this;
    }

    @Override
    public SwingDataTable<T> clear() {
        data.clear();
        pageIndex = 0;
        refreshView();
        return this;
    }

    @Override
    public List<T> items() {
        return Collections.unmodifiableList(new ArrayList<>(data));
    }

    @Override
    public T itemAt(int index) {
        checkDataIndex(index);
        return data.get(index);
    }

    @Override
    public int count() {
        return data.size();
    }

    @Override
    public int filteredCount() {
        return filteredSourceIndices.size();
    }

    @Override
    public SwingDataTable<T> onSelect(Consumer<T> handler) {
        selectHandler = handler;
        return this;
    }

    @Override
    public SwingDataTable<T> onDoubleClick(Consumer<T> handler) {
        doubleClickHandler = handler;
        return this;
    }

    @Override
    public SwingDataTable<T> setCreateHandler(Supplier<Optional<T>> handler) {
        createHandler = handler;
        updateCrudButtons();
        return this;
    }

    @Override
    public SwingDataTable<T> setEditHandler(Function<T, Optional<T>> handler) {
        editHandler = handler;
        updateCrudButtons();
        return this;
    }

    @Override
    public SwingDataTable<T> setDeleteHandler(Consumer<T> handler) {
        deleteHandler = handler;
        updateCrudButtons();
        return this;
    }

    @Override
    public SwingDataTable<T> setCrudEnabled(boolean enabled) {
        crudEnabled = enabled;
        crudToolbar.setVisible(enabled);
        updateCrudButtons();
        toolbar.revalidate();
        toolbar.repaint();
        return this;
    }

    @Override
    public SwingDataTable<T> setSelectionMode(SelectionMode mode) {
        Objects.requireNonNull(mode, "mode");
        switch (mode) {
            case NONE -> {
                table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                table.setRowSelectionAllowed(false);
            }
            case SINGLE -> {
                table.setRowSelectionAllowed(true);
                table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            }
            case MULTIPLE -> {
                table.setRowSelectionAllowed(true);
                table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            }
        }
        table.clearSelection();
        updateCrudButtons();
        return this;
    }

    @Override
    public SwingDataTable<T> setSortable(boolean sortable) {
        this.sortable = sortable;
        return this;
    }

    /** Sorts by a model column index; {@code -1} clears sorting. */
    public SwingDataTable<T> sortBy(int columnIndex, boolean ascending) {
        if (columnIndex < -1 || columnIndex >= columns.size()) {
            throw new IndexOutOfBoundsException("Column index: " + columnIndex);
        }
        this.sortColumn = columnIndex;
        this.ascending = ascending;
        refreshView();
        return this;
    }

    public int sortColumn() {
        return sortColumn;
    }

    public boolean sortAscending() {
        return ascending;
    }

    @Override
    public SwingDataTable<T> setSearchText(String text) {
        String raw = text == null ? "" : text;
        searchText = raw.trim().toLowerCase(Locale.ROOT);
        pageIndex = 0;
        if (!updatingSearchField && !searchField.getText().equals(raw)) {
            updatingSearchField = true;
            try {
                searchField.setText(raw);
            } finally {
                updatingSearchField = false;
            }
        }
        refreshView();
        return this;
    }

    @Override
    public SwingDataTable<T> setPageSize(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("Page size must be positive");
        }
        pageSize = size;
        pageIndex = 0;
        if (!String.valueOf(size).equals(pageSizeBox.getSelectedItem())) {
            updatingPageSize = true;
            try {
                pageSizeBox.setSelectedItem(String.valueOf(size));
            } finally {
                updatingPageSize = false;
            }
        }
        refreshView();
        return this;
    }

    @Override
    public SwingDataTable<T> setPageIndex(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Page index cannot be negative");
        }
        pageIndex = index;
        refreshView();
        return this;
    }

    @Override
    public SwingDataTable<T> nextPage() {
        if (pageIndex + 1 < pageCount()) {
            pageIndex++;
            refreshView();
        }
        return this;
    }

    @Override
    public SwingDataTable<T> previousPage() {
        if (pageIndex > 0) {
            pageIndex--;
            refreshView();
        }
        return this;
    }

    @Override
    public int currentPage() {
        return pageIndex;
    }

    @Override
    public int pageCount() {
        return (filteredSourceIndices.size() + pageSize - 1) / pageSize;
    }

    @Override
    public JPanel peer() {
        return root;
    }

    JTable table() {
        return table;
    }

    private void checkDataIndex(int index) {
        if (index < 0 || index >= data.size()) {
            throw new IndexOutOfBoundsException("Data index: " + index);
        }
    }

    private void createItem() {
        if (!crudEnabled || createHandler == null) {
            return;
        }
        Optional<T> result = Objects.requireNonNull(createHandler.get(), "createHandler result");
        result.ifPresent(this::addItem);
    }

    private void editSelectedItem() {
        int sourceIndex = selectedSourceIndex();
        if (!crudEnabled || editHandler == null || sourceIndex < 0) {
            return;
        }
        Optional<T> result = Objects.requireNonNull(
                editHandler.apply(data.get(sourceIndex)), "editHandler result");
        result.ifPresent(item -> updateItem(sourceIndex, item));
    }

    private void deleteSelectedItem() {
        int sourceIndex = selectedSourceIndex();
        if (!crudEnabled || sourceIndex < 0) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(root, "Delete the selected item?",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            T removed = data.get(sourceIndex);
            if (deleteHandler != null) {
                deleteHandler.accept(removed);
            }
            removeItem(sourceIndex);
        }
    }

    private int selectedSourceIndex() {
        int[] selectedRows = table.getSelectedRows();
        if (!table.getRowSelectionAllowed() || selectedRows.length != 1
                || selectedRows[0] >= pageSourceIndices.size()) {
            return -1;
        }
        return pageSourceIndices.get(selectedRows[0]);
    }

    private void updateCrudButtons() {
        boolean hasSelection = selectedSourceIndex() >= 0;
        addButton.setEnabled(crudEnabled && createHandler != null);
        editItem.setEnabled(crudEnabled && editHandler != null && hasSelection);
        deleteItem.setEnabled(crudEnabled && hasSelection);
    }

    private void maybeShowPopup(MouseEvent e) {
        if (!e.isPopupTrigger() || !crudEnabled) {
            return;
        }
        int row = table.rowAtPoint(e.getPoint());
        if (row < 0 || row >= pageItems.size()) {
            return;
        }
        if (!isRowSelected(row)) {
            table.setRowSelectionInterval(row, row);
        }
        updateCrudButtons();
        rowMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    private boolean isRowSelected(int row) {
        for (int selected : table.getSelectedRows()) {
            if (selected == row) {
                return true;
            }
        }
        return false;
    }

    private void configureColumns() {
        for (int i = 0; i < columns.size(); i++) {
            final Column<T, ?> column = columns.get(i);
            TableColumn swingColumn = table.getColumnModel().getColumn(i);
            swingColumn.setPreferredWidth(column.preferredWidth());
            if (column.cellRenderer() != null) {
                swingColumn.setCellRenderer(column.cellRenderer());
            } else {
                swingColumn.setCellRenderer(new DefaultTableCellRenderer() {
                    @Override
                    public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                                                                            boolean isSelected, boolean hasFocus, int row, int columnIndex) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                                columnIndex);
                        setText(column.format(value));
                        return this;
                    }
                });
            }
            if (column.cellEditor() != null) {
                swingColumn.setCellEditor(column.cellEditor());
            }
        }
    }

    private void refreshView() {
        updatingView = true;
        try {
            filteredSourceIndices.clear();
            for (int i = 0; i < data.size(); i++) {
                if (matchesSearch(data.get(i))) {
                    filteredSourceIndices.add(i);
                }
            }
            if (sortable && sortColumn >= 0 && sortColumn < columns.size()) {
                final Column<T, ?> column = columns.get(sortColumn);
                Comparator<Integer> comparator = (first, second) ->
                        column.compare(data.get(first), data.get(second));
                if (!ascending) {
                    comparator = comparator.reversed();
                }
                filteredSourceIndices.sort(comparator);
            }
            int totalPages = pageCount();
            if (totalPages == 0) {
                pageIndex = 0;
            } else if (pageIndex >= totalPages) {
                pageIndex = totalPages - 1;
            }
            int from = Math.min(pageIndex * pageSize, filteredSourceIndices.size());
            int to = Math.min(from + pageSize, filteredSourceIndices.size());
            pageSourceIndices.clear();
            pageSourceIndices.addAll(filteredSourceIndices.subList(from, to));
            pageItems.clear();
            for (int sourceIndex : pageSourceIndices) {
                pageItems.add(data.get(sourceIndex));
            }
            model.fireTableDataChanged();
            table.clearSelection();
            table.getTableHeader().repaint();
            updateCrudButtons();
            renderFooter();
        } finally {
            updatingView = false;
        }
    }

    private void renderFooter() {
        footer.removeAll();
        int total = pageCount();
        footer.add(new JLabel("Size:"));
        footer.add(pageSizeBox);
        footer.add(pager("<<", pageIndex > 0, () -> setPageIndex(0)));
        footer.add(pager("<", pageIndex > 0, () -> previousPage()));
        for (Object slot : slots()) {
            if (slot instanceof Integer p) {
                footer.add(pager(String.valueOf(p + 1), p != pageIndex, () -> setPageIndex(p)));
            } else {
                footer.add(new JLabel("..."));
            }
        }
        footer.add(pager(">", pageIndex + 1 < total, () -> nextPage()));
        footer.add(pager(">>", pageIndex + 1 < total, () -> setPageIndex(total - 1)));
        footer.revalidate();
        footer.repaint();
    }

    private List<Object> slots() {
        int total = pageCount();
        java.util.Set<Integer> keep = new java.util.TreeSet<>();
        if (total == 0) {
            return List.of();
        }
        keep.add(0);
        keep.add(total - 1);
        for (int p = pageIndex - 2; p <= pageIndex + 2; p++) {
            if (p >= 0 && p < total) {
                keep.add(p);
            }
        }
        List<Object> out = new ArrayList<>();
        int prev = -1;
        for (int p : keep) {
            if (p - prev > 1) {
                out.add("...");
            }
            out.add(p);
            prev = p;
        }
        return out;
    }

    private static JButton pager(String text, boolean enabled, Runnable action) {
        JButton b = new JButton(text);
        b.setEnabled(enabled);
        b.addActionListener(e -> action.run());
        return b;
    }

    private boolean matchesSearch(T item) {
        if (searchText.isEmpty()) {
            return true;
        }
        for (Column<T, ?> column : columns) {
            String text = column.format(column.valueOf(item)).toLowerCase(Locale.ROOT);
            if (text.contains(searchText)) {
                return true;
            }
        }
        return false;
    }

    private final class Model extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return pageItems.size();
        }

        @Override
        public int getColumnCount() {
            return columns.size();
        }

        @Override
        public String getColumnName(int column) {
            return columns.get(column).title();
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return columns.get(column).valueType();
        }

        @Override
        public Object getValueAt(int row, int column) {
            return columns.get(column).valueOf(pageItems.get(row));
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
}