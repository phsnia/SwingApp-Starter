package com.example.HMS.GUI.Components.SideBar;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static javax.swing.SwingUtilities.invokeAndWait;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SwingSideBarTest {

    @Test
    void sectionsDoNotCountAsClickableItems() {
        onEdt(() -> {
            SwingSideBar bar = new SwingSideBar();
            bar.addSection("Navigation")
                    .addItem("Home", () -> {
                    })
                    .addItem("Manga", () -> {
                    });

            assertThat(bar.size()).isEqualTo(2);
            assertThat(bar.activeIndex()).isZero();
        });
    }

    @Test
    void activeIndexIsValidatedAndClearable() {
        onEdt(() -> {
            SwingSideBar bar = new SwingSideBar()
                    .addItem("One", () -> {
                    })
                    .addItem("Two", () -> {
                    });

            bar.setActive(1);
            assertThat(bar.activeIndex()).isEqualTo(1);

            bar.setActive(99);
            assertThat(bar.activeIndex()).isEqualTo(-1);
        });
    }

    @Test
    void layoutOptionsRoundTrip() {
        onEdt(() -> {
            SwingSideBar bar = new SwingSideBar()
                    .setDock(Dock.RIGHT)
                    .setArrangement(Arrangement.FLOW)
                    .setItemAlignment(Alignment.CENTER)
                    .setItemWidth(ItemWidth.CONTENT)
                    .setSpacing(6)
                    .setWidth(220);

            assertThat(bar.dock()).isEqualTo(Dock.RIGHT);
            assertThat(bar.arrangement()).isEqualTo(Arrangement.FLOW);
            assertThat(bar.itemAlignment()).isEqualTo(Alignment.CENTER);
            assertThat(bar.itemWidth()).isEqualTo(ItemWidth.CONTENT);
            assertThat(bar.peer().getPreferredSize().width).isEqualTo(220);
        });
    }

    @Test
    void dockRejectsInvalidValues() {
        assertThatThrownBy(() -> new SwingSideBar().setDock(null))
                .isInstanceOf(IllegalArgumentException.class);
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
