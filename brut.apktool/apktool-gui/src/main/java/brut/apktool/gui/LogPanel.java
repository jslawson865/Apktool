package brut.apktool.gui;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * Simple logging panel used to display Apktool output and status messages.
 */
final class LogPanel extends JPanel {
    private final JTextArea textArea;

    LogPanel() {
        super(new BorderLayout());
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(100, 160));

        JButton clearButton = new JButton("Clear Log");
        clearButton.addActionListener(e -> clear());

        add(scrollPane, BorderLayout.CENTER);
        add(clearButton, BorderLayout.SOUTH);
    }

    JTextArea getTextArea() {
        return textArea;
    }

    void appendLine(String text) {
        append(text + System.lineSeparator());
    }

    void append(String text) {
        SwingUtilities.invokeLater(() -> {
            textArea.append(text);
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }

    void clear() {
        SwingUtilities.invokeLater(() -> textArea.setText(""));
    }
}
