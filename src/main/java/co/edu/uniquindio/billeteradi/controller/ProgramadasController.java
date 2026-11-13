package co.edu.uniquindio.billeteradi.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/**
 * Controlador para Operaciones Programadas (Hito 5 - en desarrollo).
 */
public class ProgramadasController {

    @FXML private ComboBox<String> cmbTipoOp;
    @FXML private ComboBox<String> cmbOrigenProg;
    @FXML private ComboBox<String> cmbDestinoProg;
    @FXML private TextField txtImporteProg;
    @FXML private DatePicker dpFecha;
    @FXML private TextField txtHora;
    @FXML private TableView<String> tblProgramadas;

    @FXML
    public void initialize() {
    }

    @FXML
    private void onProgramar() {
        PrincipalController.mostrarAlerta("Funcionalidad en desarrollo (Hito 5).");
    }

    @FXML
    private void onCancelar() {
        PrincipalController.mostrarAlerta("Funcionalidad en desarrollo (Hito 5).");
    }
}
