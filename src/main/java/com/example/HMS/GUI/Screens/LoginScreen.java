package com.example.HMS.GUI.Screens;

import com.example.HMS.GUI.Theme;
import com.example.HMS.GUI.UI;
import com.example.HMS.GUI.Components.TextBox.impl.SwingTextBox;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Minimal login form. Authentication is delegated through {@link #onSubmit};
 * this screen only owns the fields and validation.
 */
public final class LoginScreen extends JPanel {
    public record Credentials(String email, String password) {
    }

    private static final Pattern EMAIL =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final SwingTextBox email;
    private final SwingTextBox password;
    private final JLabel error;
    private Consumer<Credentials> submitHandler;

    public LoginScreen(UI ui) {
        setLayout(new GridBagLayout());
        setOpaque(false);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(28, 32, 28, 32)));

        JLabel title = new JLabel("Welcome back");
        title.setFont(Theme.TITLE_FONT);
        JLabel subtitle = new JLabel("Sign in to manage your manga library");
        subtitle.setForeground(Theme.MUTED_TEXT);

        email = ui.textbox().setPrompt("you@example.com");
        password = ui.passwordBox().setPrompt("Password");
        email.setSize(320, 34);
        password.setSize(320, 34);
        email.onSubmit(value -> password.focus());
        password.onSubmit(value -> submit());

        error = new JLabel(" ");
        error.setForeground(Theme.DANGER);

        JButton login = new JButton("Sign in");
        login.setBackground(Theme.ACCENT);
        login.setForeground(Color.WHITE);
        login.setFont(Theme.FONT_BOLD);
        login.addActionListener(e -> submit());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(0, 0, 4, 0);
        card.add(title, c);

        c.gridy = 1;
        c.insets = new Insets(0, 0, 20, 0);
        card.add(subtitle, c);

        addRow(card, "Email", email.peer(), 2);
        addRow(card, "Password", password.peer(), 3);

        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(8, 0, 0, 0);
        card.add(login, c);

        c.gridy = 5;
        c.insets = new Insets(8, 0, 0, 0);
        card.add(error, c);

        JLabel hint = new JLabel("Demo: any valid email and a 4+ character password",
                SwingConstants.CENTER);
        hint.setForeground(Theme.MUTED_TEXT);
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 11f));
        c.gridy = 6;
        c.insets = new Insets(12, 0, 0, 0);
        card.add(hint, c);

        add(card);
    }

    public LoginScreen onSubmit(Consumer<Credentials> handler) {
        this.submitHandler = handler;
        return this;
    }

    private void submit() {
        email.setError(null);
        password.setError(null);
        error.setText(" ");

        String mail = email.getText().trim();
        String pass = password.getText();

        if (mail.isEmpty() || !EMAIL.matcher(mail).matches()) {
            email.setError("Enter a valid email address");
            error.setText("Enter a valid email address.");
            email.focus();
            return;
        }
        if (pass.length() < 4) {
            password.setError("At least 4 characters");
            error.setText("Password must be at least 4 characters.");
            password.focus();
            return;
        }
        if (submitHandler != null) {
            submitHandler.accept(new Credentials(mail, pass));
        }
    }

    private static void addRow(JPanel card, String label, java.awt.Component input, int row) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.anchor = GridBagConstraints.LINE_START;
        left.insets = new Insets(6, 0, 6, 10);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row;
        right.weightx = 1;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.insets = new Insets(6, 0, 6, 0);

        card.add(new JLabel(label), left);
        card.add(input, right);
    }
}
