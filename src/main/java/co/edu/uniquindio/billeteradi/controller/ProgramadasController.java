package co.edu.uniquindio.billeteradi.controller;

import co.edu.uniquindio.billeteradi.model.entity.Billetera;
import co.edu.uniquindio.billeteradi.model.entity.OperacionProgramada;
import co.edu.uniquindio.billeteradi.model.entity.Usuario;
import co.edu.uniquindio.billeteradi.model.enums.TipoTransaccion;
import co.edu.uniquindio.billeteradi.model.exception.ReglaNegocioException;
import co.edu.uniquindio.billeteradi.model.service.ProgramacionService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ProgramadasController {

    @FXML private ComboBox<String> cmbTipoOp;
    @FXML private ComboBox<String> cmbOrigenProg;
    @FXML private ComboBox<String> cmbDestinoProg;
    @FXML private TextField txtImporteProg;
    @FXML private DatePicker dpFecha;
    @FXML private TextField txtHora;
    @FXML private TableView<OperacionProgramada> tblProgramadas;
    @FXML private TableColumn<OperacionProgramada, String> colProgId;
    @FXML private TableColumn<OperacionProgramada, String> colProgTipo;
    @FXML private TableColumn<OperacionProgramada, String> colProgImporte;
    @FXML private TableColumn<OperacionProgramada, String> colProgOrigen;
    @FXML private TableColumn<OperacionProgramada, String> colProgDestino;
    @FXML private TableColumn<OperacionProgramada, String> colProgFecha;
    @FXML private TableColumn<OperacionProgramada, String> colProgEstado;
    @FXML private Label lblRegistrosProg;

    private ProgramacionService programacionService;
    private UsuarioService usuarioService;
    private Runnable onDataChanged;
    private Usuario usuarioActivo;
    private final ObservableList<OperacionProgramada> listaProgramadas = FXCollections.observableArrayList();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        colProgId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
        colProgTipo.setCellValueFactory(c -> new SimpleStringProperty(
                formatearTipo(c.getValue().getTipo().name())));
        colProgImporte.setCellValueFactory(c -> new SimpleStringProperty(
                "$ " + c.getValue().getImporte().toPlainString()));
        colProgOrigen.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getOrigenId() != null ? c.getValue().getOrigenId() : "-"));
        colProgDestino.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDestinoId() != null ? c.getValue().getDestinoId() : "-"));
        colProgFecha.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFechaEjecucion().format(FMT)));
        colProgEstado.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEstado().name()));
        tblProgramadas.setItems(listaProgramadas);

        cmbTipoOp.setItems(FXCollections.observableArrayList("Recarga", "Retiro", "Transferencia"));
    }

    public void initServices(ProgramacionService programacionService, UsuarioService usuarioService,
                             Runnable onDataChanged) {
        this.programacionService = programacionService;
        this.usuarioService = usuarioService;
        this.onDataChanged = onDataChanged;
    }

    public void setUsuarioActivo(Usuario usuario) {
        this.usuarioActivo = usuario;
        cargarCombosBilleteras();
        refrescarTabla();
    }

    public void refrescar() {
        cargarCombosBilleteras();
        refrescarTabla();
    }

    private void cargarCombosBilleteras() {
        cmbOrigenProg.getItems().clear();
        cmbDestinoProg.getItems().clear();
        if (usuarioActivo == null) return;

        List<Billetera> propias = usuarioService.listarBilleteras(usuarioActivo.getId());
        for (Billetera b : propias) {
            String item = b.getId() + " - " + b.getNombre() + " ($" + b.getSaldo().toPlainString() + ")";
            cmbOrigenProg.getItems().add(item);
            cmbDestinoProg.getItems().add(item);
        }

        for (Usuario u : usuarioService.listarUsuarios()) {
            if (u.getId().equals(usuarioActivo.getId())) continue;
            for (Billetera b : usuarioService.listarBilleteras(u.getId())) {
                String item = b.getId() + " - " + u.getNombre() + " / " + b.getNombre();
                cmbDestinoProg.getItems().add(item);
            }
        }
    }

    private void refrescarTabla() {
        listaProgramadas.clear();
        if (programacionService == null) return;
        List<OperacionProgramada> todas = programacionService.listarTodas();
        listaProgramadas.addAll(todas);
        lblRegistrosProg.setText(listaProgramadas.size() + " registros");
    }

    @FXML
    private void onProgramar() {
        if (usuarioActivo == null) {
            PrincipalController.mostrarAlerta("Seleccione un usuario activo.");
            return;
        }
        if (programacionService == null) {
            PrincipalController.mostrarAlerta("Servicio de programacion no disponible.");
            return;
        }

        String tipoStr = cmbTipoOp.getValue();
        if (tipoStr == null) {
            PrincipalController.mostrarAlerta("Seleccione un tipo de operacion.");
            return;
        }

        TipoTransaccion tipo;
        switch (tipoStr) {
            case "Recarga" -> tipo = TipoTransaccion.RECARGA;
            case "Retiro" -> tipo = TipoTransaccion.RETIRO;
            case "Transferencia" -> tipo = TipoTransaccion.TRANSFERENCIA;
            default -> {
                PrincipalController.mostrarAlerta("Tipo de operacion no valido.");
                return;
            }
        }

        String origenId = extraerIdBilletera(cmbOrigenProg.getValue());
        String destinoId = extraerIdBilletera(cmbDestinoProg.getValue());

        if (tipo == TipoTransaccion.RECARGA && destinoId == null) {
            destinoId = origenId;
            origenId = null;
        }
        if (tipo == TipoTransaccion.TRANSFERENCIA && (origenId == null || destinoId == null)) {
            PrincipalController.mostrarAlerta("Seleccione billetera origen y destino para transferencias.");
            return;
        }
        if (tipo == TipoTransaccion.RETIRO && origenId == null) {
            PrincipalController.mostrarAlerta("Seleccione billetera origen para retiros.");
            return;
        }

        String importe = txtImporteProg.getText();
        if (importe == null || importe.isBlank()) {
            PrincipalController.mostrarAlerta("Ingrese un importe valido.");
            return;
        }

        LocalDate fecha = dpFecha.getValue();
        String horaStr = txtHora.getText();
        if (fecha == null || horaStr == null || horaStr.isBlank()) {
            PrincipalController.mostrarAlerta("Ingrese fecha y hora de ejecucion.");
            return;
        }

        LocalTime hora;
        try {
            hora = LocalTime.parse(horaStr.trim());
        } catch (Exception e) {
            PrincipalController.mostrarAlerta("Formato de hora invalido. Use HH:mm (ej: 14:30).");
            return;
        }

        LocalDateTime fechaEjecucion = fecha.atTime(hora);

        try {
            OperacionProgramada op = programacionService.programar(
                    usuarioActivo.getId(), origenId, destinoId,
                    tipo, new java.math.BigDecimal(importe), fechaEjecucion);
            PrincipalController.mostrarExito(
                    "Operacion programada exitosamente\n"
                    + "ID: " + op.getId() + "\n"
                    + "Tipo: " + formatearTipo(op.getTipo().name()) + "\n"
                    + "Importe: $ " + op.getImporte().toPlainString() + "\n"
                    + "Fecha programada: " + op.getFechaEjecucion().format(FMT));
            txtImporteProg.clear();
            txtHora.clear();
            dpFecha.setValue(null);
            refrescarTabla();
            if (onDataChanged != null) onDataChanged.run();
        } catch (ReglaNegocioException e) {
            PrincipalController.mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onProcesarVencidas() {
        if (programacionService == null) {
            PrincipalController.mostrarAlerta("Servicio de programacion no disponible.");
            return;
        }
        try {
            programacionService.procesarVencidas(LocalDateTime.now());
            PrincipalController.mostrarExito("Se procesaron las operaciones vencidas.");
            refrescarTabla();
            if (onDataChanged != null) onDataChanged.run();
        } catch (Exception e) {
            PrincipalController.mostrarError("Error al procesar vencidas: " + e.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        OperacionProgramada seleccionada = tblProgramadas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            PrincipalController.mostrarAlerta("Seleccione una operacion de la tabla.");
            return;
        }
        if (seleccionada.getEstado() != co.edu.uniquindio.billeteradi.model.enums.EstadoProgramacion.PENDIENTE) {
            PrincipalController.mostrarAlerta("Solo se pueden cancelar operaciones en estado PENDIENTE.");
            return;
        }
        seleccionada.setEstado(co.edu.uniquindio.billeteradi.model.enums.EstadoProgramacion.CANCELADA);
        PrincipalController.mostrarExito("Operacion " + seleccionada.getId() + " cancelada.");
        refrescarTabla();
        if (onDataChanged != null) onDataChanged.run();
    }

    private String extraerIdBilletera(String item) {
        if (item == null || item.isBlank()) return null;
        int sep = item.indexOf(" - ");
        return sep > 0 ? item.substring(0, sep).trim() : item.trim();
    }

    private String formatearTipo(String raw) {
        if (raw == null) return "";
        String lower = raw.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
