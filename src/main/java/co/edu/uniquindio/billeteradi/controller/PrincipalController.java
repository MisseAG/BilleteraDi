package co.edu.uniquindio.billeteradi.controller;

import co.edu.uniquindio.billeteradi.model.entity.Usuario;
import co.edu.uniquindio.billeteradi.model.enums.TipoBilletera;
import co.edu.uniquindio.billeteradi.model.repository.PersistenciaJson;
import co.edu.uniquindio.billeteradi.model.repository.RepositorioMemoria;
import co.edu.uniquindio.billeteradi.model.service.MovimientoService;
import co.edu.uniquindio.billeteradi.model.service.RecompensaService;
import co.edu.uniquindio.billeteradi.model.service.UsuarioService;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.List;

/**
 * Controlador principal de la aplicación.
 * Gestiona navegación lateral, usuario activo y sincronización entre módulos.
 */
public class PrincipalController {

    @FXML private ComboBox<String> cmbUsuarioActivo;
    @FXML private Label lblEstado;
    @FXML private Label lblNivel;
    @FXML private Label lblPuntos;
    @FXML private StackPane contentPane;

    @FXML private Button btnNavUsuarios;
    @FXML private Button btnNavBilleteras;
    @FXML private Button btnNavMovimientos;
    @FXML private Button btnNavProgramadas;
    @FXML private Button btnNavReportes;

    @FXML private Node usuariosView;
    @FXML private UsuariosController usuariosViewController;
    @FXML private Node billeterasView;
    @FXML private BilleterasController billeterasViewController;
    @FXML private Node movimientosView;
    @FXML private MovimientosController movimientosViewController;
    @FXML private Node programadasView;
    @FXML private Node reportesView;

    private RepositorioMemoria repositorio;
    private UsuarioService usuarioService;
    private MovimientoService movimientoService;
    private RecompensaService recompensaService;
    private PersistenciaJson persistencia;

    private Usuario usuarioActivo;

    @FXML
    public void initialize() {
        repositorio = new RepositorioMemoria();
        usuarioService = new UsuarioService(repositorio);
        movimientoService = new MovimientoService(repositorio, usuarioService);
        recompensaService = new RecompensaService(repositorio);
        persistencia = new PersistenciaJson();

        // 1. Cargar datos primero (persistencia JSON o datos semilla)
        if (!persistencia.cargar(repositorio, usuarioService)) {
            cargarDatosSemilla();
            persistencia.guardar(repositorio);
        }

        // 2. Inyectar servicios a los subcontroladores con datos ya disponibles
        usuariosViewController.initServices(usuarioService, this::onDataChanged);
        billeterasViewController.initServices(usuarioService, this::onDataChanged);
        movimientosViewController.initServices(usuarioService, movimientoService, this::onDataChanged);

        // 3. Poblar selector de usuario activo
        refrescarUsuarios();

        // 4. Mostrar vista inicial de usuarios
        onNavUsuarios();
    }

    private void cargarDatosSemilla() {
        usuarioService.registrar("1001", "Ana Garcia");
        usuarioService.registrar("1002", "Carlos Lopez");
        usuarioService.registrar("1003", "Maria Rodriguez");
        usuarioService.crearBilletera("1001", "Ahorro Personal", TipoBilletera.AHORRO);
        usuarioService.crearBilletera("1001", "Gastos Diarios", TipoBilletera.GASTOS_DIARIOS);
        usuarioService.crearBilletera("1002", "Inversiones", TipoBilletera.INVERSION);
        usuarioService.crearBilletera("1003", "Compras Online", TipoBilletera.COMPRAS);
    }

    private void onDataChanged() {
        refrescarUsuarios();
        usuariosViewController.refrescar();
        persistencia.guardar(repositorio);
    }

    public void refrescarUsuarios() {
        List<Usuario> usuarios = usuarioService.listarUsuarios();
        String seleccionado = cmbUsuarioActivo.getValue();
        cmbUsuarioActivo.setItems(FXCollections.observableArrayList(
                usuarios.stream()
                        .map(u -> u.getId() + " — " + u.getNombre())
                        .toList()
        ));
        if (seleccionado != null && cmbUsuarioActivo.getItems().contains(seleccionado)) {
            cmbUsuarioActivo.setValue(seleccionado);
        } else if (!usuarios.isEmpty()) {
            cmbUsuarioActivo.getSelectionModel().selectFirst();
            actualizarUsuarioActivo();
        }
    }

    @FXML
    private void onUsuarioSeleccionado() {
        actualizarUsuarioActivo();
    }

    private void actualizarUsuarioActivo() {
        String sel = cmbUsuarioActivo.getValue();
        if (sel == null || sel.isBlank()) {
            usuarioActivo = null;
            lblEstado.setText("");
            lblNivel.setText("");
            lblPuntos.setText("");
            return;
        }
        String id = sel.split(" — ")[0].trim();
        try {
            usuarioActivo = usuarioService.obtenerUsuario(id);
        } catch (Exception e) {
            usuarioActivo = null;
            return;
        }

        lblEstado.setText(usuarioActivo.isActivo() ? "Activo" : "Inactivo");
        lblEstado.getStyleClass().setAll(usuarioActivo.isActivo() ? "badge-active" : "badge-inactive");
        lblNivel.setText(usuarioActivo.getNivel().name());
        lblPuntos.setText(usuarioActivo.getPuntosAcumuladosNetos() + " pts");

        billeterasViewController.setUsuarioActivo(usuarioActivo);
        movimientosViewController.setUsuarioActivo(usuarioActivo);
    }

    private void mostrarVista(Node vista, Button boton) {
        contentPane.getChildren().forEach(n -> {
            if (n.isVisible()) {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(120), n);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> n.setVisible(false));
                fadeOut.play();
            } else {
                n.setVisible(false);
            }
        });

        vista.setOpacity(0.0);
        vista.setVisible(true);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), vista);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        List.of(btnNavUsuarios, btnNavBilleteras, btnNavMovimientos, btnNavProgramadas, btnNavReportes)
                .forEach(b -> b.getStyleClass().remove("sidebar-button-active"));
        boton.getStyleClass().add("sidebar-button-active");
    }

    @FXML
    private void onNavUsuarios() {
        mostrarVista(usuariosView, btnNavUsuarios);
        usuariosViewController.refrescar();
    }

    @FXML
    private void onNavBilleteras() {
        if (usuarioActivo == null) {
            mostrarAlerta("Seleccione un usuario activo antes de ver billeteras.");
            return;
        }
        mostrarVista(billeterasView, btnNavBilleteras);
        billeterasViewController.refrescar();
    }

    @FXML
    private void onNavMovimientos() {
        if (usuarioActivo == null) {
            mostrarAlerta("Seleccione un usuario activo antes de operar movimientos.");
            return;
        }
        mostrarVista(movimientosView, btnNavMovimientos);
        movimientosViewController.refrescar();
    }

    @FXML
    private void onNavProgramadas() {
        mostrarVista(programadasView, btnNavProgramadas);
    }

    @FXML
    private void onNavReportes() {
        mostrarVista(reportesView, btnNavReportes);
    }

    static void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Aviso");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    static void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de Operación");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    static void mostrarExito(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Operación Exitosa");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
