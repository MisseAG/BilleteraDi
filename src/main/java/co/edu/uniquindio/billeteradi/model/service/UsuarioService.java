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
 * Servicio de lógica de negocio para usuarios y billeteras (RF01 y RF02).
 */
public class UsuarioService {

    private final RepositorioMemoria repositorio;

    public UsuarioService(RepositorioMemoria repositorio) {
        this.repositorio = repositorio;
    }

    // ---------------- Usuarios ----------------

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

    public Usuario editar(String id, String nuevoNombre) {
        Usuario usuario = obtenerUsuario(id);
        String nombreLimpio = exigirTexto(nuevoNombre, "El nombre");
        usuario.setNombre(nombreLimpio);
        return usuario;
    }

    public Usuario desactivar(String id) {
        Usuario usuario = obtenerUsuario(id);
        if (!usuario.isActivo()) {
            throw new ReglaNegocioException("El usuario '" + usuario.getId() + "' ya está inactivo");
        }
        usuario.setActivo(false);
        return usuario;
    }

    public Usuario reactivar(String id) {
        Usuario usuario = obtenerUsuario(id);
        if (usuario.isActivo()) {
            throw new ReglaNegocioException("El usuario '" + usuario.getId() + "' ya está activo");
        }
        usuario.setActivo(true);
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

    // ---------------- Billeteras ----------------

    public Billetera crearBilletera(String usuarioId, String nombre, TipoBilletera tipo) {
        Usuario propietario = obtenerUsuario(usuarioId);
        if (!propietario.isActivo()) {
            throw new ReglaNegocioException("El usuario '" + propietario.getId() + "' está inactivo y no puede crear billeteras");
        }
        String nombreLimpio = exigirTexto(nombre, "El nombre de la billetera");
        if (tipo == null) {
            throw new ReglaNegocioException("Debe indicar el tipo de billetera");
        }
        Billetera billetera = new Billetera(repositorio.siguienteCodigoBilletera(), propietario.getId(), nombreLimpio, tipo);
        repositorio.agregarBilletera(billetera);
        return billetera;
    }

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

    public List<Billetera> listarBilleteras(String usuarioId) {
        Usuario usuario = obtenerUsuario(usuarioId);
        return repositorio.listarBilleterasDeUsuario(usuario.getId());
    }

    public List<Billetera> listarTodasBilleteras() {
        return repositorio.listarTodasBilleteras();
    }

    // ---------------- Utilidades ----------------

    private static String exigirTexto(String valor, String etiqueta) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new ReglaNegocioException(etiqueta + " no puede estar vacío");
        }
        return valor.trim();
    }
}
