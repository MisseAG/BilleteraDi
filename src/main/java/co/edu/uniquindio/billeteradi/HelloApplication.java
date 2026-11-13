package co.edu.uniquindio.billeteradi;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                HelloApplication.class.getResource("views/VistaPrincipal.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1200, 720);
        scene.getStylesheets().add(
                HelloApplication.class.getResource("styles/saas.css").toExternalForm());
        stage.setTitle("BilleteraDi — Billetera Digital");
        stage.setMinWidth(1000);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
