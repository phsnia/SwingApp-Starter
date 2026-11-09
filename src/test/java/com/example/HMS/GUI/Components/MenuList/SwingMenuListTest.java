package com.example.HMS.GUI.Components.MenuList;

import org.junit.jupiter.api.Test;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import java.lang.reflect.InvocationTargetException;

import static javax.swing.SwingUtilities.invokeAndWait;
import static org.assertj.core.api.Assertions.assertThat;

class SwingMenuListTest {

    @Test
    void duplicatePrefixesShareOneSubmenu() {
        onEdt(() -> {
            SwingMenuList menu = new SwingMenuList("File");
            menu.add("File/New", () -> {
            });
            menu.add("File / Open", () -> {
            });

            JPopupMenu popup = menu.popup();
            assertThat(popup.getComponentCount()).isEqualTo(1);

            JMenu file = (JMenu) popup.getComponent(0);
            assertThat(file.getText()).isEqualTo("File");
            assertThat(file.getMenuComponentCount()).isEqualTo(2);
            assertThat(file.getMenuComponent(1)).isInstanceOf(javax.swing.JMenuItem.class);
        });
    }

    @Test
    void removeDeletesOnlyTheRequestedLeaf() {
        onEdt(() -> {
            SwingMenuList menu = new SwingMenuList("File");
            menu.add("File/New", () -> {
            });
            menu.add("File/Open", () -> {
            });

            menu.remove("File/Open");

            JMenu file = (JMenu) menu.popup().getComponent(0);
            assertThat(file.getMenuComponentCount()).isEqualTo(1);
            assertThat(((javax.swing.JMenuItem) file.getMenuComponent(0)).getText()).isEqualTo("New");
        });
    }

    @Test
    void separatorsAndDisabledItemsAreSupported() {
        onEdt(() -> {
            SwingMenuList menu = new SwingMenuList("Edit");
            menu.add("Undo", () -> {
            })
                    .addSeparator()
                    .add("Redo", () -> {
                    }, false);

            assertThat(menu.popup().getComponentCount()).isEqualTo(3);
            assertThat(menu.popup().getComponent(1)).isInstanceOf(javax.swing.JSeparator.class);
            assertThat(menu.popup().getComponent(2).isEnabled()).isFalse();
        });
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
}
