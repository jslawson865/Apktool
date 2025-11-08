package brut.apktool.gui;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Output stream that forwards all writes to a {@link JTextArea}.
 */
final class TextAreaOutputStream extends OutputStream {
    private final JTextArea textArea;
    private final StringBuilder buffer = new StringBuilder();

    TextAreaOutputStream(JTextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void write(int b) {
        write(new byte[]{(byte) b}, 0, 1);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        buffer.append(new String(b, off, len, StandardCharsets.UTF_8));
        int newlineIndex;
        while ((newlineIndex = buffer.indexOf("\n")) != -1) {
            final String line = buffer.substring(0, newlineIndex + 1);
            append(line);
            buffer.delete(0, newlineIndex + 1);
        }
    }

    @Override
    public void flush() {
        flushBuffer();
    }

    @Override
    public void close() {
        flushBuffer();
    }

    private void flushBuffer() {
        if (buffer.length() > 0) {
            final String text = buffer.toString();
            buffer.setLength(0);
            append(text);
        }
    }

    private void append(String text) {
        SwingUtilities.invokeLater(() -> {
            textArea.append(text);
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }
}
