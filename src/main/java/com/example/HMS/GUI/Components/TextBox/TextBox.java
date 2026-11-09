package com.example.HMS.GUI.Components.TextBox;

import java.util.function.Consumer;

/**
 * Text input used by forms.
 *
 * <p>The prompt (placeholder) is purely visual: it never becomes the field
 * value, so {@link #getText()} only ever returns what the user typed.</p>
 *
 * @param <T> Swing peer type exposed by {@link #peer()}
 */
public interface TextBox<T> {

    TextBox<T> setPosition(double x, double y);

    TextBox<T> setSize(double w, double h);

    String getText();

    TextBox<T> setText(String text);

    TextBox<T> clear();

    TextBox<T> setPrompt(String text);

    TextBox<T> setEditable(boolean editable);

    TextBox<T> setEnabled(boolean enabled);

    /** Shows an error border and tooltip; pass {@code null} to clear. */
    TextBox<T> setError(String message);

    TextBox<T> setMaxLength(int maxLength);

    TextBox<T> password(boolean password);

    TextBox<T> onChange(Consumer<String> fn);

    /** Called when the user presses Enter. */
    TextBox<T> onSubmit(Consumer<String> fn);

    void focus();

    T peer();
}
