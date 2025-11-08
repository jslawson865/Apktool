package brut.apktool.gui;

import brut.androlib.ApkBuilder;
import brut.androlib.Config;
import brut.common.BrutException;
import brut.directory.ExtFile;
import brut.util.AaptManager;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

final class BuildPanel extends JPanel {
    private final GeneralOptionsPanel optionsPanel;
    private final LogPanel logPanel;

    private final JTextField projectDirField = new JTextField();
    private final JTextField outputApkField = new JTextField();
    private final JCheckBox forceAllCheck = new JCheckBox("Force all");
    private final JCheckBox debugCheck = new JCheckBox("Debug mode");
    private final JCheckBox netSecCheck = new JCheckBox("Add Network Security Config");
    private final JCheckBox verboseCheck = new JCheckBox("Verbose logging");
    private final JCheckBox copyOriginalCheck = new JCheckBox("Copy original files");
    private final JCheckBox noCrunchCheck = new JCheckBox("Do not crunch resources");
    private final JCheckBox disableSanitizerCheck = new JCheckBox("Disable manifest sanitizer");
    private final JRadioButton autoAapt = new JRadioButton("Auto (default)", true);
    private final JRadioButton aapt1 = new JRadioButton("Force AAPT1");
    private final JRadioButton aapt2 = new JRadioButton("Force AAPT2");
    private final JButton buildButton = new JButton("Build");

    BuildPanel(GeneralOptionsPanel optionsPanel, LogPanel logPanel) {
        super(new GridBagLayout());
        this.optionsPanel = optionsPanel;
        this.logPanel = logPanel;
        setBorder(BorderFactory.createTitledBorder("Build"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        addLabel("Project Dir", gbc, 0);
        addFileChooserField(projectDirField, gbc, 0, JFileChooser.DIRECTORIES_ONLY);

        addLabel("Output APK", gbc, 1);
        addFileChooserField(outputApkField, gbc, 1, JFileChooser.FILES_ONLY);

        int row = 2;
        row = addOption(forceAllCheck, gbc, row);
        row = addOption(debugCheck, gbc, row);
        row = addOption(netSecCheck, gbc, row);
        row = addOption(verboseCheck, gbc, row);
        row = addOption(copyOriginalCheck, gbc, row);
        row = addOption(noCrunchCheck, gbc, row);
        row = addOption(disableSanitizerCheck, gbc, row);

        ButtonGroup aaptGroup = new ButtonGroup();
        aaptGroup.add(autoAapt);
        aaptGroup.add(aapt1);
        aaptGroup.add(aapt2);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        add(new JLabel("AAPT"), gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        add(autoAapt, gbc);
        row++;

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        add(aapt1, gbc);
        row++;

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        add(aapt2, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        buildButton.addActionListener(e -> startBuild());
        add(buildButton, gbc);
    }

    private int addOption(JCheckBox checkBox, GridBagConstraints gbc, int row) {
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        add(checkBox, gbc);
        return row + 1;
    }

    private void addLabel(String text, GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
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
            int result = chooser.showOpenDialog(BuildPanel.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                field.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        gbc.gridx = 2;
        gbc.gridy = row;
        gbc.weightx = 0;
        add(browse, gbc);
    }

    private void startBuild() {
        String projectPath = projectDirField.getText().trim();
        if (projectPath.isEmpty()) {
            logPanel.appendLine("Please select a project directory to build.");
            return;
        }
        File projectDir = new File(projectPath);
        if (!projectDir.isDirectory()) {
            logPanel.appendLine("The selected project directory does not exist: " + projectDir);
            return;
        }

        File outputFile = null;
        if (!outputApkField.getText().trim().isEmpty()) {
            outputFile = new File(outputApkField.getText().trim());
        }

        if (netSecCheck.isSelected() && aapt1.isSelected()) {
            logPanel.appendLine("Network Security Config requires AAPT2. Choose Auto or Force AAPT2.");
            return;
        }

        Config baseConfig;
        try {
            baseConfig = optionsPanel.createConfig();
        } catch (NumberFormatException ex) {
            logPanel.appendLine("Invalid number in general options: " + ex.getMessage());
            return;
        }

        buildButton.setEnabled(false);
        logPanel.appendLine("Starting build for " + projectDir.getAbsolutePath());

        File finalOutputFile = outputFile;
        Config configForWorker = baseConfig;
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                Config config = configForWorker;
                configureBuildOptions(config);

                if (finalOutputFile != null) {
                    publish("Output APK: " + finalOutputFile.getAbsolutePath());
                }

                if (config.aaptPath != null && !config.aaptPath.isEmpty()) {
                    config.aaptVersion = AaptManager.getAaptVersion(config.aaptPath);
                }

                new ApkBuilder(config, new ExtFile(projectDir)).build(finalOutputFile);
                publish("Build completed successfully.");
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String chunk : chunks) {
                    logPanel.appendLine(chunk);
                }
            }

            @Override
            protected void done() {
                buildButton.setEnabled(true);
                try {
                    get();
                } catch (Exception ex) {
                    handleBuildError(ex);
                }
            }
        };
        worker.execute();
    }

    private void configureBuildOptions(Config config) {
        config.forceBuildAll = forceAllCheck.isSelected();
        config.debugMode = debugCheck.isSelected();
        config.netSecConf = netSecCheck.isSelected();
        config.verbose = verboseCheck.isSelected();
        config.copyOriginalFiles = copyOriginalCheck.isSelected();
        config.noCrunch = noCrunchCheck.isSelected();
        config.setSanitizeManifest(!disableSanitizerCheck.isSelected());

        if (aapt1.isSelected()) {
            config.useAapt2 = false;
        } else if (aapt2.isSelected()) {
            config.useAapt2 = true;
        }
    }

    private void handleBuildError(Exception ex) {
        Throwable cause = ex.getCause() == null ? ex : ex.getCause();
        if (cause instanceof BrutException) {
            logPanel.appendLine("Build failed: " + cause.getMessage());
        } else {
            logPanel.appendLine("Unexpected build error: " + cause.getMessage());
        }
        StringWriter writer = new StringWriter();
        cause.printStackTrace(new PrintWriter(writer));
        logPanel.append(writer.toString());
    }
}
