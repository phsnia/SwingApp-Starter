package com.example.HMS.GUI;

import com.example.HMS.GUI.Components.DataTable.SwingDataTable;
import com.example.HMS.GUI.Components.DropMenu.SwingDropMenu;
import com.example.HMS.GUI.Components.Form.FormDialog;
import com.example.HMS.GUI.Components.MenuList.SwingMenuList;
import com.example.HMS.GUI.Components.SideBar.SwingSideBar;
import com.example.HMS.GUI.Components.TextBox.impl.SwingTextBox;

import java.awt.Window;

/**
 * Single entry point for creating components, so screens never instantiate
 * Swing implementations directly.
 */
public class UI {

    public SwingApp app(String title) {
        return new SwingApp(title);
    }

    public SwingTextBox textbox() {
        return new SwingTextBox();
    }

    public SwingTextBox textbox(String text) {
        return new SwingTextBox(text);
    }

    public SwingTextBox passwordBox() {
        return new SwingTextBox().password(true);
    }

    public SwingDropMenu dropMenu(String... items) {
        return new SwingDropMenu(items);
    }

    public SwingMenuList menuList(String label) {
        return new SwingMenuList(label);
    }

    public SwingSideBar sidebar() {
        return new SwingSideBar();
    }

    public <T> SwingDataTable<T> table() {
        return new SwingDataTable<>();
    }

    public <T> FormDialog<T> form(Window owner, String title) {
        return FormDialog.create(owner, title);
    }
}
