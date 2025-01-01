
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;

public class Main extends JFrame {
    private static final String MODELS_DIR = "/Users/rubyrover/Desktop/PJATK/UTP/utp3_project/src/main/java/models";
    private static final String DATA_DIR = "/Users/rubyrover/Desktop/PJATK/UTP/utp3_project/src/main/resources";

    private JComboBox<String> modelComboBox;
    private JComboBox<String> dataComboBox;
    private JTable resultsTable;
    private DefaultTableModel tableModel;

    public Main() {
            setTitle("Modelling framework sample");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(1050, 400);
            setLocationRelativeTo(null);
            setLayout(new BorderLayout());

            // Left panel for model and data selection
            JPanel leftPanel = new JPanel();
            leftPanel.setLayout(new GridLayout(3, 1, 10, 10));

            // Model selection
            JPanel modelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            modelPanel.add(new JLabel("Model:"));
            modelComboBox = new JComboBox<>(getModelNames());
            modelPanel.add(modelComboBox);

            // Data selection
            JPanel dataPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            dataPanel.add(new JLabel("Data:"));
            dataComboBox = new JComboBox<>(getDataFileNames());
            dataPanel.add(dataComboBox);

            // Run model button
            JPanel runModelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton runModelButton = new JButton("Run model");
            runModelButton.addActionListener(e -> runModel());
            runModelPanel.add(runModelButton);

            leftPanel.add(modelPanel);
            leftPanel.add(dataPanel);
            leftPanel.add(runModelPanel);

            // Right panel for results display
            JPanel rightPanel = new JPanel(new BorderLayout());
            tableModel = new DefaultTableModel();
            resultsTable = new JTable(tableModel);
            resultsTable.setGridColor(Color.LIGHT_GRAY);
            resultsTable.setShowGrid(true);
            resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            resultsTable.setTableHeader(null);
            JScrollPane scrollPane = new JScrollPane(resultsTable);
            rightPanel.add(scrollPane, BorderLayout.CENTER);

            // Bottom panel for action buttons
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            JButton runScriptButton = new JButton("Run script from file");
            JButton createAdHocButton = new JButton("Create and run ad hoc script");
            bottomPanel.add(runScriptButton);
            bottomPanel.add(createAdHocButton);

            add(leftPanel, BorderLayout.WEST);
            add(rightPanel, BorderLayout.CENTER);
            add(bottomPanel, BorderLayout.SOUTH);

            // Event listeners for buttons
            runScriptButton.addActionListener(e -> runScriptFromFile());
            createAdHocButton.addActionListener(e -> createAndRunAdHocScript());
        }

    private String[] getModelNames() {
        File modelsDir = new File(MODELS_DIR);
        String[] files = modelsDir.list((dir, name) -> name.endsWith(".java"));

        if (files == null) return new String[0]; // Handle null if no files found

        return Arrays.stream(files)
                .map(name -> name.replace(".java", ""))  // Remove .java extension
                .filter(name -> {
                    try {
                        return !Class.forName("models." + name).isAnnotation();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        return false;
                    }
                })
                .toArray(String[]::new);
    }

        private String[] getDataFileNames() {
            File dataDir = new File(DATA_DIR);
            return dataDir.list((dir, name) -> name.endsWith(".txt"));
        }

    private void runModel() {
        String selectedModel = (String) modelComboBox.getSelectedItem();
        String selectedData = (String) dataComboBox.getSelectedItem();

        try {
            Controller ctl = new Controller(selectedModel);
            ctl.readDataFrom(Paths.get(DATA_DIR, selectedData).toString())
                    .runModel();
            String res = ctl.getResultsAsTsv();
            updateResultsTable(res);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error running the model: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void runScriptFromFile() {
        JFileChooser fileChooser = new JFileChooser(new File("src/main/java/scripts"));
        fileChooser.setDialogTitle("Select a script file to run");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String selectedModel = (String) modelComboBox.getSelectedItem();
            String selectedData = (String) dataComboBox.getSelectedItem();
            try {
                Controller ctl = new Controller(selectedModel);
                String res = ctl.readDataFrom(Paths.get(DATA_DIR, selectedData).toString())
                        .runModel().runScriptFromFile(selectedFile.getAbsolutePath()).getResultsAsTsv();
                updateResultsTable(res);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error running the script: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private void createAndRunAdHocScript() {
        JTextArea scriptArea = new JTextArea(10, 40);
        scriptArea.setLineWrap(true);
        scriptArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(scriptArea);
        int result = JOptionPane.showConfirmDialog(
                this,
                scrollPane,
                "Enter Groovy Script",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String script = scriptArea.getText().trim();
            String selectedModel = (String) modelComboBox.getSelectedItem();
            String selectedData = (String) dataComboBox.getSelectedItem();
            if (!script.isEmpty()) {
                try {
                    Controller ctl = new Controller(selectedModel);
                    ctl.readDataFrom(Paths.get(DATA_DIR, selectedData).toString())
                            .runModel().runScript(script);
                    String res = ctl.getResultsAsTsv();
                    updateResultsTable(res);
                    updateResultsTable(res);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                            "Error running the script: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "Script cannot be empty!",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        }
    }

    private void updateResultsTable(String results) {
        // Clear the existing table model
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);


        // Split the results string into rows
        String[] rows = results.split("\n");

        // Determine the maximum number of columns based on the first row
        int maxColumns = rows[0].split("\t").length;
        tableModel.setColumnCount(maxColumns);

        // Add rows to the table
        for (String row : rows) {
            String[] cells = row.split("\t");
            tableModel.addRow(cells);
        }

        // Adjust the table column widths
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            resultsTable.getColumnModel().getColumn(i).setPreferredWidth(150);
        }
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeLater(() -> {
            Main gui = new Main();
            gui.setVisible(true);
        });
    }
}
