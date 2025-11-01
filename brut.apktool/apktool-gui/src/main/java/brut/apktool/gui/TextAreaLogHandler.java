package brut.apktool.gui;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * java.util.logging handler that routes log entries to the GUI log panel.
 */
final class TextAreaLogHandler extends Handler {
    private final LogPanel logPanel;

    TextAreaLogHandler(LogPanel logPanel) {
        this.logPanel = logPanel;
        setLevel(Level.ALL);
    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append('[')
            .append(record.getLevel().getName())
            .append("] ")
            .append(record.getMessage())
            .append(System.lineSeparator());
        Throwable thrown = record.getThrown();
        if (thrown != null) {
            StringWriter writer = new StringWriter();
            thrown.printStackTrace(new PrintWriter(writer));
            builder.append(writer);
        }
        logPanel.append(builder.toString());
    }

    @Override
    public void flush() {
        // nothing to flush
    }

    @Override
    public void close() throws SecurityException {
        // nothing to close
    }
}
