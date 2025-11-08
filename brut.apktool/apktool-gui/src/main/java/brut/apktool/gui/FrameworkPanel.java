package brut.apktool.gui;

import brut.androlib.Config;
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.res.Framework;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

final class FrameworkPanel extends JPanel {
    private final GeneralOptionsPanel optionsPanel;
    private final LogPanel logPanel;

    private final JTextField frameworkApkField = new JTextField();
    private final JTextField publicizeFileField = new JTextField();
    private final JCheckBox forceDeleteCheck = new JCheckBox("Force delete when clearing");

    FrameworkPanel(GeneralOptionsPanel optionsPanel, LogPanel logPanel) {
        super(new GridBagLayout());
        this.optionsPanel = optionsPanel;
        this.logPanel = logPanel;
        setBorder(BorderFactory.createTitledBorder("Frameworks"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        addLabel("Framework APK", gbc, 0);
        addFileChooserField(frameworkApkField, gbc, 0, JFileChooser.FILES_ONLY);

        addLabel("Publicize Resources", gbc, 1);
        addFileChooserField(publicizeFileField, gbc, 1, JFileChooser.FILES_ONLY);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        add(forceDeleteCheck, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        JButton installButton = new JButton("Install");
        installButton.addActionListener(e -> startInstall());
        add(installButton, gbc);

        gbc.gridx = 1;
        JButton listButton = new JButton("List");
        listButton.addActionListener(e -> runInBackground("Listing installed frameworks", this::listFrameworks));
        add(listButton, gbc);

        gbc.gridx = 2;
        JButton emptyButton = new JButton("Empty");
        emptyButton.addActionListener(e -> startEmptyFrameworks());
        add(emptyButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        JButton publicizeButton = new JButton("Publicize");
        publicizeButton.addActionListener(e -> startPublicize());
        add(publicizeButton, gbc);
    }

    private void addLabel(String text, GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        add(new JLabel(text), gbc);
    }

    private void addFileChooserField(JTextField field, GridBagConstraints gbc, int row, int selectionMode) {
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1;
        gbc.gridwidth = 1;
        add(field, gbc);

        JButton browse = new JButton("Browse");
        browse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(selectionMode);
            if (!field.getText().isBlank()) {
                chooser.setSelectedFile(new File(field.getText()));
            }
            int result = chooser.showOpenDialog(FrameworkPanel.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                field.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        gbc.gridx = 2;
        gbc.gridy = row;
        gbc.weightx = 0;
        add(browse, gbc);
    }

    private void startInstall() {
        String path = frameworkApkField.getText().trim();
        if (path.isEmpty()) {
            logPanel.appendLine("Select a framework APK to install.");
            return;
        }
        File apk = new File(path);
        if (!apk.isFile()) {
            logPanel.appendLine("Framework APK does not exist: " + apk);
            return;
        }
        runInBackground("Installing framework from " + apk.getAbsolutePath(), () -> installFramework(apk));
    }

    private void startEmptyFrameworks() {
        String message = "This will remove all installed frameworks. Continue?";
        int result = JOptionPane.showConfirmDialog(this, message, "Confirm", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        runInBackground("Clearing framework directory", this::emptyFrameworkDirectory);
    }

    private void startPublicize() {
        String path = publicizeFileField.getText().trim();
        if (path.isEmpty()) {
            logPanel.appendLine("Select an ARSC/Framework file to publicize.");
            return;
        }
        File arsc = new File(path);
        if (!arsc.isFile()) {
            logPanel.appendLine("File does not exist: " + arsc);
            return;
        }
        runInBackground("Publicizing resources for " + arsc.getAbsolutePath(), () -> publicizeResources(arsc));
    }

    private void runInBackground(String message, Task task) {
        logPanel.appendLine(message);
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                task.run();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    logPanel.appendLine("Operation completed successfully.");
                } catch (Exception ex) {
                    handleError(ex);
                }
            }
        };
        worker.execute();
    }

    private void installFramework(File apk) throws AndrolibException {
        Config config = optionsPanel.createConfig();
        config.frameworkTag = optionsPanel.getFrameworkTag();
        new Framework(config).installFramework(apk);
    }

    private void listFrameworks() throws AndrolibException {
        Config config = optionsPanel.createConfig();
        new Framework(config).listFrameworkDirectory();
    }

    private void emptyFrameworkDirectory() throws AndrolibException {
        Config config = optionsPanel.createConfig();
        config.forceDeleteFramework = forceDeleteCheck.isSelected();
        new Framework(config).emptyFrameworkDirectory();
    }

    private void publicizeResources(File arsc) throws AndrolibException {
        Config config = optionsPanel.createConfig();
        new Framework(config).publicizeResources(arsc);
    }

    private void handleError(Exception ex) {
        Throwable cause = ex.getCause() == null ? ex : ex.getCause();
        if (cause instanceof NumberFormatException) {
            logPanel.appendLine("Invalid number in general options: " + cause.getMessage());
            return;
        } else if (cause instanceof AndrolibException) {
            logPanel.appendLine("Framework operation failed: " + cause.getMessage());
        } else {
            logPanel.appendLine("Unexpected framework error: " + cause.getMessage());
        }
        StringWriter writer = new StringWriter();
        cause.printStackTrace(new PrintWriter(writer));
        logPanel.append(writer.toString());
    }

    @FunctionalInterface
    private interface Task {
        void run() throws Exception;
    }
}
