package com.example.HMS.GUI.Components.Form;

import com.example.HMS.GUI.Components.DropMenu.DropMenu;
import com.example.HMS.GUI.Components.TextBox.TextBox;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Small modal form builder. Field rows are added with a label and a component
 * (usually a {@link TextBox} or {@link DropMenu}); the dialog handles layout,
 * validation messages, and OK/Cancel.
 *
 * @param <T> value produced when the user confirms
 */
public final class FormDialog<T> {
    private static final Color ERROR_COLOR = new Color(200, 40, 40);

    private final Window owner;
    private final String title;
    private final JPanel rows = new JPanel(new GridBagLayout());
    private final JLabel errorLabel = new JLabel(" ");
    private final List<JComponent> inputs = new ArrayList<>();

    private Supplier<String> validator;
    private Supplier<Optional<T>> resultSupplier;

    private FormDialog(Window owner, String title) {
        this.owner = owner;
        this.title = title;
    }

    public static <T> FormDialog<T> create(Window owner, String title) {
        return new FormDialog<>(owner, title);
    }

    public FormDialog<T> addText(String label, TextBox<? extends JComponent> input) {
        return addField(label, input.peer());
    }

    public FormDialog<T> addDrop(String label, DropMenu<? extends JComponent> input) {
        return addField(label, input.peer());
    }

    public FormDialog<T> addField(String label, JComponent input) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = inputs.size();
        left.anchor = GridBagConstraints.LINE_END;
        left.insets = new Insets(4, 0, 4, 8);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = inputs.size();
        right.weightx = 1;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.insets = new Insets(4, 0, 4, 0);

        rows.add(new JLabel(label), left);
        rows.add(input, right);
        inputs.add(input);
        return this;
    }

    /** Returns an error message, or {@code null}/blank when the form is valid. */
    public FormDialog<T> validate(Supplier<String> validator) {
        this.validator = validator;
        return this;
    }

    /** Builds the result when the user confirms; empty keeps the dialog open. */
    public FormDialog<T> result(Supplier<Optional<T>> resultSupplier) {
        this.resultSupplier = resultSupplier;
        return this;
    }

    public Optional<T> show() {
        JDialog dialog = new JDialog(owner, title, JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(rows, BorderLayout.CENTER);

        errorLabel.setForeground(ERROR_COLOR);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(errorLabel, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton cancel = new JButton("Cancel");
        JButton ok = new JButton("OK");
        buttons.add(cancel);
        buttons.add(ok);
        bottom.add(buttons, BorderLayout.EAST);
        content.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.getRootPane().setDefaultButton(ok);

        AtomicReference<Optional<T>> result = new AtomicReference<>(Optional.empty());

        ok.addActionListener(e -> {
            errorLabel.setText(" ");
            String error = validator == null ? null : validator.get();
            if (error != null && !error.isBlank()) {
                errorLabel.setText(error);
                return;
            }
            Optional<T> produced = resultSupplier == null
                    ? Optional.empty()
                    : resultSupplier.get();
            if (produced != null && produced.isPresent()) {
                result.set(produced);
                dialog.dispose();
            } else if (resultSupplier != null) {
                errorLabel.setText("Please check the form values.");
            }
        });
        cancel.addActionListener(e -> dialog.dispose());

        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
        dialog.getRootPane().getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });

        dialog.pack();
        dialog.setMinimumSize(new Dimension(320, dialog.getHeight()));
        dialog.setLocationRelativeTo(owner);
        if (!inputs.isEmpty()) {
            SwingUtilities.invokeLater(inputs.get(0)::requestFocusInWindow);
        }
        dialog.setVisible(true);
        return result.get();
    }
}
