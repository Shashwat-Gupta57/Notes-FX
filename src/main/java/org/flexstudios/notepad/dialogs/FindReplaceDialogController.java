package org.flexstudios.notepad.dialogs;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

public class FindReplaceDialogController {

    @FXML private TextField findTextField;
    @FXML private TextField replaceTextField;
    @FXML private Label replaceLabel;
    @FXML private CheckBox matchCaseCheckBox;
    @FXML private CheckBox wrapAroundCheckBox;
    @FXML private RadioButton upRadioButton;
    @FXML private RadioButton downRadioButton;
    @FXML private ToggleGroup directionToggleGroup;
    @FXML private VBox optionsBox;
    @FXML private VBox directionBox;
    @FXML private HBox buttonsBox;
    @FXML private Button findNextButton;
    @FXML private Button replaceButton;
    @FXML private Button replaceAllButton;
    @FXML private Button cancelButton;

    private Stage stage;
    private javafx.scene.control.TextArea textArea;
    private boolean isReplaceMode = false;
    private boolean result = true; // Dialog result - always true when closed normally

    public void setStage(Stage stage) {
        this.stage = stage;

        // Set up radio button toggle group
        upRadioButton.setToggleGroup(directionToggleGroup);
        downRadioButton.setToggleGroup(directionToggleGroup);

        // Handle Enter key in find text field
        findTextField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                onFindNext(null);
            }
        });

        // Handle Enter key in replace text field
        replaceTextField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                onReplace(null);
            }
        });
    }

    public void setTextArea(javafx.scene.control.TextArea textArea) {
        this.textArea = textArea;
    }

    public void setReplaceMode(boolean replaceMode) {
        this.isReplaceMode = replaceMode;
        updateUIForMode();
    }

    private void updateUIForMode() {
        if (isReplaceMode) {
            replaceLabel.setVisible(true);
            replaceTextField.setVisible(true);
            replaceButton.setVisible(true);
            replaceAllButton.setVisible(true);
            stage.setTitle("Replace");
        } else {
            replaceLabel.setVisible(false);
            replaceTextField.setVisible(false);
            replaceButton.setVisible(false);
            replaceAllButton.setVisible(false);
            stage.setTitle("Find");
        }
    }

    @FXML
    private void onFindNext(javafx.event.ActionEvent event) {
        String searchText = findTextField.getText();
        if (searchText.isEmpty()) {
            showError("Find what: text not found");
            return;
        }

        boolean found = performSearch(searchText);
        if (!found) {
            showError("Cannot find \"" + searchText + "\"");
        }
    }

    private boolean performSearch(String searchText) {
        String content = textArea.getText();
        int currentPos = textArea.getCaretPosition();
        int selectionStart = textArea.getSelection().getStart();
        int selectionEnd = textArea.getSelection().getEnd();

        // Determine search start position
        int startPos;
        boolean searchDown = downRadioButton.isSelected();

        if (searchDown) {
            // If we have a selection, start after it
            startPos = (selectionStart != selectionEnd) ? selectionEnd : currentPos;
        } else {
            // If we have a selection, start before it
            startPos = (selectionStart != selectionEnd) ? selectionStart : currentPos;
        }

        // Perform case-sensitive or case-insensitive search
        String searchContent = matchCaseCheckBox.isSelected() ? content : content.toLowerCase();
        String searchPattern = matchCaseCheckBox.isSelected() ? searchText : searchText.toLowerCase();

        int foundIndex = -1;

        if (searchDown) {
            // Search forward
            foundIndex = searchContent.indexOf(searchPattern, startPos);
            if (foundIndex == -1 && wrapAroundCheckBox.isSelected()) {
                foundIndex = searchContent.indexOf(searchPattern);
            }
        } else {
            // Search backward
            foundIndex = searchContent.lastIndexOf(searchPattern, startPos - searchText.length());
            if (foundIndex == -1 && wrapAroundCheckBox.isSelected()) {
                foundIndex = searchContent.lastIndexOf(searchPattern);
            }
        }

        if (foundIndex != -1) {
            // Select the found text
            textArea.selectRange(foundIndex, foundIndex + searchText.length());
            textArea.requestFocus();
            return true;
        }

        return false;
    }

    @FXML
    private void onReplace(javafx.event.ActionEvent event) {
        String searchText = findTextField.getText();
        if (searchText.isEmpty()) {
            showError("Find what: text not found");
            return;
        }

        // If we have a selection that matches the search text, replace it
        String selectedText = textArea.getSelectedText();
        boolean shouldReplace = false;

        if (!selectedText.isEmpty()) {
            if (matchCaseCheckBox.isSelected()) {
                shouldReplace = selectedText.equals(searchText);
            } else {
                shouldReplace = selectedText.equalsIgnoreCase(searchText);
            }
        }

        if (shouldReplace) {
            String replaceText = replaceTextField.getText();
            textArea.replaceSelection(replaceText);
        }

        // Find next occurrence
        onFindNext(null);
    }

    @FXML
    private void onReplaceAll(javafx.event.ActionEvent event) {
        String searchText = findTextField.getText();
        if (searchText.isEmpty()) {
            showError("Find what: text not found");
            return;
        }

        String content = textArea.getText();
        String replaceText = replaceTextField.getText();

        int replaceCount = 0;
        String newContent;

        if (matchCaseCheckBox.isSelected()) {
            // Case-sensitive replace
            int index = content.indexOf(searchText);
            while (index != -1) {
                replaceCount++;
                index = content.indexOf(searchText, index + 1);
            }
            newContent = content.replace(searchText, replaceText);
        } else {
            // Case-insensitive replace
            String lowerContent = content.toLowerCase();
            String lowerSearch = searchText.toLowerCase();

            int index = lowerContent.indexOf(lowerSearch);
            while (index != -1) {
                replaceCount++;
                index = lowerContent.indexOf(lowerSearch, index + 1);
            }

            // For case-insensitive replace, we need to replace each occurrence manually
            newContent = content;
            int searchStart = 0;
            while (true) {
                int foundIndex = lowerContent.indexOf(lowerSearch, searchStart);
                if (foundIndex == -1) break;

                newContent = newContent.substring(0, foundIndex) +
                           replaceText +
                           newContent.substring(foundIndex + searchText.length());

                lowerContent = newContent.toLowerCase();
                searchStart = foundIndex + replaceText.length();
            }
        }

        if (replaceCount > 0) {
            textArea.setText(newContent);
            showInfo(replaceCount + " occurrence(s) replaced");
        } else {
            showError("Cannot find \"" + searchText + "\"");
        }
    }

    @FXML
    private void onCancel(javafx.event.ActionEvent event) {
        stage.close();
    }

    public String getFindText() {
        return findTextField.getText();
    }

    public String getReplaceText() {
        return replaceTextField.getText();
    }

    public boolean isMatchCase() {
        return matchCaseCheckBox.isSelected();
    }

    public boolean isWrapAround() {
        return wrapAroundCheckBox.isSelected();
    }

    public boolean isSearchDown() {
        return downRadioButton.isSelected();
    }

    public void setFindText(String text) {
        findTextField.setText(text);
    }

    public boolean getResult() {
        return result;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Find");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Replace");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}