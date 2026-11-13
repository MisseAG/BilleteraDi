package co.edu.uniquindio.billeteradi.model.entity;

import co.edu.uniquindio.billeteradi.model.enums.EstadoTransaccion;
import co.edu.uniquindio.billeteradi.model.enums.TipoTransaccion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad que registra un movimiento financiero en la plataforma.
 */
public class Transaccion {

    private final String id;
    private final LocalDateTime fecha;
    private final TipoTransaccion tipo;
    private final BigDecimal importe;
    private final String origenId;
    private final String destinoId;
    private EstadoTransaccion estado;
    private final long puntosGenerados;
    private final String transaccionOriginalId;

    public Transaccion(String id, LocalDateTime fecha, TipoTransaccion tipo, BigDecimal importe,
                       String origenId, String destinoId, EstadoTransaccion estado,
                       long puntosGenerados, String transaccionOriginalId) {
        this.id = Objects.requireNonNull(id, "id");
        this.fecha = Objects.requireNonNull(fecha, "fecha");
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.importe = Objects.requireNonNull(importe, "importe");
        this.origenId = origenId;
        this.destinoId = destinoId;
        this.estado = Objects.requireNonNull(estado, "estado");
        this.puntosGenerados = puntosGenerados;
        this.transaccionOriginalId = transaccionOriginalId;
    }

    public String getId() { return id; }
    public LocalDateTime getFecha() { return fecha; }
    public TipoTransaccion getTipo() { return tipo; }
    public BigDecimal getImporte() { return importe; }
    public String getOrigenId() { return origenId; }
    public String getDestinoId() { return destinoId; }
    public EstadoTransaccion getEstado() { return estado; }
    public long getPuntosGenerados() { return puntosGenerados; }
    public String getTransaccionOriginalId() { return transaccionOriginalId; }

    public void setEstado(EstadoTransaccion estado) {
        this.estado = Objects.requireNonNull(estado, "estado");
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Transaccion t && id.equals(t.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Transaccion{" + id + ", " + tipo + ", " + importe + ", " + estado + "}";
    }
}
