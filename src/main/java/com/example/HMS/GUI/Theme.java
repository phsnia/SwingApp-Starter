package com.example.HMS.GUI;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Font;
import java.awt.Window;

/**
 * Central colors, fonts and spacing. Also installs a small set of UIManager
 * defaults so every component looks consistent without an external look and feel.
 */
public final class Theme {
    public static final Color BACKGROUND = new Color(246, 247, 249);
    public static final Color SURFACE = Color.WHITE;
    public static final Color BORDER = new Color(214, 218, 224);
    public static final Color TEXT = new Color(33, 37, 43);
    public static final Color MUTED_TEXT = new Color(110, 115, 125);
    public static final Color ACCENT = new Color(37, 99, 235);
    public static final Color DANGER = new Color(200, 40, 40);

    public static final int GAP = 8;
    public static final int PAD = 16;

    public static final Font FONT = new Font(fontFamily(), Font.PLAIN, 13);
    public static final Font FONT_BOLD = FONT.deriveFont(Font.BOLD);
    public static final Font TITLE_FONT = FONT.deriveFont(Font.BOLD, 18f);

    private Theme() {
    }

    public static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Keep the cross-platform look and feel.
        }
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Viewport.background", SURFACE);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Label.font", FONT);
        UIManager.put("Button.font", FONT);
        UIManager.put("Button.background", SURFACE);
        UIManager.put("Button.focus", new Color(0, 0, 0, 0));
        UIManager.put("TextField.font", FONT);
        UIManager.put("TextField.background", SURFACE);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("ComboBox.font", FONT);
        UIManager.put("ComboBox.background", SURFACE);
        UIManager.put("Table.font", FONT);
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.alternateRowColor", new Color(249, 250, 251));
        UIManager.put("TableHeader.font", FONT_BOLD);
        UIManager.put("TableHeader.background", new Color(241, 243, 246));
        UIManager.put("ToolTip.font", FONT);
        UIManager.put("OptionPane.messageFont", FONT);
        UIManager.put("OptionPane.buttonFont", FONT);
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));
    }

    public static Border padded() {
        return BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD);
    }

    public static void apply(Window window) {
        if (window != null) {
            window.setFont(FONT);
        }
    }

    private static String fontFamily() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return "Segoe UI";
        }
        if (os.contains("mac")) {
            return "Helvetica Neue";
        }
        return "SansSerif";
    }
}
