package org.flexstudios.notepad.dialogs;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

public class GoToDialogController {

    @FXML private Label lineRangeLabel;
    @FXML private TextField lineNumberTextField;
    @FXML private Button okButton;
    @FXML private Button cancelButton;

    private Stage stage;
    private javafx.scene.control.TextArea textArea;
    private boolean result = false;
    private int lineNumber = 1;

    public void setStage(Stage stage) {
        this.stage = stage;

        // Handle Enter key in line number field
        lineNumberTextField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                onOK(null);
            }
        });

        // Update line range label
        updateLineRangeLabel();
    }

    public void setTextArea(javafx.scene.control.TextArea textArea) {
        this.textArea = textArea;
        updateLineRangeLabel();
    }

    private void updateLineRangeLabel() {
        if (textArea != null) {
            int totalLines = getTotalLines();
            lineRangeLabel.setText("Line number (1 to " + totalLines + "):");
        }
    }

    private int getTotalLines() {
        String content = textArea.getText();
        if (content.isEmpty()) {
            return 1;
        }
        return content.split("\n").length;
    }

    public boolean getResult() {
        return result;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @FXML
    private void onOK(javafx.event.ActionEvent event) {
        String lineText = lineNumberTextField.getText().trim();

        if (lineText.isEmpty()) {
            showError("Please enter a line number");
            return;
        }

        try {
            int requestedLine = Integer.parseInt(lineText);
            int totalLines = getTotalLines();

            if (requestedLine < 1 || requestedLine > totalLines) {
                showError("Line number must be between 1 and " + totalLines);
                return;
            }

            lineNumber = requestedLine;
            result = true;
            stage.close();

        } catch (NumberFormatException e) {
            showError("Please enter a valid number");
        }
    }

    @FXML
    private void onCancel(javafx.event.ActionEvent event) {
        result = false;
        stage.close();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Go To");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}