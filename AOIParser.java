package parser;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.prefs.Preferences;
import javax.swing.*;

public class AOIParser extends JFrame {
    private JTextField txtSerialNumber, txtWorkorder, txtProgram, txtStatus, txtSide;
    private JTextArea logTextArea;
    private JLabel lblMachineName;
    private String logFilePath;
    private boolean logEnable;
    private String inputFolderPath;
    private String outFolderPath;
    private String backupFolderPath;
    private String sqlConStr;
    private String operationId;
    private String operationDuration;
    private volatile boolean stopProcessing = false;
    private String topOperationId;
    private String bottomOperationId;

    private Preferences prefs;
    private static final String PREFS_NODE = "AOIParserPrefs";
    private static final String CONFIG_PATH_KEY = "ConfigFilePath";

    public AOIParser() {
        prefs = Preferences.userRoot().node(PREFS_NODE);

        // Initialize the UI components, including lblMachineName
        initComponents();

        // Load configuration after UI components have been initialized
        loadConfig();

        // Set up window listener to stop processing when the window is closed
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopProcessing = true;
            }
        });
    }

    private void initComponents() {
        setTitle("AOI Parser");
        setBounds(100, 100, 850, 650);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBackground(new Color(240, 248, 255)); // Light blue background
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        Font labelFont = new Font("Arial", Font.BOLD, 14);

        // SerialNumber label and text field
        JLabel lblSerialNumber = new JLabel("SerialNumber:");
        lblSerialNumber.setFont(labelFont);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(lblSerialNumber, gbc);

        txtSerialNumber = new JTextField(25);
        txtSerialNumber.setEditable(false);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(txtSerialNumber, gbc);

        // Workorder label and text field
        JLabel lblWorkorder = new JLabel("Workorder:");
        lblWorkorder.setFont(labelFont);
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(lblWorkorder, gbc);

        txtWorkorder = new JTextField(25);
        txtWorkorder.setEditable(false);
        gbc.gridx = 1;
        panel.add(txtWorkorder, gbc);

        // Program label and text field
        JLabel lblProgram = new JLabel("Program:");
        lblProgram.setFont(labelFont);
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(lblProgram, gbc);

        txtProgram = new JTextField(25);
        txtProgram.setEditable(false);
        gbc.gridx = 1;
        panel.add(txtProgram, gbc);

        // Status label and text field
        JLabel lblStatus = new JLabel("Status:");
        lblStatus.setFont(labelFont);
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(lblStatus, gbc);

        txtStatus = new JTextField(25);
        txtStatus.setEditable(false);
        gbc.gridx = 1;
        panel.add(txtStatus, gbc);

        // Side label and text field
        JLabel lblSide = new JLabel("Side:");
        lblSide.setFont(labelFont);
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(lblSide, gbc);

        txtSide = new JTextField(25);
        txtSide.setEditable(false);
        gbc.gridx = 1;
        panel.add(txtSide, gbc);

        // Machine name label
        lblMachineName = new JLabel("Machine: ");
        lblMachineName.setFont(labelFont);
        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(lblMachineName, gbc);

        // Version label
        JLabel lblVersion = new JLabel("Version - 1.4.0");
        lblVersion.setFont(labelFont);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(lblVersion, gbc);

        // Log text area with scroll pane
        logTextArea = new JTextArea(10, 50);
        logTextArea.setEditable(false);
        logTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logTextArea);
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(scrollPane, gbc);

        // Create menu bar with settings menu
        JMenuBar menuBar = new JMenuBar();
        JMenu settingsMenu = new JMenu("Settings");
        JMenuItem changeConfigItem = new JMenuItem("Change Configuration File");
        changeConfigItem.addActionListener(e -> promptForConfigFile());
        settingsMenu.add(changeConfigItem);
        menuBar.add(settingsMenu);
        setJMenuBar(menuBar);

        // Add panel to the frame
        add(panel);
    }

    private void loadConfig() {
        String lastConfigPath = prefs.get(CONFIG_PATH_KEY, null);
        if (lastConfigPath != null) {
            File configFile = new File(lastConfigPath);
            if (configFile.exists()) {
                loadConfigFromFile(configFile);
                return;
            } else {
                log("Saved configuration file not found. Prompting for new configuration file.");
            }
        }
        promptForConfigFile();
    }

    private void promptForConfigFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Configuration File");

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File configFile = fileChooser.getSelectedFile();
            if (configFile.exists()) {
                prefs.put(CONFIG_PATH_KEY, configFile.getAbsolutePath());
                loadConfigFromFile(configFile);
            } else {
                log("Selected configuration file does not exist.");
                showErrorInUI("Selected configuration file does not exist.");
            }
        } else {
            log("Configuration file selection was canceled.");
            showErrorInUI("Configuration file selection was canceled.");
        }
    }

    private void loadConfigFromFile(File configFile) {
        try (BufferedReader input = new BufferedReader(new FileReader(configFile))) {
            Properties prop = new Properties();
            prop.load(input);

            // Initialize configuration properties
            logEnable = Boolean.parseBoolean(prop.getProperty("LogEnable", "false"));
            String logDirPath = prop.getProperty("LogPath");
            inputFolderPath = prop.getProperty("InputFolderPath");
            outFolderPath = prop.getProperty("OutputPath");
            backupFolderPath = prop.getProperty("BackupFolderPath");
            topOperationId = prop.getProperty("TopOperationId");
            bottomOperationId = prop.getProperty("BottomOperationId");
            operationDuration = prop.getProperty("OperationDuration");
            sqlConStr = prop.getProperty("ConnectionString");
            String machineName = prop.getProperty("MachineName", "Unknown");

            // Validate logDirPath
            if (logEnable) {
                if (logDirPath != null && !logDirPath.trim().isEmpty()) {
                    logDirPath = logDirPath.trim();
                    if (!logDirPath.endsWith(File.separator)) {
                        logDirPath += File.separator;
                    }

                    File logDir = new File(logDirPath);
                    if (!logDir.exists() || !logDir.isDirectory()) {
                        logEnable = false;
                        logFilePath = null;
                        showErrorInUI("Log directory does not exist: " + logDirPath + ". Logging is disabled.");
                    } else {
                        String logFileName = "Log_" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".txt";
                        logFilePath = Paths.get(logDirPath, logFileName).toString();
                    }
                } else {
                    logEnable = false;
                    logFilePath = null;
                    showErrorInUI("'LogPath' is missing or empty in configuration. Logging is disabled.");
                }
            }

            createDirectoryIfNotExists(inputFolderPath);
            createDirectoryIfNotExists(outFolderPath);
            createDirectoryIfNotExists(backupFolderPath);

            lblMachineName.setText("Machine: " + machineName);

            if (logEnable) {
                log("Configuration loaded successfully from: " + configFile.getAbsolutePath());
                log("Configuration Parameters:");
                log("  Log Enabled: " + logEnable);
                log("  Log Directory: " + logDirPath);
                log("  Log File Path: " + logFilePath);
                log("  Input Folder Path: " + inputFolderPath);
                log("  Output Folder Path: " + outFolderPath);
                log("  Backup Folder Path: " + backupFolderPath);
                log("  Top Operation ID: " + topOperationId);
                log("  Bottom Operation ID: " + bottomOperationId);
                log("  Operation Duration: " + operationDuration);
                log("  SQL Connection String: " + sqlConStr);
                log("  Machine Name: " + machineName);
            }

            // Start processing immediately since side selection is dynamic
            startProcessing();

        } catch (IOException ex) {
            if (logEnable && logFilePath != null) {
                log("Error loading configuration: " + ex.getMessage());
            } else {
                System.err.println("Error loading configuration: " + ex.getMessage());
            }
            showErrorInUI("Error loading configuration: " + ex.getMessage());
        }
    }

    private void startProcessing() {
        Thread csvThread = new Thread(this::CSVFunctionalTesting);
        csvThread.start();
    }

    private void CSVFunctionalTesting() {
        File inputFolder = new File(inputFolderPath);

        while (!stopProcessing) {
            log("Checking for files in input folder: " + inputFolder.getAbsolutePath(), false);

            File[] csvFiles = inputFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
            log("Found " + (csvFiles != null ? csvFiles.length : 0) + " CSV files in input folder.", false);

            if (csvFiles != null && csvFiles.length > 0) {
                for (File inputFile : csvFiles) {
                    log("Processing input file: " + inputFile.getAbsolutePath());

                    try (BufferedReader csvReader = new BufferedReader(new FileReader(inputFile));
                         Connection sqlConn = DriverManager.getConnection(sqlConStr)) {

                        DataRecord dataRecord = extractDataFromCSV(csvReader);

                        // Set operationId based on side extracted from Program Name
                        if ("Bottom".equalsIgnoreCase(dataRecord.side)) {
                            operationId = bottomOperationId;
                        } else if ("TOP".equalsIgnoreCase(dataRecord.side)) {
                            operationId = topOperationId;
                        } else {
                            log("Unknown side detected in Program Name. Cannot determine operation ID.");
                            logInUI("Unknown side detected in Program Name for SerialNumber: " + dataRecord.serialNumber);
                            continue;
                        }

                        fetchDatabaseDetails(sqlConn, dataRecord);

                        log("Extracted values: SerialNumber = " + dataRecord.serialNumber + ", Status = "
                                + dataRecord.status + ", Program = " + dataRecord.program + ", Side = "
                                + dataRecord.side + ", Sequence = " + dataRecord.sequence + ", WorkOrderDetail = " + dataRecord.workOrderDetail);

                        SwingUtilities.invokeLater(() -> {
                            txtSerialNumber.setText(dataRecord.serialNumber);
                            txtStatus.setText(dataRecord.status);
                            txtProgram.setText(dataRecord.program);
                            txtWorkorder.setText(dataRecord.workOrderDetail);
                            txtSide.setText(dataRecord.side);
                        });

                        String dtTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date());

                        createXMLFile(dataRecord, dtTime);

                    } catch (Exception ex) {
                        log("Error processing file " + inputFile.getName() + ": " + ex.getMessage());
                        logInUI("Error processing file: " + inputFile.getName() + ". Error occurred during processing.");
                        ex.printStackTrace();
                    }

                    moveFileToBackup(inputFile);
                }
            } else {
                log("No CSV files found in input folder: " + inputFolder.getAbsolutePath(), false);
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class DataRecord {
        String serialNumber;
        String status;
        String program;
        String sequence;
        String workOrderDetail;
        String side;
    }

    private DataRecord extractDataFromCSV(BufferedReader csvReader) throws Exception {
        DataRecord dataRecord = new DataRecord();
        String line;

        while ((line = csvReader.readLine()) != null) {
            String[] row = line.split(",", -1);

            if (row.length < 2) continue;

            String key = row[0].trim();
            String value = row[1].trim();

            switch (key) {
                case "Barcode":
                    dataRecord.serialNumber = value;
                    break;
                case "Result":
                    if ("Good".equalsIgnoreCase(value)) {
                        dataRecord.status = "PASS";
                    } else if ("NG".equalsIgnoreCase(value)) {
                        dataRecord.status = "FAIL";
                    } else {
                        dataRecord.status = value;
                    }
                    break;
                case "Program Name":
                    dataRecord.program = value;
                    if (value.toUpperCase().contains("BOT")) dataRecord.side = "Bottom";
                    else if (value.toUpperCase().contains("TOP")) dataRecord.side = "TOP";
                    break;
                default:
                    break;
            }
        }

        if (dataRecord.serialNumber == null || dataRecord.status == null || dataRecord.program == null) {
            throw new Exception("One or more required fields are missing in the CSV file.");
        }

        return dataRecord;
    }

    private void fetchDatabaseDetails(Connection sqlConn, DataRecord dataRecord) {
        String query = "SELECT w.[OrderNumber], p.BlockNo " +
                "FROM [ValorQM].[dbo].[tUnitItem] t " +
                "LEFT JOIN [ValorMDM].[dbo].[WorkOrder] w ON t.OrderID = w.ID " +
                "LEFT JOIN [ValorPRO].[dbo].[PanelBlockTrace] p ON t.SerialNumber = p.PcbID OR t.SerialNumber = p.BlockID " +
                "WHERE t.SerialNumber = ?";

        try (PreparedStatement pst = sqlConn.prepareStatement(query)) {
            pst.setString(1, dataRecord.serialNumber);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    dataRecord.workOrderDetail = rs.getString("OrderNumber");
                    dataRecord.sequence = rs.getString("BlockNo");
                    log("Fetched WorkOrder: " + dataRecord.workOrderDetail + ", Sequence: " + dataRecord.sequence);
                } else {
                    log("No data found for SerialNumber: " + dataRecord.serialNumber);
                }
            }
        } catch (SQLException e) {
            log("SQL Error fetching details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void logInUI(String message) {
        SwingUtilities.invokeLater(() -> {
            logTextArea.append(message + "\n");
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        });
        log(message, false);
    }

    private void createXMLFile(DataRecord dataRecord, String dtTime) {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            File xmlFile = new File(outFolderPath, timestamp + "_" + dataRecord.serialNumber + "_GenericTester.xml");

            createDirectoryIfNotExists(outFolderPath);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(xmlFile))) {
                writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
                writer.newLine();
                writer.write("<GenericTester xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
                writer.write("xmlns:xsd=\"http://www.w3.org/2001/XMLSchema-instance\" ");
                writer.write("RecipeId=\"" + dataRecord.program + "\" ");
                writer.write("UserLogin=\"User\" ");
                writer.write("WorkOrderId=\"" + dataRecord.workOrderDetail + "\" ");
                writer.write("OperationId=\"" + operationId + "\" ");
                writer.write("LoopTimestamp=\"" + dtTime + "\" ");
                writer.write("xmlns=\"Valor.GenericTester.xsd\">");
                writer.newLine();
                writer.write("<BoardTestResult BarcodeId=\"" + dataRecord.serialNumber + "\" ");
                writer.write("StatusCode=\"" + dataRecord.status + "\" ");
                writer.write("BoardTestStartTimestamp=\"" + dtTime + "\" ");
                writer.write("OperationDuration=\"" + operationDuration + "\" ");
                writer.write("Sequence=\"" + dataRecord.sequence + "\" ");
                writer.write("/>");
                writer.newLine();
                writer.write("</GenericTester>");
                writer.newLine();
                log("XML file created: " + xmlFile.getAbsolutePath());
            }
        } catch (IOException ex) {
            log("Error creating XML file: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void moveFileToBackup(File inputFile) {
        File backupFile = new File(backupFolderPath, inputFile.getName());
        try {
            Files.move(inputFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log("Moved input file to backup: " + backupFile.getAbsolutePath());
        } catch (IOException e) {
            log("Failed to move input file to backup: " + inputFile.getAbsolutePath() + ". Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createDirectoryIfNotExists(String path) {
        if (path != null && !path.trim().isEmpty()) {
            try {
                Files.createDirectories(Paths.get(path));
                System.out.println("Ensured directory exists: " + path);
            } catch (IOException e) {
                System.err.println("Failed to create directory: " + path);
                showErrorInUI("Failed to create directory: " + path);
            }
        }
    }

    private void log(String message) {
        log(message, true);
    }

    private void log(String message, boolean showInUI) {
        if (logEnable && logFilePath != null && !logFilePath.trim().isEmpty()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true))) {
                writer.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " : " + message + "\n");
            } catch (IOException ex) {
                System.err.println("Error writing to log file: " + ex.getMessage());
            }
        }
        if (showInUI) {
            SwingUtilities.invokeLater(() -> {
                logTextArea.append(message + "\n");
                logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
            });
        }
    }

    private void showErrorInUI(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                AOIParser frame = new AOIParser();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
