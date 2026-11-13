package co.edu.uniquindio.billeteradi.controller;

import co.edu.uniquindio.billeteradi.model.entity.Billetera;
import co.edu.uniquindio.billeteradi.model.entity.Usuario;
import co.edu.uniquindio.billeteradi.model.enums.EstadoBilletera;
import co.edu.uniquindio.billeteradi.model.enums.TipoBilletera;
import co.edu.uniquindio.billeteradi.model.exception.ReglaNegocioException;
import co.edu.uniquindio.billeteradi.model.service.UsuarioService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;

/**
 * Controlador de la vista de billeteras (RF02).
 */
public class BilleterasController {

    @FXML private TableView<Billetera> tblBilleteras;
    @FXML private TableColumn<Billetera, String> colCodigo;
    @FXML private TableColumn<Billetera, String> colNombreB;
    @FXML private TableColumn<Billetera, String> colTipo;
    @FXML private TableColumn<Billetera, String> colSaldo;
    @FXML private TableColumn<Billetera, String> colEstadoB;
    @FXML private TextField txtNombreBilletera;
    @FXML private ComboBox<TipoBilletera> cmbTipo;
    @FXML private Label lblUsuarioActivo;
    @FXML private Label lblRegistros;

    private UsuarioService usuarioService;
    private Runnable onDataChanged;
    private Usuario usuarioActivo;
    private final ObservableList<Billetera> listaBilleteras = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colCodigo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
        colNombreB.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colTipo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTipo().name()));
        colSaldo.setCellValueFactory(c -> new SimpleStringProperty("$ " + c.getValue().getSaldo().toPlainString()));
        colEstadoB.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEstado().name()));
        tblBilleteras.setItems(listaBilleteras);
        cmbTipo.setItems(FXCollections.observableArrayList(TipoBilletera.values()));
    }

    public void initServices(UsuarioService usuarioService, Runnable onDataChanged) {
        this.usuarioService = usuarioService;
        this.onDataChanged = onDataChanged;
    }

    public void setUsuarioActivo(Usuario usuario) {
        this.usuarioActivo = usuario;
        if (usuario != null) {
            lblUsuarioActivo.setText("Usuario: " + usuario.getId() + " — " + usuario.getNombre());
        } else {
            lblUsuarioActivo.setText("Sin usuario seleccionado");
        }
        refrescar();
    }

    public void refrescar() {
        if (usuarioService == null || usuarioActivo == null) {
            listaBilleteras.clear();
            lblRegistros.setText("0 registros");
            return;
        }
        List<Billetera> billeteras = usuarioService.listarBilleteras(usuarioActivo.getId());
        listaBilleteras.setAll(billeteras);
        lblRegistros.setText(billeteras.size() + " registros");
    }

    @FXML
    private void onCrear() {
        if (usuarioActivo == null) {
            PrincipalController.mostrarAlerta("Seleccione un usuario activo.");
            return;
        }
        TipoBilletera tipo = cmbTipo.getValue();
        if (tipo == null) {
            PrincipalController.mostrarAlerta("Seleccione un tipo de billetera.");
            return;
        }
        try {
            Billetera b = usuarioService.crearBilletera(
                    usuarioActivo.getId(), txtNombreBilletera.getText(), tipo);
            PrincipalController.mostrarExito(
                    "Billetera creada exitosamente\n\n"
                    + "Nombre: " + b.getNombre() + "\n"
                    + "Código: " + b.getId() + "\n"
                    + "Tipo: " + b.getTipo().name());
            txtNombreBilletera.clear();
            cmbTipo.getSelectionModel().clearSelection();
            refrescar();
            if (onDataChanged != null) onDataChanged.run();
        } catch (ReglaNegocioException e) {
            PrincipalController.mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onBloquear() {
        Billetera sel = tblBilleteras.getSelectionModel().getSelectedItem();
        if (sel == null) {
            PrincipalController.mostrarAlerta("Seleccione una billetera de la tabla.");
            return;
        }
        try {
            usuarioService.cambiarEstadoBilletera(sel.getId(), EstadoBilletera.BLOQUEADA);
            PrincipalController.mostrarExito(
                    "Billetera bloqueada\n\n"
                    + "Nombre: " + sel.getNombre() + "\n"
                    + "Código: " + sel.getId());
            refrescar();
            if (onDataChanged != null) onDataChanged.run();
        } catch (ReglaNegocioException e) {
            PrincipalController.mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onActivar() {
        Billetera sel = tblBilleteras.getSelectionModel().getSelectedItem();
        if (sel == null) {
            PrincipalController.mostrarAlerta("Seleccione una billetera de la tabla.");
            return;
        }
        try {
            usuarioService.cambiarEstadoBilletera(sel.getId(), EstadoBilletera.ACTIVA);
            PrincipalController.mostrarExito(
                    "Billetera activada\n\n"
                    + "Nombre: " + sel.getNombre() + "\n"
                    + "Código: " + sel.getId());
            refrescar();
            if (onDataChanged != null) onDataChanged.run();
        } catch (ReglaNegocioException e) {
            PrincipalController.mostrarError(e.getMessage());
        }
    }
}
