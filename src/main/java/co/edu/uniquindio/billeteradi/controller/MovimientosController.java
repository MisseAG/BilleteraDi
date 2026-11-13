package co.edu.uniquindio.billeteradi.controller;

import co.edu.uniquindio.billeteradi.model.entity.Billetera;
import co.edu.uniquindio.billeteradi.model.entity.Transaccion;
import co.edu.uniquindio.billeteradi.model.entity.Usuario;
import co.edu.uniquindio.billeteradi.model.exception.ReglaNegocioException;
import co.edu.uniquindio.billeteradi.model.service.MovimientoService;
import co.edu.uniquindio.billeteradi.model.service.UsuarioService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Controlador de la vista de movimientos financieros (RF03, RF04, RF05).
 */
public class MovimientosController {

    @FXML private Button btnTabRecarga;
    @FXML private Button btnTabRetiro;
    @FXML private Button btnTabTransferencia;

    @FXML private VBox paneRecarga;
    @FXML private VBox paneRetiro;
    @FXML private VBox paneTransferencia;

    @FXML private ComboBox<String> cmbBilleteraRecarga;
    @FXML private TextField txtImporteRecarga;

    @FXML private ComboBox<String> cmbBilleteraRetiro;
    @FXML private TextField txtImporteRetiro;

    @FXML private ComboBox<String> cmbOrigenTransferencia;
    @FXML private ComboBox<String> cmbDestinoTransferencia;
    @FXML private TextField txtImporteTransferencia;

    @FXML private TableView<Transaccion> tblTransacciones;
    @FXML private TableColumn<Transaccion, String> colTxId;
    @FXML private TableColumn<Transaccion, String> colTxTipo;
    @FXML private TableColumn<Transaccion, String> colTxImporte;
    @FXML private TableColumn<Transaccion, String> colTxOrigen;
    @FXML private TableColumn<Transaccion, String> colTxDestino;
    @FXML private TableColumn<Transaccion, String> colTxEstado;
    @FXML private TableColumn<Transaccion, String> colTxFecha;
    @FXML private Label lblRegistrosTx;

    private UsuarioService usuarioService;
    private MovimientoService movimientoService;
    private Runnable onDataChanged;
    private Usuario usuarioActivo;
    private final ObservableList<Transaccion> listaTransacciones = FXCollections.observableArrayList();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final Pattern PATRON_IMPORTE = Pattern.compile("\\d*\\.?\\d{0,2}");

    @FXML
    public void initialize() {
        colTxId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
        colTxTipo.setCellValueFactory(c -> new SimpleStringProperty(formatearTipo(c.getValue().getTipo().name())));
        colTxImporte.setCellValueFactory(c -> new SimpleStringProperty(
                "$ " + c.getValue().getImporte().toPlainString()));
        colTxOrigen.setCellValueFactory(c -> new SimpleStringProperty(
                descripcionBilletera(c.getValue().getOrigenId())));
        colTxDestino.setCellValueFactory(c -> new SimpleStringProperty(
                descripcionBilletera(c.getValue().getDestinoId())));
        colTxEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEstado().name()));
        colTxFecha.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFecha().format(FMT)));
        tblTransacciones.setItems(listaTransacciones);

        aplicarFormatterImporte(txtImporteRecarga);
        aplicarFormatterImporte(txtImporteRetiro);
        aplicarFormatterImporte(txtImporteTransferencia);

        configurarCellFactoryDestino();
        mostrarTab("recarga");
    }

    public void initServices(UsuarioService us, MovimientoService ms, Runnable onDataChanged) {
        this.usuarioService = us;
        this.movimientoService = ms;
        this.onDataChanged = onDataChanged;
    }

    public void setUsuarioActivo(Usuario usuario) {
        this.usuarioActivo = usuario;
        refrescar();
    }

    public void refrescar() {
        cargarCombosBilleteras();
        cargarHistorial();
    }

    private void aplicarFormatterImporte(TextField campo) {
        UnaryOperator<TextFormatter.Change> filtro = cambio -> {
            String textoNuevo = cambio.getControlNewText();
            if (PATRON_IMPORTE.matcher(textoNuevo).matches()) {
                return cambio;
            }
            return null;
        };
        campo.setTextFormatter(new TextFormatter<>(filtro));
    }

    private void configurarCellFactoryDestino() {
        cmbDestinoTransferencia.setConverter(new StringConverter<>() {
            @Override
            public String toString(String item) {
                return item == null ? "" : item;
            }
            @Override
            public String fromString(String s) {
                return s;
            }
        });

        cmbDestinoTransferencia.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setDisable(false);
                    setStyle("");
                } else if (item.startsWith("\u2500\u2500")) {
                    setText(item);
                    setDisable(true);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #64748B; -fx-font-size: 11px; -fx-background-color: #F1F5F9;");
                } else {
                    setText(item);
                    setDisable(false);
                    setStyle("-fx-font-size: 13px;");
                }
            }
        });
    }

    private void cargarCombosBilleteras() {
        if (usuarioService == null || usuarioActivo == null) return;

        List<Billetera> propias = usuarioService.listarBilleteras(usuarioActivo.getId());
        ObservableList<String> itemsPropias = FXCollections.observableArrayList(
                propias.stream()
                        .map(b -> b.getId() + " — " + b.getNombre() + " ($ " + b.getSaldo().toPlainString() + ")")
                        .toList()
        );
        cmbBilleteraRecarga.setItems(itemsPropias);
        cmbBilleteraRetiro.setItems(FXCollections.observableArrayList(itemsPropias));
        cmbOrigenTransferencia.setItems(FXCollections.observableArrayList(itemsPropias));

        cargarComboDestinoAgrupado(propias);
    }

    private void cargarComboDestinoAgrupado(List<Billetera> propias) {
        ObservableList<String> items = FXCollections.observableArrayList();

        if (propias.size() > 1) {
            items.add("── Mis billeteras ──");
            for (Billetera b : propias) {
                items.add(b.getId() + " — " + b.getNombre() + " ($ " + b.getSaldo().toPlainString() + ")");
            }
        }

        List<Billetera> todas = usuarioService.listarTodasBilleteras();
        Map<String, List<Billetera>> porUsuario = new LinkedHashMap<>();
        for (Billetera b : todas) {
            if (!b.getPropietarioId().equals(usuarioActivo.getId())) {
                porUsuario.computeIfAbsent(b.getPropietarioId(), k -> new ArrayList<>()).add(b);
            }
        }

        for (Map.Entry<String, List<Billetera>> entry : porUsuario.entrySet()) {
            String ownerId = entry.getKey();
            Optional<Usuario> ownerOpt = usuarioService.buscarUsuario(ownerId);
            String ownerLabel = ownerOpt.map(u -> u.getNombre() + " (" + u.getId() + ")").orElse(ownerId);
            items.add("── " + ownerLabel + " ──");
            for (Billetera b : entry.getValue()) {
                items.add(b.getId() + " — " + b.getNombre());
            }
        }

        cmbDestinoTransferencia.setItems(items);
    }

    private void cargarHistorial() {
        if (movimientoService == null || usuarioActivo == null) {
            listaTransacciones.clear();
            lblRegistrosTx.setText("0 transacciones");
            return;
        }
        List<Transaccion> txs = movimientoService.listarTransacciones(usuarioActivo.getId());
        listaTransacciones.setAll(txs);
        lblRegistrosTx.setText(txs.size() + " transacciones");
    }

    private String extraerIdBilletera(String textoCombo) {
        if (textoCombo == null || textoCombo.isBlank() || textoCombo.startsWith("──")) return null;
        return textoCombo.split(" — ")[0].trim();
    }

    private String descripcionBilletera(String billeteraId) {
        if (billeteraId == null || billeteraId.isBlank()) return "—";
        try {
            Billetera b = usuarioService.obtenerBilletera(billeteraId);
            return b.getNombre() + " (" + b.getId() + ")";
        } catch (Exception e) {
            return billeteraId;
        }
    }

    private String descripcionCompletaBilletera(String billeteraId) {
        if (billeteraId == null || billeteraId.isBlank()) return "—";
        try {
            Billetera b = usuarioService.obtenerBilletera(billeteraId);
            Optional<Usuario> ownerOpt = usuarioService.buscarUsuario(b.getPropietarioId());
            String ownerName = ownerOpt.map(Usuario::getNombre).orElse(b.getPropietarioId());
            return b.getNombre() + " (" + b.getId() + ") - " + ownerName;
        } catch (Exception e) {
            return billeteraId;
        }
    }

    private String formatearTipo(String tipo) {
        return switch (tipo) {
            case "RECARGA" -> "Recarga";
            case "RETIRO" -> "Retiro";
            case "TRANSFERENCIA" -> "Transferencia";
            default -> tipo;
        };
    }

    @FXML private void onTabRecarga()       { mostrarTab("recarga"); }
    @FXML private void onTabRetiro()        { mostrarTab("retiro"); }
    @FXML private void onTabTransferencia() { mostrarTab("transferencia"); }

    private void mostrarTab(String tab) {
        paneRecarga.setVisible("recarga".equals(tab));
        paneRecarga.setManaged("recarga".equals(tab));
        paneRetiro.setVisible("retiro".equals(tab));
        paneRetiro.setManaged("retiro".equals(tab));
        paneTransferencia.setVisible("transferencia".equals(tab));
        paneTransferencia.setManaged("transferencia".equals(tab));

        btnTabRecarga.getStyleClass().remove("tab-button-active");
        btnTabRetiro.getStyleClass().remove("tab-button-active");
        btnTabTransferencia.getStyleClass().remove("tab-button-active");

        switch (tab) {
            case "recarga"       -> btnTabRecarga.getStyleClass().add("tab-button-active");
            case "retiro"        -> btnTabRetiro.getStyleClass().add("tab-button-active");
            case "transferencia" -> btnTabTransferencia.getStyleClass().add("tab-button-active");
        }
    }

    @FXML
    private void onRecargar() {
        if (usuarioActivo == null) {
            PrincipalController.mostrarAlerta("Seleccione un usuario.");
            return;
        }
        String billeteraId = extraerIdBilletera(cmbBilleteraRecarga.getValue());
        if (billeteraId == null) {
            PrincipalController.mostrarAlerta("Seleccione una billetera.");
            return;
        }
        String importe = txtImporteRecarga.getText();
        if (importe == null || importe.isBlank()) {
            PrincipalController.mostrarAlerta("Ingrese un importe válido.");
            return;
        }
        try {
            Transaccion tx = movimientoService.recargar(
                    usuarioActivo.getId(), billeteraId, importe);
            Billetera b = usuarioService.obtenerBilletera(billeteraId);
            PrincipalController.mostrarExito(
                    "Recarga exitosa\n\n"
                    + "Billetera: " + b.getNombre() + " (" + b.getId() + ")\n"
                    + "Monto recargado: $ " + tx.getImporte().toPlainString() + "\n"
                    + "Nuevo saldo: $ " + b.getSaldo().toPlainString() + "\n"
                    + "Transacción: " + tx.getId());
            txtImporteRecarga.clear();
            refrescar();
            if (onDataChanged != null) onDataChanged.run();
        } catch (ReglaNegocioException e) {
            PrincipalController.mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onRetirar() {
        if (usuarioActivo == null) {
            PrincipalController.mostrarAlerta("Seleccione un usuario.");
            return;
        }
        String billeteraId = extraerIdBilletera(cmbBilleteraRetiro.getValue());
        if (billeteraId == null) {
            PrincipalController.mostrarAlerta("Seleccione una billetera.");
            return;
        }
        String importe = txtImporteRetiro.getText();
        if (importe == null || importe.isBlank()) {
            PrincipalController.mostrarAlerta("Ingrese un importe válido.");
            return;
        }
        try {
            Transaccion tx = movimientoService.retirar(
                    usuarioActivo.getId(), billeteraId, importe);
            Billetera b = usuarioService.obtenerBilletera(billeteraId);
            PrincipalController.mostrarExito(
                    "Retiro exitoso\n\n"
                    + "Billetera: " + b.getNombre() + " (" + b.getId() + ")\n"
                    + "Monto retirado: $ " + tx.getImporte().toPlainString() + "\n"
                    + "Nuevo saldo: $ " + b.getSaldo().toPlainString() + "\n"
                    + "Transacción: " + tx.getId());
            txtImporteRetiro.clear();
            refrescar();
            if (onDataChanged != null) onDataChanged.run();
        } catch (ReglaNegocioException e) {
            PrincipalController.mostrarError(e.getMessage());
        }
    }

    @FXML
    private void onTransferir() {
        if (usuarioActivo == null) {
            PrincipalController.mostrarAlerta("Seleccione un usuario.");
            return;
        }
        String origenId = extraerIdBilletera(cmbOrigenTransferencia.getValue());
        String destinoId = extraerIdBilletera(cmbDestinoTransferencia.getValue());
        if (origenId == null || destinoId == null) {
            PrincipalController.mostrarAlerta("Seleccione billetera origen y destino.");
            return;
        }
        String importe = txtImporteTransferencia.getText();
        if (importe == null || importe.isBlank()) {
            PrincipalController.mostrarAlerta("Ingrese un importe válido.");
            return;
        }

        String descOrigen = descripcionCompletaBilletera(origenId);
        String descDestino = descripcionCompletaBilletera(destinoId);

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar Transferencia");
        confirm.setHeaderText("Revise los datos de la transferencia");
        confirm.setContentText(
                "Desde:   " + descOrigen
                + "\nHacia:   " + descDestino
                + "\nMonto:  $ " + importe
                + "\n\n¿Desea confirmar esta operación?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Transaccion tx = movimientoService.transferir(
                        usuarioActivo.getId(), origenId, destinoId, importe);
                Billetera bOrigen = usuarioService.obtenerBilletera(origenId);
                PrincipalController.mostrarExito(
                        "Transferencia exitosa\n\n"
                        + "Desde: " + descOrigen + "\n"
                        + "Hacia: " + descDestino + "\n"
                        + "Monto transferido: $ " + tx.getImporte().toPlainString() + "\n"
                        + "Saldo restante: $ " + bOrigen.getSaldo().toPlainString() + "\n"
                        + "Transacción: " + tx.getId());
                txtImporteTransferencia.clear();
                refrescar();
                if (onDataChanged != null) onDataChanged.run();
            } catch (ReglaNegocioException e) {
                PrincipalController.mostrarError(e.getMessage());
            }
        }
    }
}
