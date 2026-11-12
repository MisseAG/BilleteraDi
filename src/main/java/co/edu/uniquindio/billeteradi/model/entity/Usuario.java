package co.edu.uniquindio.billeteradi.model.entity;

import co.edu.uniquindio.billeteradi.model.enums.NivelUsuario;

import java.util.Objects;

/**
 * Usuario de la plataforma. El identificador es inmutable (regla de negocio);
 * todo lo demás puede cambiar.
 *
 * Los campos de puntos y nivel quedan en su valor inicial en H2. Sus
 * mutadores los agrega RecompensaService en H3, para que ningún otro código
 * pueda tocarlos antes de que exista la regla que los gobierna.
 */
public class Usuario {

    private final String id;
    private String nombre;
    private boolean activo;
    private final long puntosAcumuladosNetos;   // determina el nivel
    private final long puntosDisponibles;       // sirve para canjear beneficios
    private final NivelUsuario nivel;

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

    public void setNombre(String nombre) { this.nombre = Objects.requireNonNull(nombre, "nombre"); }
    public void setActivo(boolean activo) { this.activo = activo; }

    // La igualdad usa solo el id: es el único campo que nunca cambia.
    @Override public boolean equals(Object o) {
        return this == o || (o instanceof Usuario u && id.equals(u.id));
    }
    @Override public int hashCode() { return id.hashCode(); }
    @Override public String toString() { return "Usuario{" + id + ", " + nombre + (activo ? "" : ", inactivo") + "}"; }
}
