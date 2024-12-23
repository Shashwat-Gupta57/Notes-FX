package org.flexstudios.notesjavafx;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Stack;

public class Controls {


    public static boolean isModified = false;
    private File currentFile = null;
    private SimpleDateFormat formatter = new SimpleDateFormat("dd_MM_yyyy__HH_mm_ss");
    private Font font = null;
    private float defaultFontSize = 16.0f;
    public static boolean encrypt = false;
    //backup every file always
    public static boolean config = true;
    private String img_path = null;
    private Color bgColor = null;
    private Color textColor = null;
    private String EI = null;//to be decided
    private Stack<String> undoStack = new Stack<>();
    private Stack<String> redoStack = new Stack<>();
    private static String path = null;


    @FXML
    public TextArea canvas;
    @FXML
    public Label statusBar;



    public Controls(){
    }
    @FXML
    private void initialize(){
        updateStatusBar();
        font = Font.font("Consolas",defaultFontSize);
        if(!((new File(System.getenv("LOCALAPPDATA")+"\\Notes")).exists())){
            try{
                Files.createDirectories(Paths.get(System.getenv("LOCALAPPDATA")+"\\Notes"));
            }catch (IOException ioe){
                ioe.printStackTrace();
            }
        }
        if(!((new File(System.getenv("LOCALAPPDATA")+"\\Notes\\Backup")).exists())){
            try{
                Files.createDirectories(Paths.get(System.getenv("LOCALAPPDATA")+"\\Notes\\Backup"));
            }catch (IOException ioe){
                ioe.printStackTrace();
            }
        }
        canvas.textProperty().addListener((observable, oldValue, newValue) -> {
            undoStack.push(oldValue);
            redoStack.clear();
        });
        canvas.setOnKeyPressed(e -> {
            if (!isModified) {
                if (Character.isDefined(e.getText().charAt(0)) && !e.isControlDown()) {
                    isModified = true;
                    updateTitle();
                }
            }
            updateStatusBar();
        });



    }
    public static void saveBackup(){

    }
    public static int promptForUnsavedChanges(){
        if(isModified) {
            // Custom options for the JOptionPane
            Object[] options = {"Save","Save Without Config", "Don't Save", "Cancel"};
            int choice = JOptionPane.showOptionDialog(null,
                    "You have unsaved changes. Do you want to save them before exiting?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]);

            switch (choice) {
                case 0:
                    return 0; // opt save
                case 1:
                    return 1; // opt save without config
                case 2:
                    return 2; // opt don't save
                case 3: // opt cancel
                default:
                    return 3; // Cancel exit
            }
        }
        return 2; // No unsaved changes, proceed to exit
    }
    public void updateTitle(){
        if(currentFile != null) {
            String title = currentFile.getName() + " - Notes";
            if (isModified) {
                title = "*" + title;
            }
            setTitle(title);
        } else {
            setTitle("*untitled.txt - Notes");
        }
    }
    public void setTitle(String s){
        Notes.window.setTitle(s);
    }
    public static void saveCurrentFile(){

    }
    private String currentDateAndTime(){
            Date date = new Date();
            String formattedDate = formatter.format(date);
            return formattedDate;
    }

    private void loadSettings() {
        File file = new File("saves.txt");
        if (!file.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("background:")) {
                    String colorStr = line.substring("background:".length());
                    canvas.setStyle("-fx-control-inner-background: #"+colorStr+";");  // Sets the background to yellow
                    bgColor = Color.web(colorStr);
                } else if (line.startsWith("textColor:")) {
                    String colorStr = line.substring("textColor:".length());
                    canvas.setStyle("-fx-text-fill: #"+colorStr+";");  // Sets the text color to red
                    textColor = Color.web(colorStr);
                } else if (line.startsWith("font:")) {
                    String fontStr = line.substring("font:".length());
                    String[] parts = fontStr.split(",");
                    String fontName = parts[0];
                    int fontStyle = Integer.parseInt(parts[1].trim());
                    int fontSize = Integer.parseInt(parts[2].trim());
                    Font font = Font.loadFont(fontName, fontSize);

                    canvas.setFont(font);
                } else if (line.startsWith("BG_Image:")) {
                    String path = line.substring("BG_Image:".length()).trim();
                    img_path = path;
                    Image img = new Image(img_path);
                    BackgroundImage backgroundImage = new BackgroundImage(img,
                            BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                            BackgroundPosition.CENTER, BackgroundSize.DEFAULT);

                    canvas.setBackground(new Background(backgroundImage));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.err.println("Invalid number format in settings file.");
            e.printStackTrace();
        }
    }

    @FXML
    private void updateStatusBar(){
        try {
            int caretPosition = canvas.getCaretPosition();
            String text = canvas.getText();
            int line = text.substring(0, caretPosition).split("\n", -1).length;
            int column = caretPosition - text.lastIndexOf("\n", caretPosition - 1);

            double fontSize = canvas.getFont().getSize();
            String statusText = String.format("Ln: %d, Col: %d, Font Size: %.0fpt", line, column, fontSize);
            statusBar.setText(statusText);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void newFile(){
        canvas.setText("");
        currentFile = null;
        isModified = false;
        updateTitle();
    }
    @FXML
    private void newWindow(){
        System.out.println("new window");

    }
    @FXML
    private void openFile(){
        System.out.println("open");
    }
    @FXML
    public void saveFile(){
        //save settings
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(System.getenv("LOCALAPPDATA")+"\\Notes\\saves.txt"))) {
            Font font = canvas.getFont();
            String bgHex = String.format("#%02X%02X%02X",
                    (int)(bgColor.getRed() * 255),
                    (int)(bgColor.getGreen() * 255),
                    (int)(bgColor.getBlue() * 255));
            String textHex = String.format("#%02X%02X%02X",
                    (int)(textColor.getRed() * 255),
                    (int)(textColor.getGreen() * 255),
                    (int)(textColor.getBlue() * 255));
            writer.write("background:" + bgHex + "\n");
            writer.write("textColor:" + textHex + "\n");
            writer.write("font:" + font.getName() + "," + font.getStyle() + "," + font.getSize() + "\n");
            writer.write("BG_Image:"+img_path+"\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        //save File
    }
    @FXML
    private void saveFileAs(){
        System.out.println("save as");
    }
    @FXML
    private void exitApp(){
        System.out.println("exit");
        if(isModified) {
            switch (promptForUnsavedChanges()) {
                case 0://save
                    saveCurrentFile();
                    break;
                case 1:// save without config
                    config = false;
                    saveCurrentFile();
                    break;
                case 2://don't save
                    Notes.window.close();
                    break;
                case 3:
                default:
                    return;
            }
        }
        saveBackup();
        System.exit(0);
    }
    @FXML
    private void undo(){
        if (!undoStack.isEmpty()) {
            // Move the current state to redo stack before undo
            redoStack.push(canvas.getText());
            // Pop from undo stack and set the text area to that state
            canvas.setText(undoStack.pop());
        }
    }
    @FXML
    private void redo(){
        if (!redoStack.isEmpty()) {
            // Move the current state to undo stack before redo
            undoStack.push(canvas.getText());
            // Pop from redo stack and set the text area to that state
            canvas.setText(redoStack.pop());
        }
    }
    @FXML
    private void cut(){
        System.out.println("cut");
    }
    @FXML
    private void copy(){
        System.out.println("copy");
    }
    @FXML
    private void paste(){
        System.out.println("paste");
    }
    @FXML
    private void delete(){
        System.out.println("delete");
    }
    @FXML
    private void find(){
        System.out.println("find");
    }
    @FXML
    private void replace(){
        System.out.println("replace");
    }
    @FXML
    private void goTo(){
        System.out.println("Go To");
    }
    @FXML
    private void selectAll(){
        System.out.println("selectAll");
    }
    @FXML
    private void timeDate(){
        System.out.println("timeDate");
    }
    @FXML
    private void increaseFontSize(){
        System.out.println("increaseFontSize");
    }
    @FXML
    private void decreaseFontSize(){
        System.out.println("decreaseFontSize");
    }
    @FXML
    private void resetFontSize(){
        System.out.println("resetFontSize");
    }

    @FXML
    private void changeUniversalBackground(){
        System.out.println("changeUniversalBackground");
    }
    @FXML
    private void setImageAsBackground(){
        System.out.println("setImageAsBackground");
    }

    @FXML
    private void changeUniversalTextColor(){
        System.out.println("changeUniversalTextColor");
    }
    @FXML
    private void changeFont(){
        System.out.println("changeFont");
    }
    @FXML
    private void enableEncryption(){
        System.out.println("enableEncryption");
    }

    @FXML
    private void decryptCurrentFile(){
        System.out.println("decryptCurrentFile");
    }
    @FXML
    private void dontSaveConfig(){
        System.out.println("don't Save Config");
    }
    @FXML
    private void viewBackupDirectory(){
        System.out.println("viewBackupDirectory");
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("View backup Directory");
        a.setContentText("The Backup Directory Is: "+System.getenv("LOCALAPPDATA")+"\\Notes\\Backup");
        a.show();
    }
    @FXML
    private void openDirectoryLocation() throws IOException {
        Runtime.getRuntime().exec("explorer %LOCALAPPDATA%\\Notes\\Backup");
    }

}