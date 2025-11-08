package brut.apktool.gui;

import brut.androlib.ApkDecoder;
import brut.androlib.Config;
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.exceptions.CantFindFrameworkResException;
import brut.androlib.exceptions.InFileNotFoundException;
import brut.androlib.exceptions.OutDirExistsException;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

final class DecodePanel extends JPanel {
    private final GeneralOptionsPanel optionsPanel;
    private final LogPanel logPanel;

    private final JTextField apkField = new JTextField();
    private final JTextField outputField = new JTextField();
    private final JCheckBox decodeSourcesCheck = new JCheckBox("Decode sources", true);
    private final JCheckBox onlyMainClassesCheck = new JCheckBox("Only main classes");
    private final JCheckBox decodeResourcesCheck = new JCheckBox("Decode resources", true);
    private final JCheckBox decodeAssetsCheck = new JCheckBox("Decode assets", true);
    private final JCheckBox forceDeleteCheck = new JCheckBox("Force delete destination");
    private final JCheckBox keepBrokenCheck = new JCheckBox("Keep broken resources");
    private final JCheckBox matchOriginalCheck = new JCheckBox("Match original");
    private final JCheckBox forceManifestCheck = new JCheckBox("Force manifest decoding");
    private final JCheckBox skipDebugInfoCheck = new JCheckBox("Skip debug info");
    private final JComboBox<ResolveMode> resolveModeCombo = new JComboBox<>(ResolveMode.values());
    private final JButton decodeButton = new JButton("Decode");

    DecodePanel(GeneralOptionsPanel optionsPanel, LogPanel logPanel) {
        super(new GridBagLayout());
        this.optionsPanel = optionsPanel;
        this.logPanel = logPanel;
        setBorder(BorderFactory.createTitledBorder("Decode"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        addLabel("APK File", gbc, 0);
        addFileChooserField(apkField, gbc, 0, JFileChooser.FILES_ONLY);

        addLabel("Output Dir", gbc, 1);
        addFileChooserField(outputField, gbc, 1, JFileChooser.DIRECTORIES_ONLY);

        addLabel("Resolve Mode", gbc, 2);
        resolveModeCombo.setSelectedItem(ResolveMode.REMOVE);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        add(resolveModeCombo, gbc);

        int row = 3;
        row = addOption(decodeSourcesCheck, gbc, row);
        row = addOption(onlyMainClassesCheck, gbc, row);
        row = addOption(decodeResourcesCheck, gbc, row);
        row = addOption(decodeAssetsCheck, gbc, row);
        row = addOption(forceDeleteCheck, gbc, row);
        row = addOption(keepBrokenCheck, gbc, row);
        row = addOption(matchOriginalCheck, gbc, row);
        row = addOption(forceManifestCheck, gbc, row);
        row = addOption(skipDebugInfoCheck, gbc, row);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        decodeButton.addActionListener(e -> startDecode());
        add(decodeButton, gbc);

        decodeSourcesCheck.addActionListener(e -> {
            boolean enabled = decodeSourcesCheck.isSelected();
            onlyMainClassesCheck.setEnabled(enabled);
            skipDebugInfoCheck.setEnabled(enabled);
        });
        onlyMainClassesCheck.setEnabled(decodeSourcesCheck.isSelected());
        skipDebugInfoCheck.setEnabled(decodeSourcesCheck.isSelected());
    }

    private int addOption(JCheckBox checkBox, GridBagConstraints gbc, int row) {
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
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
            int result = chooser.showOpenDialog(DecodePanel.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                field.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        gbc.gridx = 2;
        gbc.gridy = row;
        gbc.weightx = 0;
        add(browse, gbc);
    }

    private void startDecode() {
        String apkPath = apkField.getText().trim();
        if (apkPath.isEmpty()) {
            logPanel.appendLine("Please select an APK to decode.");
            return;
        }
        File apkFile = new File(apkPath);
        if (!apkFile.isFile()) {
            logPanel.appendLine("The selected APK file does not exist: " + apkFile);
            return;
        }

        File outputDir;
        String output = outputField.getText().trim();
        if (output.isEmpty()) {
            outputDir = computeDefaultOutput(apkFile);
        } else {
            outputDir = new File(output);
        }

        Config baseConfig;
        try {
            baseConfig = optionsPanel.createConfig();
        } catch (NumberFormatException ex) {
            logPanel.appendLine("Invalid number in general options: " + ex.getMessage());
            return;
        }

        decodeButton.setEnabled(false);
        logPanel.appendLine("Starting decode of " + apkFile.getAbsolutePath());

        Config configForWorker = baseConfig;
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                Config config = configForWorker;
                configureDecodeOptions(config);

                publish("Decoding resources to " + outputDir.getAbsolutePath());
                ApkDecoder decoder = new ApkDecoder(config, new ExtFile(apkFile));
                decoder.decode(outputDir);
                publish("Decode completed successfully.");
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
                decodeButton.setEnabled(true);
                try {
                    get();
                } catch (Exception ex) {
                    handleDecodeError(ex);
                }
            }
        };
        worker.execute();
    }

    private void configureDecodeOptions(Config config) throws AndrolibException {
        if (!decodeSourcesCheck.isSelected()) {
            config.setDecodeSources(Config.DECODE_SOURCES_NONE);
        } else if (onlyMainClassesCheck.isSelected()) {
            config.setDecodeSources(Config.DECODE_SOURCES_SMALI_ONLY_MAIN_CLASSES);
        }

        if (!decodeResourcesCheck.isSelected()) {
            config.setDecodeResources(Config.DECODE_RESOURCES_NONE);
        }
        if (!decodeAssetsCheck.isSelected()) {
            config.setDecodeAssets(Config.DECODE_ASSETS_NONE);
        }
        if (forceManifestCheck.isSelected()) {
            config.setForceDecodeManifest(Config.FORCE_DECODE_MANIFEST_FULL);
        }
        if (skipDebugInfoCheck.isSelected()) {
            config.baksmaliDebugMode = false;
        }
        config.forceDelete = forceDeleteCheck.isSelected();
        config.keepBrokenResources = keepBrokenCheck.isSelected();
        config.analysisMode = matchOriginalCheck.isSelected();

        ResolveMode mode = (ResolveMode) resolveModeCombo.getSelectedItem();
        if (mode != null) {
            switch (mode) {
                case REMOVE:
                    config.setDecodeResolveMode(Config.DECODE_RES_RESOLVE_REMOVE);
                    break;
                case DUMMY:
                    config.setDecodeResolveMode(Config.DECODE_RES_RESOLVE_DUMMY);
                    break;
                case KEEP:
                    config.setDecodeResolveMode(Config.DECODE_RES_RESOLVE_RETAIN);
                    break;
                default:
                    break;
            }
        }
    }

    private void handleDecodeError(Exception ex) {
        Throwable cause = ex.getCause() == null ? ex : ex.getCause();
        if (cause instanceof OutDirExistsException) {
            logPanel.appendLine("Destination directory already exists. Enable 'Force delete destination' to overwrite.");
        } else if (cause instanceof InFileNotFoundException) {
            logPanel.appendLine("Input file was not found or was not readable.");
        } else if (cause instanceof CantFindFrameworkResException) {
            logPanel.appendLine("Could not locate required framework resources: " + cause.getMessage());
        } else if (cause instanceof DirectoryException) {
            logPanel.appendLine("Could not modify internal dex files. Check permissions.");
        } else if (cause instanceof IOException) {
            logPanel.appendLine("Unable to write decoded files: " + cause.getMessage());
        } else if (cause instanceof AndrolibException) {
            logPanel.appendLine("Apktool error: " + cause.getMessage());
        } else {
            logPanel.appendLine("Unexpected error: " + cause.getMessage());
        }
        StringWriter writer = new StringWriter();
        cause.printStackTrace(new PrintWriter(writer));
        logPanel.append(writer.toString());
    }

    private File computeDefaultOutput(File apkFile) {
        String name = apkFile.getName();
        String base = name.endsWith(".apk") ? name.substring(0, name.length() - 4) : name + ".out";
        if (!name.endsWith(".apk")) {
            return new File(apkFile.getParentFile() == null ? new File(".") : apkFile.getParentFile(), base);
        }
        File parent = apkFile.getParentFile();
        if (parent == null) {
            parent = new File(".");
        }
        return new File(parent, base);
    }

    private enum ResolveMode {
        REMOVE("Remove"),
        DUMMY("Dummy"),
        KEEP("Keep");

        private final String label;

        ResolveMode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
