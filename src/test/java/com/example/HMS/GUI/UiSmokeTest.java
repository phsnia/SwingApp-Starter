package com.example.HMS.GUI;

import com.example.HMS.GUI.Components.DataTable.SwingDataTable;
import com.example.HMS.GUI.Screens.LoginScreen;
import com.example.HMS.GUI.Screens.MainScreen;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.lang.reflect.InvocationTargetException;

import static javax.swing.SwingUtilities.invokeAndWait;
import static org.assertj.core.api.Assertions.assertThat;

class UiSmokeTest {

    @Test
    void factoriesCreateEveryComponentHeadlessly() {
        onEdt(() -> {
            UI ui = new UI();

            assertThat(ui.textbox().getText()).isEmpty();
            assertThat(ui.passwordBox().getText()).isEmpty();
            assertThat(ui.dropMenu("A", "B").count()).isEqualTo(2);
            assertThat(ui.menuList("Actions").peer().getText()).isEqualTo("Actions");
            assertThat(ui.sidebar().peer()).isNotNull();

            SwingDataTable<Object> table = ui.table();
            assertThat(table.count()).isZero();
        });
    }

    @Test
    void mainScreenNavigationAddsOneSidebarItemPerScreen() {
        onEdt(() -> {
            UI ui = new UI();
            MainScreen main = new MainScreen(ui);
            main.add("First", new JPanel());
            main.add("Second", new JPanel());

            assertThat(main.sidebar().size()).isEqualTo(2);
            assertThat(main.sidebar().activeIndex()).isZero();
        });
    }

    @Test
    void loginScreenIsConstructibleWithoutADisplay() {
        onEdt(() -> {
            LoginScreen screen = new LoginScreen(new UI());
            assertThat(screen.getComponentCount()).isEqualTo(1);
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
