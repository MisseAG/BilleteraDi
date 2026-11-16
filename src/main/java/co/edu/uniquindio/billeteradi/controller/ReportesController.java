package co.edu.uniquindio.billeteradi.controller;

import co.edu.uniquindio.billeteradi.model.entity.RegistroConteo;
import co.edu.uniquindio.billeteradi.model.entity.Transaccion;
import co.edu.uniquindio.billeteradi.model.entity.Usuario;
import co.edu.uniquindio.billeteradi.model.enums.TipoBilletera;
import co.edu.uniquindio.billeteradi.model.enums.TipoTransaccion;
import co.edu.uniquindio.billeteradi.model.service.AnaliticaService;
import co.edu.uniquindio.billeteradi.model.service.RecompensaService;
import co.edu.uniquindio.billeteradi.model.service.UsuarioService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ReportesController {

    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private ComboBox<String> cmbMetrica;
    @FXML private TextField txtTopK;
    @FXML private TableView<String[]> tblResultados;
    @FXML private TableColumn<String[], String> colMetrica;
    @FXML private TableColumn<String[], String> colValor;
    @FXML private TableColumn<String[], String> colDetalle;
    @FXML private Label lblRegistrosRep;

    private AnaliticaService analiticaService;
    private UsuarioService usuarioService;
    private RecompensaService recompensaService;
    private final ObservableList<String[]> listaResultados = FXCollections.observableArrayList();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String R_FRECUENCIA_TIPO = "Frecuencia por tipo de movimiento";
    private static final String R_CATEGORIAS_ACTIVAS = "Categorias mas activas";
    private static final String R_BILLETERAS_MAYOR_USO = "Billeteras con mayor uso (Top-K)";
    private static final String R_USUARIOS_MAYOR_ACTIVIDAD = "Usuarios con mayor actividad (Top-K)";
    private static final String R_USUARIOS_MAS_TRANSFERENCIAS = "Usuarios con mas transferencias (Top-K)";
    private static final String R_MAYORES_MONTOS = "Transacciones de mayores montos (Top-K)";
    private static final String R_RANKING_PUNTOS = "Ranking de usuarios por puntos (Top-K)";

    public ReportesController() {
    }

    @FXML
    public void initialize() {
        colMetrica.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[0]));
        colValor.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[1]));
        colDetalle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[2]));
        tblResultados.setItems(listaResultados);

        cmbMetrica.setItems(FXCollections.observableArrayList(
                R_FRECUENCIA_TIPO,
                R_CATEGORIAS_ACTIVAS,
                R_BILLETERAS_MAYOR_USO,
                R_USUARIOS_MAYOR_ACTIVIDAD,
                R_USUARIOS_MAS_TRANSFERENCIAS,
                R_MAYORES_MONTOS,
                R_RANKING_PUNTOS
        ));

        txtTopK.setText("5");
    }

    public void initServices(AnaliticaService analiticaService, UsuarioService usuarioService) {
        initServices(analiticaService, usuarioService, null);
    }

    public void initServices(AnaliticaService analiticaService, UsuarioService usuarioService,
                             RecompensaService recompensaService) {
        this.analiticaService = analiticaService;
        this.usuarioService = usuarioService;
        this.recompensaService = recompensaService;
    }

    @FXML
    private void onGenerar() {
        if (analiticaService == null) {
            PrincipalController.mostrarAlerta("El servicio de analitica no esta disponible.");
            return;
        }
        String seleccion = cmbMetrica.getValue();
        if (seleccion == null) {
            PrincipalController.mostrarAlerta("Seleccione un tipo de reporte.");
            return;
        }

        LocalDateTime inicio = dpDesde.getValue() != null
                ? dpDesde.getValue().atStartOfDay() : null;
        LocalDateTime fin = dpHasta.getValue() != null
                ? dpHasta.getValue().atTime(LocalTime.MAX) : null;

        int topK = 5;
        if (txtTopK.getText() != null && !txtTopK.getText().isBlank()) {
            try {
                topK = Integer.parseInt(txtTopK.getText().trim());
                if (topK <= 0) topK = 5;
            } catch (NumberFormatException ignored) {
                topK = 5;
            }
        }

        listaResultados.clear();

        try {
            switch (seleccion) {
                case R_FRECUENCIA_TIPO -> generarFrecuenciaPorTipo(inicio, fin);
                case R_CATEGORIAS_ACTIVAS -> generarCategoriasMasActivas(inicio, fin);
                case R_BILLETERAS_MAYOR_USO -> generarBilleterasMayorUso(inicio, fin, topK);
                case R_USUARIOS_MAYOR_ACTIVIDAD -> generarUsuariosMayorActividad(inicio, fin, topK);
                case R_USUARIOS_MAS_TRANSFERENCIAS -> generarUsuariosMasTransferencias(inicio, fin, topK);
                case R_MAYORES_MONTOS -> generarMayoresMontos(topK);
                case R_RANKING_PUNTOS -> generarRankingPuntos(topK);
                default -> PrincipalController.mostrarAlerta("Reporte no reconocido.");
            }
        } catch (Exception e) {
            PrincipalController.mostrarError(e.getMessage());
        }

        lblRegistrosRep.setText(listaResultados.size() + " registros");
    }

    private void generarFrecuenciaPorTipo(LocalDateTime inicio, LocalDateTime fin) {
        Map<TipoTransaccion, Long> datos = analiticaService.reporteFrecuenciaPorTipo(inicio, fin);
        if (datos.isEmpty()) {
            PrincipalController.mostrarAlerta("No se encontraron movimientos en el periodo indicado.");
            return;
        }
        for (Map.Entry<TipoTransaccion, Long> e : datos.entrySet()) {
            listaResultados.add(new String[]{
                    formatearTipo(e.getKey().name()),
                    String.valueOf(e.getValue()),
                    "Cantidad de operaciones de tipo " + formatearTipo(e.getKey().name())
            });
        }
    }

    private void generarCategoriasMasActivas(LocalDateTime inicio, LocalDateTime fin) {
        Map<TipoBilletera, Long> datos = analiticaService.reporteCategoriasMasActivas(inicio, fin);
        if (datos.isEmpty()) {
            PrincipalController.mostrarAlerta("No se encontraron movimientos en el periodo indicado.");
            return;
        }
        for (Map.Entry<TipoBilletera, Long> e : datos.entrySet()) {
            listaResultados.add(new String[]{
                    formatearTipo(e.getKey().name()),
                    String.valueOf(e.getValue()),
                    "Participaciones de billeteras tipo " + formatearTipo(e.getKey().name())
            });
        }
    }

    private void generarBilleterasMayorUso(LocalDateTime inicio, LocalDateTime fin, int k) {
        List<RegistroConteo> datos = analiticaService.reporteBilleterasMayorUso(inicio, fin, k);
        if (datos.isEmpty()) {
            PrincipalController.mostrarAlerta("No se encontraron datos para el periodo indicado.");
            return;
        }
        for (RegistroConteo r : datos) {
            listaResultados.add(new String[]{
                    r.getId(),
                    String.valueOf(r.getCantidad()),
                    "Billetera con " + r.getCantidad() + " operaciones"
            });
        }
    }

    private void generarUsuariosMayorActividad(LocalDateTime inicio, LocalDateTime fin, int k) {
        List<RegistroConteo> datos = analiticaService.reporteUsuariosMayorActividad(inicio, fin, k);
        if (datos.isEmpty()) {
            PrincipalController.mostrarAlerta("No se encontraron datos para el periodo indicado.");
            return;
        }
        for (RegistroConteo r : datos) {
            String nombre = resolverNombreUsuario(r.getId());
            listaResultados.add(new String[]{
                    r.getId(),
                    String.valueOf(r.getCantidad()),
                    nombre + " — " + r.getCantidad() + " operaciones"
            });
        }
    }

    private void generarUsuariosMasTransferencias(LocalDateTime inicio, LocalDateTime fin, int k) {
        List<RegistroConteo> datos = analiticaService.reporteUsuariosMasTransferencias(inicio, fin, k);
        if (datos.isEmpty()) {
            PrincipalController.mostrarAlerta("No se encontraron datos para el periodo indicado.");
            return;
        }
        for (RegistroConteo r : datos) {
            String nombre = resolverNombreUsuario(r.getId());
            listaResultados.add(new String[]{
                    r.getId(),
                    String.valueOf(r.getCantidad()),
                    nombre + " — " + r.getCantidad() + " transferencias salientes"
            });
        }
    }

    private void generarMayoresMontos(int k) {
        List<Transaccion> datos = analiticaService.consultarMayoresMontos(k);
        if (datos.isEmpty()) {
            PrincipalController.mostrarAlerta("No hay transacciones registradas en el indice de montos.");
            return;
        }
        for (Transaccion tx : datos) {
            listaResultados.add(new String[]{
                    tx.getId(),
                    "$ " + tx.getImporte().toPlainString(),
                    formatearTipo(tx.getTipo().name()) + " — " + tx.getFecha().format(FMT)
            });
        }
    }

    private void generarRankingPuntos(int k) {
        if (recompensaService == null) {
            PrincipalController.mostrarAlerta("El servicio de recompensas no esta disponible.");
            return;
        }
        List<Usuario> ranking = recompensaService.obtenerRankingOrdenado(k);
        if (ranking.isEmpty()) {
            PrincipalController.mostrarAlerta("No hay usuarios con puntos registrados en el ranking.");
            return;
        }
        for (Usuario u : ranking) {
            listaResultados.add(new String[]{
                    u.getId(),
                    u.getPuntosAcumuladosNetos() + " pts",
                    u.getNombre() + " — Nivel " + u.getNivel().name() + " (" + u.getPuntosDisponibles() + " disp.)"
            });
        }
    }

    private String resolverNombreUsuario(String id) {
        if (id == null || usuarioService == null) return "Desconocido";
        try {
            return usuarioService.obtenerUsuario(id).getNombre();
        } catch (Exception e) {
            return id;
        }
    }

    private String formatearTipo(String raw) {
        if (raw == null) return "";
        String lower = raw.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
