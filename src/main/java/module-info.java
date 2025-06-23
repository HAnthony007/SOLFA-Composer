module com.demo51.demo51 {
    requires javafx.controls;
    requires javafx.fxml;
    requires jfugue;
    requires java.desktop;


    opens com.demo51.demo51 to javafx.fxml;
    exports com.demo51.demo51;
}