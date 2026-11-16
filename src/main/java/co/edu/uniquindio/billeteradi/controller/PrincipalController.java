package co.edu.uniquindio.billeteradi.controller;

import co.edu.uniquindio.billeteradi.model.entity.Billetera;
import co.edu.uniquindio.billeteradi.model.entity.Notificacion;
import co.edu.uniquindio.billeteradi.model.entity.Usuario;
import co.edu.uniquindio.billeteradi.model.enums.TipoBilletera;
import co.edu.uniquindio.billeteradi.model.repository.PersistenciaJson;
import co.edu.uniquindio.billeteradi.model.repository.RepositorioMemoria;
import co.edu.uniquindio.billeteradi.model.service.MovimientoService;
import co.edu.uniquindio.billeteradi.model.service.NotificacionService;
import co.edu.uniquindio.billeteradi.model.service.RecompensaService;
import co.edu.uniquindio.billeteradi.model.service.UsuarioService;
import co.edu.uniquindio.billeteradi.model.service.AnaliticaService;
import co.edu.uniquindio.billeteradi.model.service.ProgramacionService;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Controlador principal de la aplicación.
 * Gestiona navegación lateral, usuario activo y sincronización entre módulos.
 */
public class PrincipalController {

    public PrincipalController() {
    }

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
    @FXML private Button btnNotificaciones;

    @FXML private Node usuariosView;
    @FXML private UsuariosController usuariosViewController;
    @FXML private Node billeterasView;
    @FXML private BilleterasController billeterasViewController;
    @FXML private Node movimientosView;
    @FXML private MovimientosController movimientosViewController;
    @FXML private Node programadasView;
    @FXML private ProgramadasController programadasViewController;
    @FXML private Node reportesView;
    @FXML private ReportesController reportesViewController;

    private RepositorioMemoria repositorio;
    private UsuarioService usuarioService;
    private MovimientoService movimientoService;
    private RecompensaService recompensaService;
    private NotificacionService notificacionService;
    private PersistenciaJson persistencia;
    private AnaliticaService analiticaService;
    private ProgramacionService programacionService;

    private Usuario usuarioActivo;

    @FXML
    public void initialize() {
        repositorio = new RepositorioMemoria();
        usuarioService = new UsuarioService(repositorio);
        recompensaService = new RecompensaService(repositorio);
        notificacionService = new NotificacionService();
        movimientoService = new MovimientoService(repositorio, usuarioService, recompensaService, notificacionService);
        analiticaService = new AnaliticaService(repositorio);
        programacionService = new ProgramacionService(repositorio, movimientoService, notificacionService);
        persistencia = new PersistenciaJson();

        // 1. Cargar datos primero (persistencia JSON o datos semilla)
        if (!persistencia.cargar(repositorio, usuarioService, programacionService, analiticaService)) {
            cargarDatosSemilla();
            persistencia.guardar(repositorio, programacionService);
        }

        // Inicializar ranking de fidelización con los usuarios cargados
        for (Usuario u : repositorio.listarUsuarios()) {
            recompensaService.actualizarEnRanking(u.getId(), null, u.getPuntosAcumuladosNetos());
        }

        // 2. Inyectar servicios a los subcontroladores con datos ya disponibles
        usuariosViewController.initServices(usuarioService, this::onDataChanged);
        billeterasViewController.initServices(usuarioService, this::onDataChanged);
        movimientosViewController.initServices(usuarioService, movimientoService, this::onDataChanged);
        programadasViewController.initServices(programacionService, usuarioService, this::onDataChanged);
        reportesViewController.initServices(analiticaService, usuarioService, recompensaService);

        // 3. Poblar selector de usuario activo
        refrescarUsuarios();

        // 4. Mostrar vista inicial de usuarios
        onNavUsuarios();
    }

    private void cargarDatosSemilla() {
        usuarioService.registrar("1001", "Andres Montenegro");
        usuarioService.registrar("1002", "Nicolas Alaguna");
        usuarioService.registrar("1003", "Augusto Mise");
        usuarioService.registrar("1004", "Valentina Gomez");
        usuarioService.registrar("1005", "Santiago Morales");
        usuarioService.registrar("1006", "Camila Restrepo");

        Usuario u1 = usuarioService.obtenerUsuario("1001");
        u1.setPuntosAcumuladosNetos(5420);
        u1.setPuntosDisponibles(5420);
        u1.setNivel(co.edu.uniquindio.billeteradi.model.enums.NivelUsuario.PLATINO);

        Usuario u2 = usuarioService.obtenerUsuario("1002");
        u2.setPuntosAcumuladosNetos(2150);
        u2.setPuntosDisponibles(2150);
        u2.setNivel(co.edu.uniquindio.billeteradi.model.enums.NivelUsuario.ORO);

        Usuario u3 = usuarioService.obtenerUsuario("1003");
        u3.setPuntosAcumuladosNetos(750);
        u3.setPuntosDisponibles(750);
        u3.setNivel(co.edu.uniquindio.billeteradi.model.enums.NivelUsuario.PLATA);

        Usuario u4 = usuarioService.obtenerUsuario("1004");
        u4.setPuntosAcumuladosNetos(180);
        u4.setPuntosDisponibles(180);
        u4.setNivel(co.edu.uniquindio.billeteradi.model.enums.NivelUsuario.BRONCE);

        Usuario u5 = usuarioService.obtenerUsuario("1005");
        u5.setPuntosAcumuladosNetos(30);
        u5.setPuntosDisponibles(30);
        u5.setNivel(co.edu.uniquindio.billeteradi.model.enums.NivelUsuario.BRONCE);

        Usuario u6 = usuarioService.obtenerUsuario("1006");
        u6.setActivo(false);

        Billetera b1 = usuarioService.crearBilletera("1001", "Ahorro Personal", TipoBilletera.AHORRO);
        b1.setSaldo(new java.math.BigDecimal("2500.00"));
        Billetera b2 = usuarioService.crearBilletera("1001", "Gastos Diarios", TipoBilletera.GASTOS_DIARIOS);
        b2.setSaldo(new java.math.BigDecimal("35.00"));
        Billetera b3 = usuarioService.crearBilletera("1001", "Fondo de Inversion", TipoBilletera.INVERSION);
        b3.setSaldo(new java.math.BigDecimal("12000.00"));
        Billetera b4 = usuarioService.crearBilletera("1001", "Compras Especiales", TipoBilletera.COMPRAS);
        b4.setSaldo(new java.math.BigDecimal("150.00"));
        b4.setEstado(co.edu.uniquindio.billeteradi.model.enums.EstadoBilletera.BLOQUEADA);

        Billetera b5 = usuarioService.crearBilletera("1002", "Inversiones Cripto", TipoBilletera.INVERSION);
        b5.setSaldo(new java.math.BigDecimal("4800.00"));
        Billetera b6 = usuarioService.crearBilletera("1002", "Ahorro Vacaciones", TipoBilletera.AHORRO);
        b6.setSaldo(new java.math.BigDecimal("920.00"));
        Billetera b7 = usuarioService.crearBilletera("1002", "Transporte Masivo", TipoBilletera.TRANSPORTE);
        b7.setSaldo(new java.math.BigDecimal("0.00"));

        Billetera b8 = usuarioService.crearBilletera("1003", "Compras Online", TipoBilletera.COMPRAS);
        b8.setSaldo(new java.math.BigDecimal("450.00"));
        Billetera b9 = usuarioService.crearBilletera("1003", "Gastos Basicos", TipoBilletera.GASTOS_DIARIOS);
        b9.setSaldo(new java.math.BigDecimal("0.00"));
        Billetera b10 = usuarioService.crearBilletera("1003", "Ahorro Emergencias", TipoBilletera.AHORRO);
        b10.setSaldo(new java.math.BigDecimal("1200.00"));

        Billetera b11 = usuarioService.crearBilletera("1004", "Transporte Urbano", TipoBilletera.TRANSPORTE);
        b11.setSaldo(new java.math.BigDecimal("25.00"));
        Billetera b12 = usuarioService.crearBilletera("1004", "Gastos Compartidos", TipoBilletera.GASTOS_DIARIOS);
        b12.setSaldo(new java.math.BigDecimal("300.00"));

        Billetera b13 = usuarioService.crearBilletera("1005", "Ahorro Navideno", TipoBilletera.AHORRO);
        b13.setSaldo(new java.math.BigDecimal("15.00"));

        Billetera b14 = usuarioService.crearBilletera("1006", "Billetera Inactiva", TipoBilletera.GASTOS_DIARIOS);
        b14.setSaldo(new java.math.BigDecimal("0.00"));
        b14.setEstado(co.edu.uniquindio.billeteradi.model.enums.EstadoBilletera.BLOQUEADA);
    }

    private void onDataChanged() {
        refrescarUsuarios();
        usuariosViewController.refrescar();
        if (analiticaService != null) {
            for (co.edu.uniquindio.billeteradi.model.entity.Transaccion tx : repositorio.listarTodasTransacciones()) {
                analiticaService.registrarTransaccionEnMontos(tx);
            }
        }
        persistencia.guardar(repositorio, programacionService);
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
            actualizarUsuarioActivo();
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
        String claseNivel = switch (usuarioActivo.getNivel()) {
            case PLATA -> "nivel-plata";
            case ORO -> "nivel-oro";
            case PLATINO -> "nivel-platino";
            default -> "badge-nivel";
        };
        if ("badge-nivel".equals(claseNivel)) {
            lblNivel.getStyleClass().setAll("badge-nivel");
        } else {
            lblNivel.getStyleClass().setAll("badge-nivel", claseNivel);
        }

        lblPuntos.setText(usuarioActivo.getPuntosAcumuladosNetos() + " pts");

        billeterasViewController.setUsuarioActivo(usuarioActivo);
        movimientosViewController.setUsuarioActivo(usuarioActivo);
        programadasViewController.setUsuarioActivo(usuarioActivo);
        actualizarBadgeNotificaciones();
    }

    private void actualizarBadgeNotificaciones() {
        if (btnNotificaciones == null || notificacionService == null || usuarioActivo == null) return;
        notificacionService.despacharTodas();
        long noLeidas = notificacionService.contarNoLeidas(usuarioActivo.getId());
        btnNotificaciones.setText("(" + noLeidas + ")");

        if (btnNotificaciones.getGraphic() != null) {
            if (noLeidas > 0) {
                btnNotificaciones.getGraphic().getStyleClass().setAll("bell-icon", "bell-icon-activa");
            } else {
                btnNotificaciones.getGraphic().getStyleClass().setAll("bell-icon");
            }
        }
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
    private void onVerNotificaciones() {
        if (usuarioActivo == null) {
            mostrarAlerta("Seleccione un usuario activo.");
            return;
        }
        notificacionService.despacharTodas();
        List<Notificacion> lista = notificacionService.obtenerNotificacionesUsuario(usuarioActivo.getId());

        if (lista.isEmpty()) {
            mostrarExito("No hay notificaciones recientes para " + usuarioActivo.getNombre() + ".");
            return;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        for (Notificacion n : lista) {
            sb.append(n.isLeida() ? "[Leída] " : "[NUEVA] ")
                    .append(n.getFecha().format(fmt))
                    .append(" — ")
                    .append(n.getMensaje())
                    .append("\n\n");
        }

        notificacionService.marcarTodasComoLeidas(usuarioActivo.getId());
        actualizarBadgeNotificaciones();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notificaciones Recientes");
        alert.setHeaderText("Alertas de " + usuarioActivo.getNombre() + " (máx. 20 recientes)");
        alert.setContentText(sb.toString().trim());
        alert.showAndWait();
    }

    @FXML
    private void onNavProgramadas() {
        if (usuarioActivo == null) {
            mostrarAlerta("Seleccione un usuario activo antes de programar operaciones.");
            return;
        }
        mostrarVista(programadasView, btnNavProgramadas);
        programadasViewController.refrescar();
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

    static Optional<ButtonType> mostrarAlertaConfimacion(String titulo, String header, String contenido) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(header);
        alert.setContentText(contenido);
        return alert.showAndWait();
    }
}
