package co.edu.uniquindio.billeteradi.model.entity;

import java.time.LocalDateTime;
import java.util.Objects;

public class Notificacion {
    private final String id;
    private final String usuarioId;
    private final String mensaje;
    private final LocalDateTime fecha;
    private boolean leida;

    public Notificacion(String id, String usuarioId, String mensaje, LocalDateTime fecha) {
        this.id = Objects.requireNonNull(id, "id");
        this.usuarioId = Objects.requireNonNull(usuarioId, "usuarioId");
        this.mensaje = Objects.requireNonNull(mensaje, "mensaje");
        this.fecha = Objects.requireNonNull(fecha, "fecha");
        this.leida = false;
    }

    public String getId() { return id; }
    public String getUserid() { return usuarioId; }
    public String getMensaje() { return mensaje; }
    public LocalDateTime getFecha() { return fecha; }
    public boolean isLeida() { return leida; }

    public void setLeida(boolean leida) { this.leida = leida; }

    @Override public boolean equals(Object o) {
        return this == o || (o instanceof Notificacion n && id.equals(n.id));
    }
    @Override public int hashCode() { return id.hashCode(); }
    @Override public String toString() {
        return "Notificacion{" + id + ", " + (leida ? "LEÍDA" : "NUEVA") + ", " + mensaje + "}";
    }
}
