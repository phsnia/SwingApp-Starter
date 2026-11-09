package com.example.HMS.GUI.Components.DropMenu;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static javax.swing.SwingUtilities.invokeAndWait;
import static org.assertj.core.api.Assertions.assertThat;

class SwingDropMenuTest {

    @Test
    void replacingItemsDoesNotFireAStaleSelection() {
        onEdt(() -> {
            SwingDropMenu menu = new SwingDropMenu("A", "B");
            List<String> events = new ArrayList<>();
            menu.onSelect(events::add);

            menu.setItems("X", "Y", "Z");

            assertThat(events).isEmpty();
        });
    }

    @Test
    void selectionFiresOnceAndReportsTheValue() {
        onEdt(() -> {
            SwingDropMenu menu = new SwingDropMenu("A", "B", "C");
            List<String> events = new ArrayList<>();
            menu.onSelect(events::add);

            menu.select(2);

            assertThat(events).containsExactly("C");
            assertThat(menu.selected()).isEqualTo("C");
            assertThat(menu.selectedIndex()).isEqualTo(2);
        });
    }

    @Test
    void placeholderIsShownButNeverReturnedAsAValue() {
        onEdt(() -> {
            SwingDropMenu menu = new SwingDropMenu()
                    .setPlaceholder("Choose")
                    .setItems("A", "B");

            assertThat(menu.count()).isEqualTo(2);
            assertThat(menu.selected()).isNull();
            assertThat(menu.selectedIndex()).isEqualTo(-1);

            menu.select(0);
            assertThat(menu.selected()).isEqualTo("A");
            assertThat(menu.selectedIndex()).isZero();

            menu.clearSelection();
            assertThat(menu.selected()).isNull();
        });
    }

    @Test
    void enabledFlagReachesThePeer() {
        onEdt(() -> {
            SwingDropMenu menu = new SwingDropMenu("A");

            menu.setEnabled(false);
            assertThat(menu.peer().isEnabled()).isFalse();

            menu.setEnabled(true);
            assertThat(menu.peer().isEnabled()).isTrue();
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
