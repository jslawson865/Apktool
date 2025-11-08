package brut.apktool.gui;

import brut.androlib.ApktoolProperties;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

final class ApktoolGuiFrame extends JFrame {
    private final LogPanel logPanel;
    private final GeneralOptionsPanel optionsPanel;

    ApktoolGuiFrame() {
        super("Apktool GUI");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(900, 620));

        logPanel = new LogPanel();
        optionsPanel = new GeneralOptionsPanel();

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Decode", new DecodePanel(optionsPanel, logPanel));
        tabbedPane.addTab("Build", new BuildPanel(optionsPanel, logPanel));
        tabbedPane.addTab("Frameworks", new FrameworkPanel(optionsPanel, logPanel));

        add(optionsPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        add(logPanel, BorderLayout.SOUTH);

        setJMenuBar(createMenuBar());
        installLogForwarding();
        logPanel.appendLine("Apktool GUI ready (" + ApktoolProperties.getVersion() + ")");

        pack();
        setLocationRelativeTo(null);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> dispose());
        fileMenu.add(exitItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private void showAboutDialog() {
        String version = ApktoolProperties.getVersion();
        JOptionPane.showMessageDialog(this,
            "Apktool GUI\nVersion: " + version + "\n\nA Swing front-end for the Apktool command line utility.",
            "About Apktool GUI",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void installLogForwarding() {
        PrintStream stream = new PrintStream(new TextAreaOutputStream(logPanel.getTextArea()), true,
            StandardCharsets.UTF_8);
        System.setOut(stream);
        System.setErr(stream);

        Logger rootLogger = Logger.getLogger("");
        boolean hasHandler = false;
        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof TextAreaLogHandler) {
                hasHandler = true;
                break;
            }
        }
        if (!hasHandler) {
            rootLogger.addHandler(new TextAreaLogHandler(logPanel));
        }
        if (rootLogger.getLevel().intValue() > Level.INFO.intValue()) {
            rootLogger.setLevel(Level.INFO);
        }
    }
}
