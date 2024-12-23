module org.flexstudios.notesjavafx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens org.flexstudios.notesjavafx to javafx.fxml;
    exports org.flexstudios.notesjavafx;
}