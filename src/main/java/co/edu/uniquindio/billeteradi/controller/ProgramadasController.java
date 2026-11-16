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

    public ProgramadasController() {
    }

    @FXML private ComboBox<String> cmbTipoOp;
    @FXML private Label lblOrigenProg;
    @FXML private ComboBox<String> cmbOrigenProg;
    @FXML private Label lblDestinoProg;
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
        cmbTipoOp.valueProperty().addListener((obs, viejo, nuevo) -> actualizarCamposPorTipo(nuevo));
    }

    private void actualizarCamposPorTipo(String tipo) {
        if ("Recarga".equals(tipo)) {
            if (lblOrigenProg != null) lblOrigenProg.setText("BILLETERA A RECARGAR");
            if (cmbOrigenProg != null) cmbOrigenProg.setPromptText("Seleccione billetera a recargar...");
            if (lblDestinoProg != null) lblDestinoProg.setText("BILLETERA DESTINO (No aplica)");
            if (cmbDestinoProg != null) {
                cmbDestinoProg.setDisable(true);
                cmbDestinoProg.setValue(null);
            }
        } else if ("Retiro".equals(tipo)) {
            if (lblOrigenProg != null) lblOrigenProg.setText("BILLETERA DE RETIRO");
            if (cmbOrigenProg != null) cmbOrigenProg.setPromptText("Seleccione billetera de retiro...");
            if (lblDestinoProg != null) lblDestinoProg.setText("BILLETERA DESTINO (No aplica)");
            if (cmbDestinoProg != null) {
                cmbDestinoProg.setDisable(true);
                cmbDestinoProg.setValue(null);
            }
        } else if ("Transferencia".equals(tipo)) {
            if (lblOrigenProg != null) lblOrigenProg.setText("BILLETERA ORIGEN");
            if (cmbOrigenProg != null) cmbOrigenProg.setPromptText("Seleccione origen...");
            if (lblDestinoProg != null) lblDestinoProg.setText("BILLETERA DESTINO");
            if (cmbDestinoProg != null) {
                cmbDestinoProg.setDisable(false);
                cmbDestinoProg.setPromptText("Seleccione destino...");
            }
        } else {
            if (lblOrigenProg != null) lblOrigenProg.setText("BILLETERA ORIGEN");
            if (lblDestinoProg != null) lblDestinoProg.setText("BILLETERA DESTINO");
            if (cmbDestinoProg != null) cmbDestinoProg.setDisable(false);
        }
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

        if (tipo == TipoTransaccion.RECARGA) {
            if (origenId == null) {
                PrincipalController.mostrarAlerta("Seleccione la billetera a recargar.");
                return;
            }
            destinoId = origenId;
            origenId = null;
        } else if (tipo == TipoTransaccion.RETIRO) {
            if (origenId == null) {
                PrincipalController.mostrarAlerta("Seleccione la billetera de la cual retirar.");
                return;
            }
            destinoId = null;
        } else if (tipo == TipoTransaccion.TRANSFERENCIA) {
            if (origenId == null || destinoId == null) {
                PrincipalController.mostrarAlerta("Seleccione billetera origen y destino para transferencias.");
                return;
            }
            if (origenId.equals(destinoId)) {
                PrincipalController.mostrarAlerta("La billetera origen y destino no pueden ser la misma.");
                return;
            }
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
            cmbOrigenProg.setValue(null);
            cmbDestinoProg.setValue(null);
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
            int procesadas = programacionService.procesarVencidas(LocalDateTime.now());
            if (procesadas > 0) {
                PrincipalController.mostrarExito("Se procesaron " + procesadas + " operacion(es) vencida(s).");
            } else {
                long totalPendientes = programacionService.listarTodas().stream()
                        .filter(op -> op.getEstado() == co.edu.uniquindio.billeteradi.model.enums.EstadoProgramacion.PENDIENTE)
                        .count();
                if (totalPendientes == 0) {
                    PrincipalController.mostrarAlerta("No hay operaciones pendientes por procesar.");
                } else {
                    PrincipalController.mostrarAlerta(
                            "No hay operaciones vencidas a la fecha y hora actual.\n\n"
                            + "Hay " + totalPendientes + " operacion(es) pendiente(s) programada(s) para mas adelante.\n"
                            + "Para ejecutarlas inmediatamente sin esperar la fecha, seleccione la fila y use 'Ejecutar Seleccionada', o use 'Forzar Todas las Pendientes'.");
                }
            }
            refrescarTabla();
            if (onDataChanged != null) onDataChanged.run();
        } catch (Exception e) {
            PrincipalController.mostrarError("Error al procesar vencidas: " + e.getMessage());
        }
    }

    @FXML
    private void onEjecutarSeleccionada() {
        if (programacionService == null) {
            PrincipalController.mostrarAlerta("Servicio de programacion no disponible.");
            return;
        }
        OperacionProgramada seleccionada = tblProgramadas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            PrincipalController.mostrarAlerta("Seleccione una operacion de la tabla para ejecutarla.");
            return;
        }
        if (seleccionada.getEstado() != co.edu.uniquindio.billeteradi.model.enums.EstadoProgramacion.PENDIENTE) {
            PrincipalController.mostrarAlerta("Solo se pueden ejecutar operaciones en estado PENDIENTE.");
            return;
        }

        try {
            programacionService.ejecutarOperacion(seleccionada);
            if (seleccionada.getEstado() == co.edu.uniquindio.billeteradi.model.enums.EstadoProgramacion.EJECUTADA) {
                PrincipalController.mostrarExito("Operacion " + seleccionada.getId() + " ejecutada exitosamente.\n"
                        + "Transaccion generada: " + seleccionada.getTransaccionGeneradaId());
            } else if (seleccionada.getEstado() == co.edu.uniquindio.billeteradi.model.enums.EstadoProgramacion.FALLIDA) {
                PrincipalController.mostrarAlerta("La operacion " + seleccionada.getId() + " fallo al ejecutarse.\n"
                        + "El estado se actualizo a FALLIDA (verifique fondos o estado de la billetera).");
            }
            refrescarTabla();
            if (onDataChanged != null) onDataChanged.run();
        } catch (Exception e) {
            PrincipalController.mostrarError("Error al ejecutar operacion: " + e.getMessage());
        }
    }

    @FXML
    private void onForzarTodas() {
        if (programacionService == null) {
            PrincipalController.mostrarAlerta("Servicio de programacion no disponible.");
            return;
        }
        try {
            int procesadas = programacionService.procesarTodasPendientes();
            if (procesadas > 0) {
                PrincipalController.mostrarExito("Se ejecuto el procesamiento forzado de " + procesadas + " operacion(es) pendiente(s).");
            } else {
                PrincipalController.mostrarAlerta("No hay operaciones pendientes por ejecutar.");
            }
            refrescarTabla();
            if (onDataChanged != null) onDataChanged.run();
        } catch (Exception e) {
            PrincipalController.mostrarError("Error al forzar ejecucion: " + e.getMessage());
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
        programacionService.removerDePendientes(seleccionada);
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
