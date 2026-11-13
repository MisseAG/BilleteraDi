package co.edu.uniquindio.billeteradi.model.entity;

import co.edu.uniquindio.billeteradi.model.enums.NivelUsuario;

import java.util.Objects;

/**
 * Entidad que representa a un usuario registrado en la plataforma.
 */
public class Usuario {

    private final String id;
    private String nombre;
    private boolean activo;
    private long puntosAcumuladosNetos;
    private long puntosDisponibles;
    private NivelUsuario nivel;

    public Usuario(String id, String nombre) {
        this.id = Objects.requireNonNull(id, "id");
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.activo = true;
        this.puntosAcumuladosNetos = 0;
        this.puntosDisponibles = 0;
        this.nivel = NivelUsuario.BRONCE;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public boolean isActivo() { return activo; }
    public long getPuntosAcumuladosNetos() { return puntosAcumuladosNetos; }
    public long getPuntosDisponibles() { return puntosDisponibles; }
    public NivelUsuario getNivel() { return nivel; }

    public void setNombre(String nombre) {
        this.nombre = Objects.requireNonNull(nombre, "nombre");
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public void acreditarPuntos(long puntos) {
        if (puntos < 0) {
            throw new IllegalArgumentException("Los puntos a acreditar no pueden ser negativos");
        }
        long nuevosAcumulados = Math.addExact(puntosAcumuladosNetos, puntos);
        long nuevosDisponibles = Math.addExact(puntosDisponibles, puntos);
        this.puntosAcumuladosNetos = nuevosAcumulados;
        this.puntosDisponibles = nuevosDisponibles;
    }

    public void setNivel(NivelUsuario nivel) {
        this.nivel = Objects.requireNonNull(nivel, "nivel");
    }

    public void setPuntosDisponibles(long puntos) {
        if (puntos < 0) {
            throw new IllegalArgumentException("Puntos disponibles no pueden ser negativos");
        }
        this.puntosDisponibles = puntos;
    }

    public void setPuntosAcumuladosNetos(long puntos) {
        if (puntos < 0) {
            throw new IllegalArgumentException("Puntos acumulados no pueden ser negativos");
        }
        this.puntosAcumuladosNetos = puntos;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Usuario u && id.equals(u.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Usuario{" + id + ", " + nombre + (activo ? "" : ", inactivo") + "}";
    }
}
