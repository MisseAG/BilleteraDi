package co.edu.uniquindio.billeteradi.model.service;

import co.edu.uniquindio.billeteradi.model.entity.Billetera;
import co.edu.uniquindio.billeteradi.model.entity.Usuario;
import co.edu.uniquindio.billeteradi.model.enums.EstadoBilletera;
import co.edu.uniquindio.billeteradi.model.enums.TipoBilletera;
import co.edu.uniquindio.billeteradi.model.exception.ReglaNegocioException;
import co.edu.uniquindio.billeteradi.model.repository.RepositorioMemoria;

import java.util.List;
import java.util.Optional;

/**
 * Casos de uso de usuarios y billeteras (RF01 y RF02).
 *
 * Patrón de todos los métodos que escriben: VALIDAR primero y modificar al
 * final. Así un rechazo nunca deja datos a medias (ver 3.5.1 del documento).
 * Los identificadores los escribe el operario (modelo de ventanilla), por eso
 * se normalizan con trim() y se rechazan si quedan vacíos.
 */
public class UsuarioService {

    private final RepositorioMemoria repositorio;

    public UsuarioService(RepositorioMemoria repositorio) {
        this.repositorio = repositorio;
    }

    // ---------------- usuarios ----------------

    /** Registra un usuario activo, con 0 puntos y nivel Bronce. */
    public Usuario registrar(String id, String nombre) {
        String idLimpio = exigirTexto(id, "El identificador");
        String nombreLimpio = exigirTexto(nombre, "El nombre");
        if (repositorio.existeUsuario(idLimpio)) {
            throw new ReglaNegocioException("Ya existe un usuario con el identificador '" + idLimpio + "'");
        }
        Usuario usuario = new Usuario(idLimpio, nombreLimpio);
        repositorio.agregarUsuario(usuario);
        return usuario;
    }

    /** Cambia el nombre. El identificador es inmutable, por eso no se recibe uno nuevo. */
    public Usuario editar(String id, String nuevoNombre) {
        Usuario usuario = obtenerUsuario(id);
        String nombreLimpio = exigirTexto(nuevoNombre, "El nombre");
        usuario.setNombre(nombreLimpio);
        return usuario;
    }

    /**
     * Desactiva al usuario sin borrar nada: su registro, sus billeteras y (desde H3)
     * su historial siguen en el repositorio y se pueden consultar.
     */
    public Usuario desactivar(String id) {
        Usuario usuario = obtenerUsuario(id);
        if (!usuario.isActivo()) {
            throw new ReglaNegocioException("El usuario '" + usuario.getId() + "' ya está inactivo");
        }
        usuario.setActivo(false);
        return usuario;
    }

    public Optional<Usuario> buscarUsuario(String id) {
        return id == null ? Optional.empty() : repositorio.buscarUsuario(id.trim());
    }

    public Usuario obtenerUsuario(String id) {
        return buscarUsuario(id).orElseThrow(() ->
                new ReglaNegocioException("El usuario '" + (id == null ? "" : id.trim()) + "' no existe"));
    }

    public List<Usuario> listarUsuarios() {
        return repositorio.listarUsuarios();
    }

    // ---------------- billeteras ----------------

    /** Crea una billetera ACTIVA con saldo 0.00 para un usuario activo. */
    public Billetera crearBilletera(String usuarioId, String nombre, TipoBilletera tipo) {
        Usuario propietario = obtenerUsuario(usuarioId);
        if (!propietario.isActivo()) {
            throw new ReglaNegocioException("El usuario '" + propietario.getId() + "' está inactivo y no puede crear billeteras");
        }
        String nombreLimpio = exigirTexto(nombre, "El nombre de la billetera");
        if (tipo == null) {
            throw new ReglaNegocioException("Debe indicar el tipo de billetera");
        }
        // Todo validado: recién ahora se consume un código.
        Billetera billetera = new Billetera(repositorio.siguienteCodigoBilletera(), propietario.getId(), nombreLimpio, tipo);
        repositorio.agregarBilletera(billetera);
        return billetera;
    }

    /** Bloquea o reactiva una billetera. Pedir el mismo estado que ya tiene se rechaza. */
    public Billetera cambiarEstadoBilletera(String billeteraId, EstadoBilletera nuevoEstado) {
        Billetera billetera = obtenerBilletera(billeteraId);
        if (nuevoEstado == null) {
            throw new ReglaNegocioException("Debe indicar el nuevo estado de la billetera");
        }
        if (billetera.getEstado() == nuevoEstado) {
            throw new ReglaNegocioException("La billetera '" + billetera.getId() + "' ya está " + nuevoEstado);
        }
        billetera.setEstado(nuevoEstado);
        return billetera;
    }

    public Optional<Billetera> buscarBilletera(String id) {
        return id == null ? Optional.empty() : repositorio.buscarBilletera(id.trim());
    }

    public Billetera obtenerBilletera(String id) {
        return buscarBilletera(id).orElseThrow(() ->
                new ReglaNegocioException("La billetera '" + (id == null ? "" : id.trim()) + "' no existe"));
    }

    /** Billeteras del usuario en orden de creación (O(k)). Falla si el usuario no existe. */
    public List<Billetera> listarBilleteras(String usuarioId) {
        Usuario usuario = obtenerUsuario(usuarioId);
        return repositorio.listarBilleterasDeUsuario(usuario.getId());
    }

    // ---------------- utilidades ----------------

    private static String exigirTexto(String valor, String etiqueta) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new ReglaNegocioException(etiqueta + " no puede estar vacío");
        }
        return valor.trim();
    }
}
