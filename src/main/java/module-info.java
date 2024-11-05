module com.example.proje {
    requires javafx.fxml;

    requires com.dlsc.formsfx;
    requires java.sql;
    requires com.jfoenix;
    requires transitive javafx.controls;
    requires java.desktop;

    opens com.example.proje to javafx.fxml;
    exports com.example.proje;
}