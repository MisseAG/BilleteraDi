package co.edu.uniquindio.billeteradi.model.entity;

import co.edu.uniquindio.billeteradi.model.enums.EstadoProgramacion;
import co.edu.uniquindio.billeteradi.model.enums.TipoTransaccion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class OperacionProgramada {

    private final String id;
    private final String usuarioId;
    private final String origenId;
    private final String destinoId;
    private final TipoTransaccion tipo;
    private final BigDecimal importe;
    private final LocalDateTime fechaEjecucion;
    private final long consecutivo; // Usado para desempate estable bo
    private EstadoProgramacion estado;
    private String transaccionGeneradaId;

    public OperacionProgramada(String id, String usuarioId, String origenId, String destinoId,
                               TipoTransaccion tipo, BigDecimal importe, LocalDateTime fechaEjecucion,
                               long consecutivo, EstadoProgramacion estado) {
        this.id = Objects.requireNonNull(id, "id");
        this.usuarioId = Objects.requireNonNull(usuarioId, "usuarioId");
        this.origenId = origenId;
        this.destinoId = destinoId;
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.importe = Objects.requireNonNull(importe, "importe");
        this.fechaEjecucion = Objects.requireNonNull(fechaEjecucion, "fechaEjecucion");
        this.consecutivo = consecutivo;
        this.estado = Objects.requireNonNull(estado, "estado");
    }

    public String getId() { return id; }
    public String getUsuarioId() { return usuarioId; }
    public String getOrigenId() { return origenId; }
    public String getDestinoId() { return destinoId; }
    public TipoTransaccion getTipo() { return tipo; }
    public BigDecimal getImporte() { return importe; }
    public LocalDateTime getFechaEjecucion() { return fechaEjecucion; }
    public long getConsecutivo() { return consecutivo; }
    public EstadoProgramacion getEstado() { return estado; }
    public String getTransaccionGeneradaId() { return transaccionGeneradaId; }

    public void setEstado(EstadoProgramacion estado) {
        this.estado = Objects.requireNonNull(estado, "estado");
    }

    public void setTransaccionGeneradaId(String transaccionGeneradaId) {
        this.transaccionGeneradaId = transaccionGeneradaId;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof OperacionProgramada that && id.equals(that.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "OperacionProgramada{" + id + ", " + tipo + ", importe=" + importe + ", estado=" + estado +
                ", fecha=" + fechaEjecucion + "}";
    }
}
