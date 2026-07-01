module org.exemple {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires com.github.kwhat.jnativehook;
    requires java.sql;
    requires jbcrypt;

    requires io.github.cdimascio.dotenv.java;

    opens org.example to javafx.graphics, javafx.fxml;

    exports org.example;
}