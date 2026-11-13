package co.edu.uniquindio.billeteradi.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableView;

/**
 * Controlador para Reportes y Analítica (Hito 6 - en desarrollo).
 */
public class ReportesController {

    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private ComboBox<String> cmbMetrica;
    @FXML private TableView<String> tblResultados;

    @FXML
    public void initialize() {
    }

    @FXML
    private void onGenerar() {
        PrincipalController.mostrarAlerta("Funcionalidad en desarrollo (Hito 6).");
    }
}
