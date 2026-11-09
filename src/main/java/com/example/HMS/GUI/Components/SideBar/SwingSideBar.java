package com.example.HMS.GUI.Components.SideBar;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Default {@link SideBar} implementation.
 *
 * <p>Items live inside a vertical scroll pane so long navigation lists remain
 * usable. Use {@link #setActive(int)} to highlight the current destination.</p>
 */
public class SwingSideBar implements SideBar<JScrollPane> {
    private static final Color ACTIVE_BACKGROUND = new Color(37, 99, 235);
    private static final Color SECTION_COLOR = new Color(110, 115, 125);

    private final JPanel panel = new JPanel() {
        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            return new Dimension(width, size.height);
        }
    };
    private final JScrollPane scroll;
    private final List<JButton> items = new ArrayList<>();
    private final List<JLabel> sections = new ArrayList<>();
    private final List<Object> blocks = new ArrayList<>();

    private Dock dock = Dock.LEFT;
    private Arrangement arrangement = Arrangement.GRID;
    private Alignment itemAlignment = Alignment.LEFT;
    private ItemWidth itemWidth = ItemWidth.FILL;

    private int gap = 4;
    private int width = 160;
    private int activeIndex = -1;

    public SwingSideBar() {
        scroll = new JScrollPane(panel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(panel.getBackground());
        applyWidth();
        rebuild();
    }

    @Override
    public SwingSideBar setWidth(double w) {
        width = Math.max(1, (int) w);
        applyWidth();
        return this;
    }

    @Override
    public SwingSideBar setSpacing(double spacing) {
        gap = Math.max(0, (int) spacing);
        rebuild();
        return this;
    }

    @Override
    public SwingSideBar setDock(Dock dock) {
        if (dock != Dock.LEFT && dock != Dock.RIGHT) {
            throw new IllegalArgumentException("dock must be LEFT or RIGHT");
        }
        this.dock = dock;
        return this;
    }

    @Override
    public Dock dock() {
        return dock;
    }

    @Override
    public SwingSideBar setArrangement(Arrangement a) {
        arrangement = a;
        rebuild();
        return this;
    }

    @Override
    public Arrangement arrangement() {
        return arrangement;
    }

    @Override
    public SwingSideBar setItemAlignment(Alignment a) {
        itemAlignment = a;
        rebuild();
        return this;
    }

    @Override
    public Alignment itemAlignment() {
        return itemAlignment;
    }

    @Override
    public SwingSideBar setItemWidth(ItemWidth w) {
        itemWidth = w;
        rebuild();
        return this;
    }

    @Override
    public ItemWidth itemWidth() {
        return itemWidth;
    }

    @Override
    public SwingSideBar addItem(String label, Runnable action) {
        return addItem(label, null, action);
    }

    @Override
    public SwingSideBar addItem(String label, Icon icon, Runnable action) {
        JButton button = new JButton(label);
        if (icon != null) {
            button.setIcon(icon);
        }
        if (action != null) {
            button.addActionListener(e -> action.run());
        }
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setHorizontalAlignment(swingAlign());
        items.add(button);
        blocks.add(button);
        if (activeIndex < 0) {
            activeIndex = 0;
        }
        rebuild();
        return this;
    }

    @Override
    public SwingSideBar addSection(String title) {
        JLabel label = new JLabel(title == null ? "" : title);
        label.setForeground(SECTION_COLOR);
        label.setFont(label.getFont().deriveFont(Font.BOLD, label.getFont().getSize2D() - 1f));
        label.setBorder(BorderFactory.createEmptyBorder(gap, 4, gap, 4));
        sections.add(label);
        blocks.add(label);
        rebuild();
        return this;
    }

    @Override
    public SwingSideBar setActive(int index) {
        activeIndex = index >= 0 && index < items.size() ? index : -1;
        styleItems();
        return this;
    }

    @Override
    public int activeIndex() {
        return activeIndex;
    }

    @Override
    public SwingSideBar clear() {
        items.clear();
        sections.clear();
        blocks.clear();
        activeIndex = -1;
        rebuild();
        return this;
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public JScrollPane peer() {
        return scroll;
    }

    private void applyWidth() {
        scroll.setPreferredSize(new Dimension(width, 120));
        scroll.setMinimumSize(new Dimension(width, 0));
        panel.revalidate();
    }

    private void rebuild() {
        panel.removeAll();
        panel.setLayout(switch (arrangement) {
            case BOX_Y -> new BoxLayout(panel, BoxLayout.Y_AXIS);
            case BOX_X -> new BoxLayout(panel, BoxLayout.X_AXIS);
            case FLOW -> new FlowLayout(swingAlign(), gap, gap);
            default -> new GridLayout(0, 1, 0, gap);
        });

        boolean firstItem = true;
        for (Object block : blocks) {
            if (block instanceof JLabel section) {
                panel.add(wrapSection(section));
            } else if (block instanceof JButton button) {
                panel.add(wrap(button, !firstItem));
                firstItem = false;
            }
        }
        styleItems();
        applyWidth();
        panel.revalidate();
        panel.repaint();
    }

    private void styleItems() {
        Color activeForeground = Color.WHITE;
        Color normalForeground = UIManager.getColor("Button.foreground");
        Font normalFont = UIManager.getFont("Button.font");
        for (int i = 0; i < items.size(); i++) {
            JButton button = items.get(i);
            boolean active = i == activeIndex;
            button.setBackground(active ? ACTIVE_BACKGROUND : UIManager.getColor("Panel.background"));
            button.setForeground(active ? activeForeground : normalForeground);
            button.setFont(normalFont);
            button.setHorizontalAlignment(swingAlign());
        }
    }

    private JPanel wrapSection(JLabel section) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(section, BorderLayout.CENTER);
        if (arrangement == Arrangement.BOX_Y || arrangement == Arrangement.BOX_X) {
            wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, wrap.getPreferredSize().height));
        }
        return wrap;
    }

    private JPanel wrap(JButton button, boolean pad) {
        boolean verticalKind =
                arrangement == Arrangement.GRID || arrangement == Arrangement.BOX_Y;
        JPanel wrap = itemWidth == ItemWidth.FILL
                ? new JPanel(new BorderLayout())
                : new JPanel(new FlowLayout(swingAlign(), 0, 0));
        wrap.setOpaque(false);
        if (pad) {
            wrap.setBorder(verticalKind
                    ? BorderFactory.createEmptyBorder(gap, 0, 0, 0)
                    : BorderFactory.createEmptyBorder(0, gap, 0, 0));
        }
        wrap.add(button);
        Dimension pref = wrap.getPreferredSize();
        if (arrangement == Arrangement.BOX_Y) {
            wrap.setMaximumSize(new Dimension(
                    itemWidth == ItemWidth.FILL ? Integer.MAX_VALUE : pref.width,
                    pref.height));
        } else if (arrangement == Arrangement.BOX_X) {
            wrap.setMaximumSize(new Dimension(
                    pref.width,
                    itemWidth == ItemWidth.FILL ? Integer.MAX_VALUE : pref.height));
        }
        return wrap;
    }

    private int swingAlign() {
        return switch (itemAlignment) {
            case CENTER -> SwingConstants.CENTER;
            case RIGHT -> SwingConstants.RIGHT;
            default -> SwingConstants.LEFT;
        };
    }
}
