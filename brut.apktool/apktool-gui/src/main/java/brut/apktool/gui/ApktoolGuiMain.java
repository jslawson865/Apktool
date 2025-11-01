package brut.apktool.gui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point for the Swing based Apktool GUI.
 */
public final class ApktoolGuiMain {
    private ApktoolGuiMain() {
    }

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // ignore if system look and feel is not available
        }
        SwingUtilities.invokeLater(() -> {
            ApktoolGuiFrame frame = new ApktoolGuiFrame();
            frame.setVisible(true);
        });
    }
}
