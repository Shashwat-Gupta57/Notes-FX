package org.flexstudios.notepad;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;

public class NotepadApplication extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) throws IOException {
        this.primaryStage = primaryStage;

        // Load the FXML UI
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/flexstudios/notepad/notepad.fxml"));
        Parent root = loader.load();

        // Get controller reference
        NotepadController controller = loader.getController();
        controller.setStage(primaryStage);

        // Set up the stage
        primaryStage.setTitle("Untitled - Notepad");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(300);

        // Set application icon if available
        try {
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/org/flexstudios/notepad/icons/notepad.png")));
        } catch (Exception e) {
            // Icon not found, continue without it
        }

        // Set up close handler
        primaryStage.setOnCloseRequest(this::handleWindowClose);

        primaryStage.show();
    }

    private void handleWindowClose(WindowEvent event) {
        // Let the controller handle this
        // The controller will determine if we should close or not
    }

    public static void main(String[] args) {
        launch(args);
    }
}