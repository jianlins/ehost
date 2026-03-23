package userInterface;

import config.system.SysConf;
import rest.server.PropertiesUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Dialog for managing system configuration from eHOST.sys and application.properties.
 */
public class SystemConfigDialog extends JDialog {

    private final GUI parentGui;

    // Tab 1: Feature Visibility (MASK)
    private JCheckBox chkResultEditor;
    private JCheckBox chkNlpAssisted;
    private JCheckBox chkPinExtractor;
    private JCheckBox chkDictionaryManager;
    private JCheckBox chkFileConverter;
    private JCheckBox chkSystemSettings;
    private JCheckBox chkRestfulServer;

    // Tab 2: REST Server
    private JTextField txtServerAddress;
    private JSpinner spnServerPort;
    private JTextField txtMgmtAddress;
    private JSpinner spnMgmtPort;
    private JComboBox<String> cmbLogRoot;
    private JComboBox<String> cmbLogSpringWeb;
    private JComboBox<String> cmbLogHibernate;

    private static final String[] LOG_LEVELS = {"ERROR", "WARN", "INFO", "DEBUG"};

    // Snapshot of original REST values to detect changes
    private String origServerAddress;
    private String origServerPort;
    private String origMgmtAddress;
    private String origMgmtPort;

    public SystemConfigDialog(GUI parent) {
        super(parent, "System Configuration", true);
        this.parentGui = parent;

        initComponents();
        loadCurrentValues();
        snapshotRestValues();

        setSize(520, 480);
        setResizable(false);
        setLocationRelativeTo(parent);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });
    }

    private void initComponents() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Feature Visibility", buildFeatureTab());
        tabbedPane.addTab("REST Server", buildRestTab());

        JPanel buttonPanel = buildButtonPanel();

        getContentPane().setLayout(new BorderLayout(8, 8));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(8, 8, 8, 8));
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel buildFeatureTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Mask checkboxes
        JPanel maskPanel = new JPanel(new GridLayout(0, 1, 4, 4));
        maskPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Toolbar Feature Visibility",
                TitledBorder.LEFT, TitledBorder.TOP));

        chkResultEditor = new JCheckBox("Result Editor");
        chkNlpAssisted = new JCheckBox("NLP Assisted Annotation");
        chkPinExtractor = new JCheckBox("PIN Extractor");
        chkDictionaryManager = new JCheckBox("Dictionary Manager");
        chkFileConverter = new JCheckBox("File Converter (currently unused)");
        chkSystemSettings = new JCheckBox("System Settings");

        maskPanel.add(chkResultEditor);
        maskPanel.add(chkNlpAssisted);
        maskPanel.add(chkPinExtractor);
        maskPanel.add(chkDictionaryManager);
        maskPanel.add(chkFileConverter);
        maskPanel.add(chkSystemSettings);

        // REST server toggle
        JPanel serverPanel = new JPanel(new GridLayout(0, 1, 4, 4));
        serverPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Server",
                TitledBorder.LEFT, TitledBorder.TOP));

        chkRestfulServer = new JCheckBox("Enable RESTful Server");
        serverPanel.add(chkRestfulServer);

        panel.add(maskPanel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(serverPanel);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel buildRestTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Server Address
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("Server Address:"), gbc);
        txtServerAddress = new JTextField(20);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(txtServerAddress, gbc);

        // Server Port
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("Server Port:"), gbc);
        spnServerPort = new JSpinner(new SpinnerNumberModel(8010, 1, 65535, 1));
        spnServerPort.setEditor(new JSpinner.NumberEditor(spnServerPort, "#"));
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(spnServerPort, gbc);

        // Management Server Address
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("Management Server Address:"), gbc);
        txtMgmtAddress = new JTextField(20);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(txtMgmtAddress, gbc);

        // Management Server Port
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("Management Server Port:"), gbc);
        spnMgmtPort = new JSpinner(new SpinnerNumberModel(8010, 1, 65535, 1));
        spnMgmtPort.setEditor(new JSpinner.NumberEditor(spnMgmtPort, "#"));
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(spnMgmtPort, gbc);

        // Separator
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        panel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        // Logging: Root
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("Logging Level (Root):"), gbc);
        cmbLogRoot = new JComboBox<>(LOG_LEVELS);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(cmbLogRoot, gbc);

        // Logging: Spring Web
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("Logging Level (Spring Web):"), gbc);
        cmbLogSpringWeb = new JComboBox<>(LOG_LEVELS);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(cmbLogSpringWeb, gbc);

        // Logging: Hibernate
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JLabel("Logging Level (Hibernate):"), gbc);
        cmbLogHibernate = new JComboBox<>(LOG_LEVELS);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(cmbLogHibernate, gbc);

        // Restart note
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.insets = new Insets(12, 4, 4, 4);
        JLabel noteLabel = new JLabel(
                "<html><i>Note: REST server changes require application restart to take effect.</i></html>");
        noteLabel.setForeground(Color.GRAY);
        panel.add(noteLabel, gbc);

        // Push everything to top
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));

        JButton btnDefaults = new JButton("Restore Defaults");
        btnDefaults.addActionListener(this::onRestoreDefaults);

        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> onCancel());

        JButton btnSave = new JButton("Save");
        btnSave.addActionListener(e -> onSave());

        panel.add(btnDefaults);
        panel.add(btnCancel);
        panel.add(btnSave);

        return panel;
    }

    // --- Load / Save ---

    private void loadCurrentValues() {
        // Feature Visibility (MASK)
        char[] mask = env.Parameters.Sysini.functions;
        if (mask == null || mask.length != 6) {
            mask = new char[]{'0', '1', '0', '0', '0', '1'};
        }
        chkResultEditor.setSelected(mask[0] == '1');
        chkNlpAssisted.setSelected(mask[1] == '1');
        chkPinExtractor.setSelected(mask[2] == '1');
        chkDictionaryManager.setSelected(mask[3] == '1');
        chkFileConverter.setSelected(mask[4] == '1');
        chkSystemSettings.setSelected(mask[5] == '1');

        // RESTful Server
        chkRestfulServer.setSelected(env.Parameters.RESTFulServer);

        // REST Server settings from application.properties
        txtServerAddress.setText(
                PropertiesUtil.getProperty("server.address", "127.0.0.1"));
        spnServerPort.setValue(Integer.parseInt(
                PropertiesUtil.getProperty("server.port", "8010")));
        txtMgmtAddress.setText(
                PropertiesUtil.getProperty("management.server.address", "127.0.0.1"));
        spnMgmtPort.setValue(Integer.parseInt(
                PropertiesUtil.getProperty("management.server.port", "8010")));

        selectComboItem(cmbLogRoot,
                PropertiesUtil.getProperty("logging.level.root", "WARN"));
        selectComboItem(cmbLogSpringWeb,
                PropertiesUtil.getProperty("logging.level.org.springframework.web", "INFO"));
        selectComboItem(cmbLogHibernate,
                PropertiesUtil.getProperty("logging.level.org.hibernate", "ERROR"));
    }

    private void snapshotRestValues() {
        origServerAddress = txtServerAddress.getText();
        origServerPort = String.valueOf(spnServerPort.getValue());
        origMgmtAddress = txtMgmtAddress.getText();
        origMgmtPort = String.valueOf(spnMgmtPort.getValue());
    }

    private void onSave() {
        // 1. Update MASK in env.Parameters
        char[] mask = new char[6];
        mask[0] = chkResultEditor.isSelected() ? '1' : '0';
        mask[1] = chkNlpAssisted.isSelected() ? '1' : '0';
        mask[2] = chkPinExtractor.isSelected() ? '1' : '0';
        mask[3] = chkDictionaryManager.isSelected() ? '1' : '0';
        mask[4] = chkFileConverter.isSelected() ? '1' : '0';
        mask[5] = chkSystemSettings.isSelected() ? '1' : '0';
        env.Parameters.Sysini.functions = mask;

        // 2. Update RESTful Server flag
        env.Parameters.RESTFulServer = chkRestfulServer.isSelected();

        // 3. Save eHOST.sys
        SysConf.saveSystemConfigure();

        // 4. Save application.properties via PropertiesUtil
        String newServerAddress = txtServerAddress.getText().trim();
        String newServerPort = String.valueOf(spnServerPort.getValue());
        String newMgmtAddress = txtMgmtAddress.getText().trim();
        String newMgmtPort = String.valueOf(spnMgmtPort.getValue());

        PropertiesUtil.updateProperty("server.address", newServerAddress);
        PropertiesUtil.updateProperty("server.port", newServerPort);
        PropertiesUtil.updateProperty("management.server.address", newMgmtAddress);
        PropertiesUtil.updateProperty("management.server.port", newMgmtPort);
        PropertiesUtil.updateProperty("logging.level.root",
                (String) cmbLogRoot.getSelectedItem());
        PropertiesUtil.updateProperty("logging.level.org.springframework.web",
                (String) cmbLogSpringWeb.getSelectedItem());
        PropertiesUtil.updateProperty("logging.level.org.hibernate",
                (String) cmbLogHibernate.getSelectedItem());

        // 5. Refresh toolbar visibility
        parentGui.enableFunctionsByMask();

        // 6. Reload cached eHOST config
        PropertiesUtil.reloadEhostConfig();

        // 7. Warn if REST server settings changed
        boolean restChanged = !newServerAddress.equals(origServerAddress)
                || !newServerPort.equals(origServerPort)
                || !newMgmtAddress.equals(origMgmtAddress)
                || !newMgmtPort.equals(origMgmtPort);

        dispose();

        if (restChanged) {
            JOptionPane.showMessageDialog(parentGui,
                    "REST server settings have been changed.\n"
                            + "Please restart the application for changes to take effect.",
                    "Restart Required",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void onCancel() {
        dispose();
    }

    private void onRestoreDefaults(ActionEvent e) {
        // MASK defaults
        chkResultEditor.setSelected(false);
        chkNlpAssisted.setSelected(true);
        chkPinExtractor.setSelected(false);
        chkDictionaryManager.setSelected(false);
        chkFileConverter.setSelected(false);
        chkSystemSettings.setSelected(true);

        // REST server default
        chkRestfulServer.setSelected(true);

        // REST settings defaults
        txtServerAddress.setText("127.0.0.1");
        spnServerPort.setValue(8010);
        txtMgmtAddress.setText("127.0.0.1");
        spnMgmtPort.setValue(8010);

        selectComboItem(cmbLogRoot, "WARN");
        selectComboItem(cmbLogSpringWeb, "INFO");
        selectComboItem(cmbLogHibernate, "ERROR");
    }

    private static void selectComboItem(JComboBox<String> combo, String value) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).equalsIgnoreCase(value)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        combo.setSelectedIndex(0);
    }
}
