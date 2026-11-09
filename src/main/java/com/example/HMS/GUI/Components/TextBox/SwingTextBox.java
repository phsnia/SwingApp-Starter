package com.example.HMS.GUI.Components.TextBox;

import javax.swing.BorderFactory;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.function.Consumer;

/**
 * Default {@link TextBox} implementation.
 *
 * <p>The prompt is painted by the field and never written to the document, so
 * {@link #getText()} only returns user input. A {@link JPasswordField} is used
 * internally so {@link #password(boolean)} can toggle masking without replacing
 * the component.</p>
 */
public class SwingTextBox implements TextBox<JTextField> {
    private static final Color PROMPT_COLOR = new Color(128, 128, 128);
    private static final Color ERROR_COLOR = new Color(200, 40, 40);

    private final HintField field;
    private final Border normalBorder;
    private String prompt = "";

    public SwingTextBox() {
        this("");
    }

    public SwingTextBox(String text) {
        field = new HintField(text);
        // A JTextField with 0 columns has almost no preferred width, so it
        // collapses in layouts that respect preferred size. Give it a sensible
        // default; callers can still override it with setSize(...).
        field.setColumns(24);
        // Echo char 0 means "show text normally", so one field type can act as
        // both a plain text box and a password box.
        field.setEchoChar((char) 0);
        normalBorder = field.getBorder();
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.repaint();
            }
        });
    }

    @Override
    public SwingTextBox setPosition(double x, double y) {
        field.setLocation((int) x, (int) y);
        return this;
    }

    @Override
    public SwingTextBox setSize(double w, double h) {
        Dimension size = new Dimension((int) w, (int) h);
        field.setPreferredSize(size);
        field.setMinimumSize(new Dimension(0, (int) h));
        return this;
    }

    @Override
    public String getText() {
        return new String(field.getPassword());
    }

    @Override
    public SwingTextBox setText(String text) {
        field.setText(text == null ? "" : text);
        return this;
    }

    @Override
    public SwingTextBox clear() {
        return setText("");
    }

    @Override
    public SwingTextBox setPrompt(String text) {
        prompt = text == null ? "" : text;
        field.setPrompt(prompt);
        field.repaint();
        return this;
    }

    @Override
    public SwingTextBox setEditable(boolean editable) {
        field.setEditable(editable);
        return this;
    }

    @Override
    public SwingTextBox setEnabled(boolean enabled) {
        field.setEnabled(enabled);
        return this;
    }

    @Override
    public SwingTextBox setError(String message) {
        if (message == null || message.isBlank()) {
            field.setBorder(normalBorder);
            field.setToolTipText(null);
        } else {
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ERROR_COLOR, 1),
                    BorderFactory.createEmptyBorder(1, 3, 1, 3)));
            field.setToolTipText(message);
        }
        field.repaint();
        return this;
    }

    @Override
    public SwingTextBox setMaxLength(int maxLength) {
        if (maxLength < 0) {
            throw new IllegalArgumentException("maxLength cannot be negative");
        }
        if (field.getDocument() instanceof AbstractDocument document) {
            document.setDocumentFilter(maxLength == 0 ? null : new MaxLengthFilter(maxLength));
        }
        return this;
    }

    @Override
    public SwingTextBox password(boolean password) {
        field.setEchoChar(password ? '\u2022' : (char) 0);
        return this;
    }

    @Override
    public SwingTextBox onChange(Consumer<String> fn) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            private void fire() {
                fn.accept(getText());
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                fire();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                fire();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                fire();
            }
        });
        return this;
    }

    @Override
    public SwingTextBox onSubmit(Consumer<String> fn) {
        field.addActionListener(e -> fn.accept(getText()));
        return this;
    }

    @Override
    public void focus() {
        field.requestFocusInWindow();
    }

    @Override
    public JTextField peer() {
        return field;
    }

    /** Painting the prompt keeps it out of the document entirely. */
    private static final class HintField extends JPasswordField {
        private String prompt = "";

        private HintField(String text) {
            super(text);
        }

        private void setPrompt(String prompt) {
            this.prompt = prompt;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (prompt.isEmpty() || getPassword().length > 0 || isFocusOwner()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(PROMPT_COLOR);
                g2.setFont(getFont());
                Insets insets = getInsets();
                FontMetrics metrics = g2.getFontMetrics();
                int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
                g2.drawString(prompt, insets.left + 2, y);
            } finally {
                g2.dispose();
            }
        }
    }

    private static final class MaxLengthFilter extends DocumentFilter {
        private final int maxLength;

        private MaxLengthFilter(int maxLength) {
            this.maxLength = maxLength;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attributes)
                throws BadLocationException {
            replace(fb, offset, 0, text, attributes);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attributes)
                throws BadLocationException {
            if (text == null) {
                return;
            }
            int currentLength = fb.getDocument().getLength();
            int newLength = currentLength - length + text.length();
            if (newLength <= maxLength) {
                super.replace(fb, offset, length, text, attributes);
                return;
            }
            int allowed = maxLength - (currentLength - length);
            if (allowed > 0) {
                super.replace(fb, offset, length, text.substring(0, allowed), attributes);
            }
        }
    }
}
