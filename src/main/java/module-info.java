module org.flexstudios.notesjavafx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    opens org.flexstudios.notesjavafx to javafx.fxml;
    exports org.flexstudios.notesjavafx;

    opens org.flexstudios.notepad to javafx.fxml;
    exports org.flexstudios.notepad;

    opens org.flexstudios.notepad.dialogs to javafx.fxml;
    exports org.flexstudios.notepad.dialogs;
}