package co.edu.uniquindio.billeteradi.controller;

import co.edu.uniquindio.billeteradi.model.entity.Usuario;
import co.edu.uniquindio.billeteradi.model.exception.ReglaNegocioException;
import co.edu.uniquindio.billeteradi.model.service.UsuarioService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador de la vista de gestión de usuarios (RF01).
 */
public class UsuariosController {

    @FXML private TableView<Usuario> tblUsuarios;
    @FXML private TableColumn<Usuario, String> colId;
    @FXML private TableColumn<Usuario, String> colNombre;
    @FXML private TableColumn<Usuario, String> colEstado;
    @FXML private TableColumn<Usuario, String> colNivel;
    @FXML private TableColumn<Usuario, String> colPuntosAcum;
    @FXML private TableColumn<Usuario, String> colPuntosDisp;
    @FXML private TextField txtBuscar;
    @FXML private TextField txtId;
    @FXML private TextField txtNombre;
    @FXML private Label lblRegistros;

    private UsuarioService usuarioService;
    private Runnable onDataChanged;
    private final ObservableList<Usuario> listaUsuarios = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActivo() ? "Activo" : "Inactivo"));
        colNivel.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNivel().name()));
        colPuntosAcum.setCellValueFactory(c -> new SimpleStringProperty(
                String.valueOf(c.getValue().getPuntosAcumuladosNetos())));
        colPuntosDisp.setCellValueFactory(c -> new SimpleStringProperty(
                String.valueOf(c.getValue().getPuntosDisponibles())));

        tblUsuarios.setItems(listaUsuarios);

        tblUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, viejo, nuevo) -> {
            if (nuevo != null) {
                txtId.setText(nuevo.getId());
                txtNombre.setText(nuevo.getNombre());
                txtId.setDisable(true);
            }
        });

        txtBuscar.textProperty().addListener((obs, viejo, nuevo) -> filtrar(nuevo));
    }

    public void initServices(UsuarioService usuarioService, Runnable onDataChanged) {
        this.usuarioService = usuarioService;
        this.onDataChanged = onDataChanged;
        refrescar();
    }

    public void refrescar() {
        if (usuarioService == null) return;
        listaUsuarios.setAll(usuarioService.listarUsuarios());
        lblRegistros.setText(listaUsuarios.size() + " registros");
    }

    private void filtrar(String texto) {
        if (usuarioService == null) return;
        if (texto == null || texto.isBlank()) {
            refrescar();
            return;
        }
        String lower = texto.toLowerCase();
        List<Usuario> filtrados = usuarioService.listarUsuarios().stream()
                .filter(u -> u.getId().toLowerCase().contains(lower)
                        || u.getNombre().toLowerCase().contains(lower))
                .collect(Collectors.toList());
        listaUsuarios.setAll(filtrados);
        lblRegistros.setText(filtrados.size() + " registros");
    }

    @FXML
    private void onRegistrar() {
        try {
            usuarioService.registrar(txtId.getText(), txtNombre.getText());
            PrincipalController.mostrarExito("Usuario registrado exitosamente.");
            limpiarFormulario();
            refrescar();
            if (onDataChanged != null) onDataChanged.run();
        } catch (ReglaNegocioException e) {
            PrincipalController.mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onEditar() {
        try {
            usuarioService.editar(txtId.getText(), txtNombre.getText());
            PrincipalController.mostrarExito("Usuario editado exitosamente.");
            refrescar();
            if (onDataChanged != null) onDataChanged.run();
        } catch (ReglaNegocioException e) {
            PrincipalController.mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onDesactivar() {
        Usuario sel = tblUsuarios.getSelectionModel().getSelectedItem();
        if (sel == null) {
            PrincipalController.mostrarAlerta("Seleccione un usuario de la tabla.");
            return;
        }
        try {
            usuarioService.desactivar(sel.getId());
            PrincipalController.mostrarExito("Usuario '" + sel.getNombre() + "' desactivado exitosamente.");
            refrescar();
            if (onDataChanged != null) onDataChanged.run();
        } catch (ReglaNegocioException e) {
            PrincipalController.mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onReactivar() {
        Usuario sel = tblUsuarios.getSelectionModel().getSelectedItem();
        if (sel == null) {
            PrincipalController.mostrarAlerta("Seleccione un usuario de la tabla.");
            return;
        }
        try {
            usuarioService.reactivar(sel.getId());
            PrincipalController.mostrarExito("Usuario '" + sel.getNombre() + "' reactivado exitosamente.");
            refrescar();
            if (onDataChanged != null) onDataChanged.run();
        } catch (ReglaNegocioException e) {
            PrincipalController.mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onLimpiar() {
        limpiarFormulario();
    }

    private void limpiarFormulario() {
        txtId.clear();
        txtNombre.clear();
        txtId.setDisable(false);
        tblUsuarios.getSelectionModel().clearSelection();
    }
}
