package org.flexstudios.notepad.dialogs;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class AboutDialogController {

    @FXML private Button okButton;

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void onOK(javafx.event.ActionEvent event) {
        stage.close();
    }
}