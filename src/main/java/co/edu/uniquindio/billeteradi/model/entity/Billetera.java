package co.edu.uniquindio.billeteradi.model.entity;

import co.edu.uniquindio.billeteradi.model.enums.EstadoBilletera;
import co.edu.uniquindio.billeteradi.model.enums.TipoBilletera;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Billetera de un único propietario. Nace con saldo cero y estado ACTIVA.
 *
 * El saldo es BigDecimal con dos decimales: nunca float ni double para dinero.
 * No hay setSaldo todavía: en H3 solo MovimientoService podrá modificarlo,
 * para que exista un único punto de entrada que mantenga consistentes
 * historiales, puntos e índices.
 */
public class Billetera {

    private final String id;
    private final String propietarioId;
    private String nombre;
    private final TipoBilletera tipo;
    private final BigDecimal saldo;
    private EstadoBilletera estado;

    public Billetera(String id, String propietarioId, String nombre, TipoBilletera tipo) {
        this.id = Objects.requireNonNull(id, "id");
        this.propietarioId = Objects.requireNonNull(propietarioId, "propietarioId");
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.saldo = new BigDecimal("0.00");
        this.estado = EstadoBilletera.ACTIVA;
    }

    public String getId() { return id; }
    public String getPropietarioId() { return propietarioId; }
    public String getNombre() { return nombre; }
    public TipoBilletera getTipo() { return tipo; }
    public BigDecimal getSaldo() { return saldo; }
    public EstadoBilletera getEstado() { return estado; }

    public void setNombre(String nombre) { this.nombre = Objects.requireNonNull(nombre, "nombre"); }
    public void setEstado(EstadoBilletera estado) { this.estado = Objects.requireNonNull(estado, "estado"); }

    @Override public boolean equals(Object o) {
        return this == o || (o instanceof Billetera b && id.equals(b.id));
    }
    @Override public int hashCode() { return id.hashCode(); }
    @Override public String toString() { return "Billetera{" + id + ", " + nombre + ", " + tipo + ", " + saldo + ", " + estado + "}"; }
}
