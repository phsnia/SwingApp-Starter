package com.example.HMS.GUI.Screens;

import com.example.HMS.GUI.Components.SideBar.SwingSideBar;
import com.example.HMS.GUI.Components.Core.Theme;
import com.example.HMS.GUI.Components.Core.UI;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Sidebar navigation with a swappable content panel.
 */
public final class MainScreen extends JPanel {
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final SwingSideBar sidebar;
    private final List<String> keys = new ArrayList<>();

    public MainScreen(UI ui) {
        super(new BorderLayout(Theme.GAP, 0));
        setOpaque(false);

        sidebar = ui.sidebar();
        sidebar.setWidth(190);

        content.setOpaque(false);
        add(sidebar.peer(), BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
    }

    public MainScreen add(String label, JComponent screen) {
        String key = "screen-" + keys.size();
        keys.add(key);
        content.add(screen, key);

        int index = sidebar.size();
        sidebar.addItem(label, () -> select(index));
        if (keys.size() == 1) {
            select(0);
        }
        return this;
    }

    public void select(int index) {
        if (index < 0 || index >= keys.size()) {
            return;
        }
        sidebar.setActive(index);
        cards.show(content, keys.get(index));
    }

    public SwingSideBar sidebar() {
        return sidebar;
    }
}
