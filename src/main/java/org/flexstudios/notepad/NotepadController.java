package org.flexstudios.notepad;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.flexstudios.notepad.dialogs.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

public class NotepadController implements Initializable {

    @FXML private MenuBar menuBar;
    @FXML private TextArea textArea;
    @FXML private HBox statusBar;
    @FXML private Label lineNumberLabel;
    @FXML private Label columnNumberLabel;
    @FXML private Label characterCountLabel;
    @FXML private Label zoomLabel;
    @FXML private Label wordWrapLabel;
    @FXML private CheckMenuItem wordWrapMenuItem;
    @FXML private CheckMenuItem statusBarMenuItem;

    private Stage stage;
    private File currentFile;
    private boolean isModified = false;
    private File lastUsedDirectory;
    private double defaultFontSize = 11.0;
    private double currentFontSize = 11.0;
    private final double minFontSize = 8.0;
    private final double maxFontSize = 72.0;
    private String lastFindText = "";
    private boolean lastMatchCase = false;
    private boolean lastWrapAround = true;
    private boolean lastSearchDown = true;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeTextArea();
        initializeStatusBar();
        initializeMenuState();
        updateWindowTitle();
        updateStatusBar();
        lastUsedDirectory = FileManager.getLastUsedDirectory();
    }

    private void initializeTextArea() {
        textArea.setWrapText(true);
        textArea.setFont(new Font("Consolas", currentFontSize));

        // Listen for text changes
        textArea.textProperty().addListener((obs, oldText, newText) -> {
            setModified(true);
            updateStatusBar();
        });

        // Listen for caret position changes
        textArea.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
            updateStatusBar();
        });

        // Setup global keyboard shortcuts
        setupGlobalShortcuts();
    }

    private void setupGlobalShortcuts() {
        // Add keyboard event filter for system-wide shortcuts
        textArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F3) {
                onFindNext(null);
                event.consume();
            }
        });
    }

    private void initializeStatusBar() {
        updateStatusBar();
    }

    private void initializeMenuState() {
        updateMenuState();
    }

    private void updateMenuState() {
        // Update undo/redo based on text area state
        if (textArea.getUndoManager() != null) {
            // Note: JavaFX TextArea undo manager is not easily accessible
            // We'll rely on built-in context menu for undo/redo
        }
    }

    private void updateWindowTitle() {
        String title = currentFile != null ? currentFile.getName() : "Untitled";
        if (isModified) {
            title = "*" + title;
        }
        title += " - Notepad";
        stage.setTitle(title);
    }

    private void updateStatusBar() {
        if (statusBar.isVisible()) {
            // Line and column numbers
            int caretPos = textArea.getCaretPosition();
            int lineNumber = getLineNumber(caretPos);
            int columnNumber = getColumnNumber(caretPos);

            lineNumberLabel.setText("Ln " + lineNumber);
            columnNumberLabel.setText("Col " + columnNumber);

            // Character count
            int charCount = textArea.getLength();
            characterCountLabel.setText(charCount + " characters");

            // Zoom level
            int zoomPercentage = (int) Math.round((currentFontSize / defaultFontSize) * 100);
            zoomLabel.setText(zoomPercentage + "%");

            // Word wrap status
            wordWrapLabel.setText(textArea.isWrapText() ? "Windows (CRLF)" : "Windows (CRLF)");
        }
    }

    private int getLineNumber(int caretPosition) {
        String text = textArea.getText().substring(0, Math.min(caretPosition, textArea.getLength()));
        return text.split("\n").length;
    }

    private int getColumnNumber(int caretPosition) {
        String text = textArea.getText();
        int lineStart = text.lastIndexOf('\n', caretPosition - 1);
        return caretPosition - (lineStart + 1);
    }

    private void setModified(boolean modified) {
        if (this.isModified != modified) {
            this.isModified = modified;
            updateWindowTitle();
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;

        // Setup window close handler
        stage.setOnCloseRequest(event -> {
            if (!handleUnsavedChanges()) {
                event.consume();
            } else {
                Platform.exit();
            }
        });
    }

    private boolean handleUnsavedChanges() {
        if (!isModified) {
            return true;
        }

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Notepad");
        alert.setHeaderText("Do you want to save changes?");
        String fileName = currentFile != null ? currentFile.getName() : "Untitled";
        alert.setContentText("Do you want to save changes to " + fileName + "?");

        ButtonType saveButton = new ButtonType("Save");
        ButtonType dontSaveButton = new ButtonType("Don't Save");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(saveButton, dontSaveButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == saveButton) {
                return handleSave();
            } else if (result.get() == dontSaveButton) {
                return true;
            }
        }
        return false; // Cancelled
    }

    // File Menu Actions
    @FXML
    private void onNew(ActionEvent event) {
        if (handleUnsavedChanges()) {
            textArea.clear();
            currentFile = null;
            setModified(false);
            updateWindowTitle();
        }
    }

    @FXML
    private void onOpen(ActionEvent event) {
        if (handleUnsavedChanges()) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open");
            fileChooser.setInitialDirectory(lastUsedDirectory);

            // Add file filters
            FileChooser.ExtensionFilter txtFilter = new FileChooser.ExtensionFilter("Text Documents (*.txt)", "*.txt");
            FileChooser.ExtensionFilter allFilter = new FileChooser.ExtensionFilter("All Files (*.*)", "*.*");
            fileChooser.getExtensionFilters().addAll(txtFilter, allFilter);

            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                try {
                    String content = FileManager.readFile(selectedFile);
                    textArea.setText(content);
                    currentFile = selectedFile;
                    lastUsedDirectory = selectedFile.getParentFile();
                    setModified(false);
                    updateWindowTitle();
                } catch (IOException e) {
                    showError("Error reading file: " + e.getMessage());
                }
            }
        }
    }

    private boolean handleSave() {
        if (currentFile == null) {
            return handleSaveAs();
        } else {
            try {
                FileManager.writeFile(currentFile, textArea.getText());
                setModified(false);
                updateWindowTitle();
                return true;
            } catch (IOException e) {
                showError("Error saving file: " + e.getMessage());
                return false;
            }
        }
    }

    @FXML
    private void onSave(ActionEvent event) {
        handleSave();
    }

    private boolean handleSaveAs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save As");
        fileChooser.setInitialDirectory(lastUsedDirectory);

        // Add file filters
        FileChooser.ExtensionFilter txtFilter = new FileChooser.ExtensionFilter("Text Documents (*.txt)", "*.txt");
        FileChooser.ExtensionFilter allFilter = new FileChooser.ExtensionFilter("All Files (*.*)", "*.*");
        fileChooser.getExtensionFilters().addAll(txtFilter, allFilter);

        // Set initial file name
        if (currentFile != null) {
            fileChooser.setInitialFileName(currentFile.getName());
        } else {
            fileChooser.setInitialFileName("Untitled.txt");
        }

        File selectedFile = fileChooser.showSaveDialog(stage);
        if (selectedFile != null) {
            try {
                FileManager.writeFile(selectedFile, textArea.getText());
                currentFile = selectedFile;
                lastUsedDirectory = selectedFile.getParentFile();
                setModified(false);
                updateWindowTitle();
                return true;
            } catch (IOException e) {
                showError("Error saving file: " + e.getMessage());
                return false;
            }
        }
        return false; // User cancelled
    }

    @FXML
    private void onSaveAs(ActionEvent event) {
        handleSaveAs();
    }

    @FXML
    private void onPageSetup(ActionEvent event) {
        // TODO: Implement page setup dialog
        showInfo("Page Setup dialog not implemented yet.");
    }

    @FXML
    private void onPrint(ActionEvent event) {
        // TODO: Implement print functionality
        showInfo("Print functionality not implemented yet.");
    }

    @FXML
    private void onExit(ActionEvent event) {
        if (handleUnsavedChanges()) {
            Platform.exit();
        }
    }

    // Edit Menu Actions
    @FXML
    private void onUndo(ActionEvent event) {
        textArea.undo();
    }

    @FXML
    private void onRedo(ActionEvent event) {
        textArea.redo();
    }

    @FXML
    private void onCut(ActionEvent event) {
        textArea.cut();
    }

    @FXML
    private void onCopy(ActionEvent event) {
        textArea.copy();
    }

    @FXML
    private void onPaste(ActionEvent event) {
        textArea.paste();
    }

    @FXML
    private void onDelete(ActionEvent event) {
        textArea.replaceSelection("");
    }

    @FXML
    private void onFind(ActionEvent event) {
        showFindReplaceDialog(false);
    }

    @FXML
    private void onFindNext(ActionEvent event) {
        if (!lastFindText.isEmpty()) {
            performQuickFind();
        } else {
            showFindReplaceDialog(false);
        }
    }

    @FXML
    private void onReplace(ActionEvent event) {
        showFindReplaceDialog(true);
    }

    @FXML
    private void onGoTo(ActionEvent event) {
        showGoToDialog();
    }

    @FXML
    private void onSelectAll(ActionEvent event) {
        textArea.selectAll();
    }

    @FXML
    private void onTimeDate(ActionEvent event) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a M/d/yyyy"));
        textArea.insertText(textArea.getCaretPosition(), timestamp);
    }

    // Format Menu Actions
    @FXML
    private void onWordWrap(ActionEvent event) {
        boolean wordWrap = wordWrapMenuItem.isSelected();
        textArea.setWrapText(wordWrap);
        updateStatusBar();
    }

    @FXML
    private void onFont(ActionEvent event) {
        showFontDialog();
    }

    // View Menu Actions
    @FXML
    private void onZoomIn(ActionEvent event) {
        if (currentFontSize < maxFontSize) {
            currentFontSize += 1;
            updateFontSize();
        }
    }

    @FXML
    private void onZoomOut(ActionEvent event) {
        if (currentFontSize > minFontSize) {
            currentFontSize -= 1;
            updateFontSize();
        }
    }

    @FXML
    private void onRestoreDefaultZoom(ActionEvent event) {
        currentFontSize = defaultFontSize;
        updateFontSize();
    }

    private void updateFontSize() {
        Font currentFont = textArea.getFont();
        Font newFont = new Font(currentFont.getFamily(), currentFontSize);
        textArea.setFont(newFont);
        updateStatusBar();
    }

    @FXML
    private void onStatusBar(ActionEvent event) {
        boolean visible = statusBarMenuItem.isSelected();
        statusBar.setVisible(visible);
        statusBar.setManaged(visible);
        updateStatusBar();
    }

    // Help Menu Actions
    @FXML
    private void onViewHelp(ActionEvent event) {
        // TODO: Implement help system
        showInfo("Help system not implemented yet.");
    }

    @FXML
    private void onAbout(ActionEvent event) {
        showAboutDialog();
    }

    // Utility Methods
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Dialog Helper Methods
    private void showFindReplaceDialog(boolean isReplaceMode) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/flexstudios/notepad/dialogs/find-replace.fxml"));
            Parent root = loader.load();
            FindReplaceDialogController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle(isReplaceMode ? "Replace" : "Find");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(stage);
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);

            controller.setStage(dialogStage);
            controller.setTextArea(textArea);
            controller.setReplaceMode(isReplaceMode);
            controller.setFindText(lastFindText);

            dialogStage.showAndWait();

            // Store search parameters for next time
            if (controller.getResult()) {
                lastFindText = controller.getFindText();
                lastMatchCase = controller.isMatchCase();
                lastWrapAround = controller.isWrapAround();
                lastSearchDown = controller.isSearchDown();
            }

        } catch (IOException e) {
            showError("Error opening find/replace dialog: " + e.getMessage());
        }
    }

    private void performQuickFind() {
        if (lastFindText.isEmpty()) {
            return;
        }

        String content = textArea.getText();
        int currentPos = textArea.getCaretPosition();
        int selectionStart = textArea.getSelection().getStart();
        int selectionEnd = textArea.getSelection().getEnd();

        // Determine search start position
        int startPos;
        if (lastSearchDown) {
            startPos = (selectionStart != selectionEnd) ? selectionEnd : currentPos;
        } else {
            startPos = (selectionStart != selectionEnd) ? selectionStart : currentPos;
        }

        // Perform search
        String searchContent = lastMatchCase ? content : content.toLowerCase();
        String searchPattern = lastMatchCase ? lastFindText : lastFindText.toLowerCase();

        int foundIndex = -1;

        if (lastSearchDown) {
            foundIndex = searchContent.indexOf(searchPattern, startPos);
            if (foundIndex == -1 && lastWrapAround) {
                foundIndex = searchContent.indexOf(searchPattern);
            }
        } else {
            foundIndex = searchContent.lastIndexOf(searchPattern, startPos - lastFindText.length());
            if (foundIndex == -1 && lastWrapAround) {
                foundIndex = searchContent.lastIndexOf(searchPattern);
            }
        }

        if (foundIndex != -1) {
            textArea.selectRange(foundIndex, foundIndex + lastFindText.length());
            textArea.requestFocus();
        } else {
            showError("Cannot find \"" + lastFindText + "\"");
        }
    }

    private void showGoToDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/flexstudios/notepad/dialogs/go-to.fxml"));
            Parent root = loader.load();
            GoToDialogController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Go To");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(stage);
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);

            controller.setStage(dialogStage);
            controller.setTextArea(textArea);

            dialogStage.showAndWait();

            if (controller.getResult()) {
                goToLine(controller.getLineNumber());
            }

        } catch (IOException e) {
            showError("Error opening Go To dialog: " + e.getMessage());
        }
    }

    private void goToLine(int lineNumber) {
        String content = textArea.getText();
        String[] lines = content.split("\n");

        if (lineNumber < 1 || lineNumber > lines.length) {
            return;
        }

        // Calculate position of the line
        int position = 0;
        for (int i = 0; i < lineNumber - 1; i++) {
            position += lines[i].length() + 1; // +1 for newline
        }

        textArea.positionCaret(position);
        textArea.requestFocus();
    }

    private void showFontDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/flexstudios/notepad/dialogs/font.fxml"));
            Parent root = loader.load();
            FontDialogController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Font");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(stage);
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);

            controller.setStage(dialogStage);
            controller.setInitialFont(textArea.getFont());

            dialogStage.showAndWait();

            if (controller.getResult()) {
                Font selectedFont = controller.getSelectedFont();
                textArea.setFont(selectedFont);
                currentFontSize = selectedFont.getSize();
                defaultFontSize = 11.0; // Keep default for zoom calculations
                updateStatusBar();
            }

        } catch (IOException e) {
            showError("Error opening Font dialog: " + e.getMessage());
        }
    }

    private void showAboutDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/flexstudios/notepad/dialogs/about.fxml"));
            Parent root = loader.load();
            AboutDialogController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("About Notepad");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(stage);
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);

            controller.setStage(dialogStage);

            dialogStage.showAndWait();

        } catch (IOException e) {
            showError("Error opening About dialog: " + e.getMessage());
        }
    }
}