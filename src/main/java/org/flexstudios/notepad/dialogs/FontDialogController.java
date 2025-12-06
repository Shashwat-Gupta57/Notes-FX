package org.flexstudios.notepad.dialogs;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;

public class FontDialogController {

    @FXML private ListView<String> fontFamilyListView;
    @FXML private ListView<String> fontSizeListView;
    @FXML private CheckBox boldCheckBox;
    @FXML private CheckBox italicCheckBox;
    @FXML private TextArea previewTextArea;
    @FXML private Button okButton;
    @FXML private Button cancelButton;

    private Stage stage;
    private Font initialFont;
    private Font selectedFont;
    private boolean result = false;

    private final List<String> STANDARD_FONT_SIZES = Arrays.asList(
        "8", "9", "10", "11", "12", "14", "16", "18", "20", "22", "24", "28", "36", "48", "72"
    );

    public void setStage(Stage stage) {
        this.stage = stage;

        // Initialize font family list
        ObservableList<String> fontFamilies = FXCollections.observableArrayList();
        fontFamilies.addAll(javafx.scene.text.Font.getFamilies());
        fontFamilyListView.setItems(fontFamilies);

        // Initialize font size list
        fontSizeListView.setItems(FXCollections.observableArrayList(STANDARD_FONT_SIZES));

        // Set up selection listeners
        fontFamilyListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        fontSizeListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        boldCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> updatePreview());
        italicCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> updatePreview());

        // Set double-click handlers
        fontFamilyListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onOK(null);
            }
        });

        fontSizeListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onOK(null);
            }
        });
    }

    public void setInitialFont(Font font) {
        this.initialFont = font;
        this.selectedFont = font;

        // Select current font family
        String fontFamily = font.getFamily();
        ObservableList<String> families = fontFamilyListView.getItems();
        for (int i = 0; i < families.size(); i++) {
            if (families.get(i).equalsIgnoreCase(fontFamily)) {
                fontFamilyListView.getSelectionModel().select(i);
                fontFamilyListView.getFocusModel().focus(i);
                fontFamilyListView.scrollTo(i);
                break;
            }
        }

        // Select current font size
        double fontSize = font.getSize();
        String sizeStr = String.valueOf((int) Math.round(fontSize));
        ObservableList<String> sizes = fontSizeListView.getItems();
        for (int i = 0; i < sizes.size(); i++) {
            if (sizes.get(i).equals(sizeStr)) {
                fontSizeListView.getSelectionModel().select(i);
                fontSizeListView.getFocusModel().focus(i);
                fontSizeListView.scrollTo(i);
                break;
            }
        }

        // Set style checkboxes
        String styleName = font.getStyle().toLowerCase();
        boldCheckBox.setSelected(styleName.contains("bold"));
        italicCheckBox.setSelected(styleName.contains("italic"));

        updatePreview();
    }

    public boolean getResult() {
        return result;
    }

    public Font getSelectedFont() {
        return selectedFont;
    }

    private void updatePreview() {
        String fontFamily = fontFamilyListView.getSelectionModel().getSelectedItem();
        String fontSizeStr = fontSizeListView.getSelectionModel().getSelectedItem();

        if (fontFamily != null && fontSizeStr != null) {
            try {
                double fontSize = Double.parseDouble(fontSizeStr);
                FontWeight weight = boldCheckBox.isSelected() ? FontWeight.BOLD : FontWeight.NORMAL;
                String style = italicCheckBox.isSelected() ? "Italic" : "Regular";

                Font previewFont = Font.font(fontFamily, weight, fontSize);
                previewTextArea.setFont(previewFont);

                // Store the selected font
                selectedFont = Font.font(fontFamily, weight, fontSize);

            } catch (NumberFormatException e) {
                // Invalid size, use default
                Font previewFont = Font.font(fontFamily, 12);
                previewTextArea.setFont(previewFont);
            }
        }
    }

    @FXML
    private void onOK(javafx.event.ActionEvent event) {
        result = true;
        stage.close();
    }

    @FXML
    private void onCancel(javafx.event.ActionEvent event) {
        result = false;
        stage.close();
    }
}