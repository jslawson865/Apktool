package brut.apktool.gui;

import brut.androlib.Config;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

/**
 * Panel containing general Apktool configuration shared by multiple actions.
 */
final class GeneralOptionsPanel extends JPanel {
    private final JTextField frameworkDirField = new JTextField();
    private final JTextField frameworkSearchField = new JTextField();
    private final JTextField apiLevelField = new JTextField();
    private final JTextField jobsField = new JTextField();
    private final JTextField frameworkTagField = new JTextField();
    private final JTextField aaptPathField = new JTextField();

    GeneralOptionsPanel() {
        super(new GridBagLayout());
        setBorder(javax.swing.BorderFactory.createTitledBorder("General Options"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;

        addLabel("Framework Dir", gbc, 0);
        addTextFieldWithBrowse(frameworkDirField, gbc, 0, JFileChooser.DIRECTORIES_ONLY);

        addLabel("Framework Search", gbc, 1);
        addTextFieldWithBrowse(frameworkSearchField, gbc, 1, JFileChooser.DIRECTORIES_ONLY);

        addLabel("Framework Tag", gbc, 2);
        addField(frameworkTagField, gbc, 2);

        addLabel("API Level", gbc, 3);
        apiLevelField.setToolTipText("Optional numeric API level to target");
        addField(apiLevelField, gbc, 3);

        addLabel("Jobs", gbc, 4);
        jobsField.setToolTipText("Number of parallel jobs (leave empty for default)");
        addField(jobsField, gbc, 4);

        addLabel("AAPT Path", gbc, 5);
        addTextFieldWithBrowse(aaptPathField, gbc, 5, JFileChooser.FILES_ONLY);
    }

    private void addLabel(String text, GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        add(new JLabel(text), gbc);
    }

    private void addField(JTextField field, GridBagConstraints gbc, int row) {
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1;
        add(field, gbc);
    }

    private void addTextFieldWithBrowse(JTextField field, GridBagConstraints gbc, int row, int selectionMode) {
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1;
        add(field, gbc);

        JButton browse = new JButton("Browse");
        browse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(selectionMode);
            if (!field.getText().isBlank()) {
                chooser.setCurrentDirectory(new File(field.getText()));
            }
            int result = chooser.showOpenDialog(GeneralOptionsPanel.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                field.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        gbc.gridx = 2;
        gbc.gridy = row;
        gbc.weightx = 0;
        add(browse, gbc);
    }

    Config createConfig() {
        Config config = Config.getDefaultConfig();

        if (!frameworkDirField.getText().isBlank()) {
            config.frameworkDirectory = frameworkDirField.getText().trim();
        }
        if (!frameworkSearchField.getText().isBlank()) {
            config.frameworkSearchPath = frameworkSearchField.getText().trim();
        }
        if (!frameworkTagField.getText().isBlank()) {
            config.frameworkTag = frameworkTagField.getText().trim();
        }
        if (!apiLevelField.getText().isBlank()) {
            config.apiLevel = Integer.parseInt(apiLevelField.getText().trim());
        }
        if (!jobsField.getText().isBlank()) {
            config.jobs = Integer.parseInt(jobsField.getText().trim());
        }
        if (!aaptPathField.getText().isBlank()) {
            config.aaptPath = aaptPathField.getText().trim();
        }
        return config;
    }

    String getFrameworkTag() {
        return frameworkTagField.getText().isBlank() ? null : frameworkTagField.getText().trim();
    }

    String getAaptPath() {
        return aaptPathField.getText().isBlank() ? null : aaptPathField.getText().trim();
    }
}
