package com.example.HMS.GUI.Components.TextBox.impl;

import com.example.HMS.GUI.Components.TextBox.SwingTextBox;
import org.junit.jupiter.api.Test;

import javax.swing.JPasswordField;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static javax.swing.SwingUtilities.invokeAndWait;

class SwingTextBoxTest {

    @Test
    void promptNeverBecomesTheValue() {
        onEdt(() -> {
            SwingTextBox box = new SwingTextBox();
            box.setPrompt("Email");

            assertThat(box.getText()).isEmpty();

            box.setText("user@example.com");
            assertThat(box.getText()).isEqualTo("user@example.com");

            box.clear();
            assertThat(box.getText()).isEmpty();
        });
    }

    @Test
    void maxLengthFiltersTypedAndProgrammaticText() {
        onEdt(() -> {
            SwingTextBox box = new SwingTextBox().setMaxLength(5);

            box.setText("abcdefgh");
            assertThat(box.getText()).hasSize(5).isEqualTo("abcde");
        });
    }

    @Test
    void passwordFlagTogglesEchoCharacter() {
        onEdt(() -> {
            SwingTextBox box = new SwingTextBox();

            box.password(true);
            assertThat(((JPasswordField) box.peer()).getEchoChar()).isEqualTo('\u2022');

            box.password(false);
            assertThat(((JPasswordField) box.peer()).getEchoChar()).isEqualTo((char) 0);
        });
    }

    @Test
    void changeAndSubmitCallbacksReceiveTheValue() {
        onEdt(() -> {
            SwingTextBox box = new SwingTextBox();
            List<String> changes = new ArrayList<>();
            List<String> submits = new ArrayList<>();

            box.onChange(changes::add);
            box.onSubmit(submits::add);

            box.setText("hello");
            box.peer().postActionEvent();

            assertThat(changes).contains("hello");
            assertThat(submits).containsExactly("hello");
        });
    }

    @Test
    void errorStateSetsAndClearsTooltip() {
        onEdt(() -> {
            SwingTextBox box = new SwingTextBox();

            box.setError("Required");
            assertThat(box.peer().getToolTipText()).isEqualTo("Required");

            box.setError(null);
            assertThat(box.peer().getToolTipText()).isNull();
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
