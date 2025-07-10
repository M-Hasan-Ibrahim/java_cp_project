import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

import N_threads_with_queueing.GUI_Model;


public class VideoGUI extends JFrame {
    private JTextField filePathField;
    private JButton chooseButton, runButton;
    private JFileChooser fileChooser;
    private JTextArea outputArea;

    public VideoGUI() {
        setTitle("Video Processor");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Top panel for file selection
        JPanel topPanel = new JPanel(new FlowLayout());
        filePathField = new JTextField(30);
        filePathField.setEditable(false);

        chooseButton = new JButton("Choose Video");
        runButton = new JButton("Run");
        runButton.setEnabled(false);

        topPanel.add(filePathField);
        topPanel.add(chooseButton);
        topPanel.add(runButton);

        add(topPanel, BorderLayout.NORTH);

        // Output area
        outputArea = new JTextArea(15, 50);
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(outputArea);
        add(scrollPane, BorderLayout.CENTER);

        // File chooser
        fileChooser = new JFileChooser();

        // Choose button logic
        chooseButton.addActionListener(e -> {
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                filePathField.setText(selectedFile.getAbsolutePath());
                runButton.setEnabled(true);
                outputArea.setText(""); // Clear previous output
            }
        });

        // Run button logic
        runButton.addActionListener(e -> {
            String path = filePathField.getText();
            if (path.isEmpty()) {
                outputArea.setText("Please choose a file first.");
                return;
            }
            runButton.setEnabled(false);
            chooseButton.setEnabled(false);
            outputArea.setText("Processing, please wait...");

            SwingWorker<String, Void> worker = new SwingWorker<>() {
                @Override
                protected String doInBackground() {
                    try {
                        // Update threads as desired
                        return GUI_Model.Runner(4, path);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return "Error: " + ex.getMessage();
                    }
                }
                @Override
                protected void done() {
                    try {
                        String result = get();
                        outputArea.setText(result);
                    } catch (Exception ex) {
                        outputArea.setText("Error showing results.");
                    }
                    runButton.setEnabled(true);
                    chooseButton.setEnabled(true);
                }
            };
            worker.execute();
        });

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(VideoGUI::new);
    }
}
