package org.flexstudios.notesjavafx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

import java.io.IOException;


public class Notes extends Application {
    public static Stage window;
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Notes.class.getResource("design-notes.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        window = stage;
        window.setTitle("*Untitled.txt - Notes");
        window.getIcons().add(new Image(getClass().getResourceAsStream("icon.png")));
        window.setOnCloseRequest(event ->{
            if(Controls.isModified){
                switch(Controls.promptForUnsavedChanges()){
                    case 0://save
                        Controls.saveCurrentFile();
                        break;
                    case 1:// save without config
                        Controls.config = false;
                        Controls.saveCurrentFile();
                        break;
                    case 2://don't save
                        window.close();
                        break;
                    case 3:
                    default:
                        event.consume();
                }
            }
            Controls.saveBackup();
        });
        window.setScene(scene);
        window.show();
    }

    /*public static void newWindow() throws IOException {
        window = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(Notes.class.getResource("design-notes.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        window.setTitle("Untitled - Notes");
        window.setScene(scene);
        window.show();
    }*/
    public static void main(String[] args) {
        launch();

    }
}