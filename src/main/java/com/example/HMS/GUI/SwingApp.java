package com.example.HMS.GUI;

import com.example.HMS.GUI.Components.DataTable.DataTable;
import com.example.HMS.GUI.Components.DropMenu.DropMenu;
import com.example.HMS.GUI.Components.MenuList.MenuList;
import com.example.HMS.GUI.Components.SideBar.Dock;
import com.example.HMS.GUI.Components.SideBar.SideBar;
import com.example.HMS.GUI.Components.TextBox.TextBox;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * Frame shell with a header, a swappable content area, and a status bar.
 */
public class SwingApp {
    public static final int DEFAULT_WIDTH = 960;
    public static final int DEFAULT_HEIGHT = 600;
    public static final int MIN_WIDTH = 640;
    public static final int MIN_HEIGHT = 420;

    private final JFrame frame;
    private final JPanel root;
    private final JPanel header;
    private final JLabel titleLabel;
    private final JPanel headerActions;
    private final JPanel content;
    private final JPanel stack;
    private final JLabel statusLabel;

    public SwingApp(String title) {
        Theme.install();
        frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));

        root = new JPanel(new BorderLayout(Theme.GAP, 0));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(Theme.PAD, Theme.PAD, Theme.PAD, Theme.PAD));

        titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.TITLE_FONT);
        titleLabel.setForeground(Theme.TEXT);
        headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.GAP, 0));
        headerActions.setOpaque(false);
        header = new JPanel(new BorderLayout(Theme.GAP, 0));
        header.setOpaque(false);
        header.add(titleLabel, BorderLayout.WEST);
        header.add(headerActions, BorderLayout.EAST);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.GAP, 0));

        stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));

        content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(stack, BorderLayout.NORTH);

        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(Theme.MUTED_TEXT);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(Theme.GAP, 0, 0, 0));
        footer.add(statusLabel, BorderLayout.WEST);

        root.add(header, BorderLayout.NORTH);
        root.add(content, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        frame.setContentPane(root);
        frame.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public SwingApp size(int width, int height) {
        frame.setSize(width, height);
        return this;
    }

    public SwingApp minSize(int width, int height) {
        frame.setMinimumSize(new Dimension(width, height));
        return this;
    }

    public SwingApp title(String title) {
        titleLabel.setText(title);
        frame.setTitle(title);
        return this;
    }

    /** Replaces the content area, keeping the header, sidebar and footer. */
    public SwingApp setContent(JComponent component) {
        content.removeAll();
        content.add(component, BorderLayout.CENTER);
        refresh(content);
        return this;
    }

    public JPanel content() {
        return content;
    }

    public SwingApp setStatus(String message) {
        statusLabel.setText(message == null ? "" : message);
        return this;
    }

    /** Adds an action component (for example a MenuList button) to the header. */
    public SwingApp addHeaderAction(JComponent component) {
        headerActions.add(component);
        refresh(headerActions);
        return this;
    }

    public <T extends JComponent> void sidebar(SideBar<T> bar) {
        JComponent peer = bar.peer();
        String zone = bar.dock() == Dock.RIGHT ? BorderLayout.EAST : BorderLayout.WEST;
        root.add(peer, zone);
        refresh(root);
    }

    public void sidebar(JComponent component) {
        sidebar(component, false);
    }

    public void sidebar(JComponent component, boolean right) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(component, right ? BorderLayout.EAST : BorderLayout.WEST);
        root.add(wrap, right ? BorderLayout.EAST : BorderLayout.WEST);
        refresh(root);
    }

    public <T extends JComponent> void add(TextBox<T> component) {
        add(component.peer());
    }

    public <T extends JComponent> void add(SideBar<T> component) {
        add(component.peer());
    }

    public <T extends JComponent> void add(DropMenu<T> component) {
        add(component.peer());
    }

    public void add(JComponent component) {
        stack.add(component);
        stack.add(Box.createVerticalStrut(Theme.GAP));
        refresh(stack);
    }

    public <T extends JComponent> void add(TextBox<T> component, String zone) {
        add(component.peer(), zone);
    }

    public <T extends JComponent> void add(SideBar<T> component, String zone) {
        add(component.peer(), zone);
    }

    public <T extends JComponent> void add(DropMenu<T> component, String zone) {
        add(component.peer(), zone);
    }

    public <T extends JComponent> void add(MenuList<T> component) {
        add(component.peer());
    }

    public <T extends JComponent> void add(MenuList<T> component, String zone) {
        add(component.peer(), zone);
    }

    public <T, P extends JComponent> void add(DataTable<T, P> component) {
        add(component.peer());
    }

    public void add(JComponent component, String zone) {
        if (BorderLayout.WEST.equals(zone)) {
            sidebar(component);
        } else {
            root.add(component, zone);
            refresh(root);
        }
    }

    /** Centers the frame and shows it on the event dispatch thread. */
    public void center() {
        if (SwingUtilities.isEventDispatchThread()) {
            frame.setLocationRelativeTo(null);
        } else {
            SwingUtilities.invokeLater(() -> frame.setLocationRelativeTo(null));
        }
    }

    public void show() {
        if (SwingUtilities.isEventDispatchThread()) {
            showNow();
        } else {
            SwingUtilities.invokeLater(this::showNow);
        }
    }

    public JFrame frame() {
        return frame;
    }

    private void showNow() {
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void refresh(JComponent panel) {
        panel.revalidate();
        panel.repaint();
    }
}
