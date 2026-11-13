package co.edu.uniquindio.billeteradi.model.repository;

import co.edu.uniquindio.billeteradi.model.entity.Billetera;
import co.edu.uniquindio.billeteradi.model.entity.Transaccion;
import co.edu.uniquindio.billeteradi.model.entity.Usuario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Almacenamiento en memoria de usuarios y billeteras (H2).
 *
 * Estructuras (sección 3 del documento):
 *  - usuarios:             mapa hash id -> Usuario            (buscar por id en O(1))
 *  - billeteras:           mapa hash id -> Billetera          (buscar por código en O(1))
 *  - billeterasPorUsuario: mapa hash idUsuario -> ids de sus billeteras
 *                          (listar las k billeteras de un usuario en O(k), sin
 *                          recorrer todas las billeteras)
 *
 * Se usa LinkedHashMap, que es un HashMap con la misma complejidad O(1) y,
 * además, conserva el orden de inserción: las tablas de la interfaz muestran
 * los registros en el orden en que se crearon, sin tener que ordenar.
 *
 * Esta clase NO valida reglas de negocio (eso es de los servicios). Solo
 * garantiza integridad estructural: no hay ids repetidos y no existen
 * billeteras huérfanas.
 */
public class RepositorioMemoria {

    private final Map<String, Usuario> usuarios = new LinkedHashMap<>();
    private final Map<String, Billetera> billeteras = new LinkedHashMap<>();
    private final Map<String, List<String>> billeterasPorUsuario = new LinkedHashMap<>();
    private long consecutivoBilletera = 0;

    // ---------------- usuarios ----------------

    /** @return true si se agregó; false si ya existía un usuario con ese id (no se modifica nada). */
    public boolean agregarUsuario(Usuario usuario) {
        return usuarios.putIfAbsent(usuario.getId(), usuario) == null;
    }

    public Optional<Usuario> buscarUsuario(String id) {
        return Optional.ofNullable(usuarios.get(id));
    }

    public boolean existeUsuario(String id) {
        return usuarios.containsKey(id);
    }

    /** Copia de solo lectura: quien la reciba no puede alterar el repositorio. */
    public List<Usuario> listarUsuarios() {
        return Collections.unmodifiableList(new ArrayList<>(usuarios.values()));
    }

    public int totalUsuarios() {
        return usuarios.size();
    }

    // ---------------- billeteras ----------------

    /**
     * Genera el siguiente código de billetera (BIL-0001, BIL-0002...).
     * Solo debe llamarse cuando ya se validó todo, para no consumir códigos
     * en operaciones rechazadas.
     */
    public String siguienteCodigoBilletera() {
        consecutivoBilletera++;
        return String.format("BIL-%04d", consecutivoBilletera);
    }

    /**
     * @return true si se agregó; false si ya existía una billetera con ese código.
     * @throws IllegalArgumentException si el propietario no existe (evita billeteras huérfanas).
     */
    public boolean agregarBilletera(Billetera billetera) {
        if (!usuarios.containsKey(billetera.getPropietarioId())) {
            throw new IllegalArgumentException(
                    "El propietario " + billetera.getPropietarioId() + " no existe en el repositorio");
        }
        if (billeteras.putIfAbsent(billetera.getId(), billetera) != null) {
            return false;
        }
        billeterasPorUsuario
                .computeIfAbsent(billetera.getPropietarioId(), k -> new ArrayList<>())
                .add(billetera.getId());
        return true;
    }

    public Optional<Billetera> buscarBilletera(String id) {
        return Optional.ofNullable(billeteras.get(id));
    }

    /** Billeteras del usuario en orden de creación. Lista vacía si no tiene o no existe. O(k). */
    public List<Billetera> listarBilleterasDeUsuario(String usuarioId) {
        List<String> ids = billeterasPorUsuario.getOrDefault(usuarioId, List.of());
        List<Billetera> resultado = new ArrayList<>(ids.size());
        for (String id : ids) {
            resultado.add(billeteras.get(id));
        }
        return Collections.unmodifiableList(resultado);
    }

    public int totalBilleteras() {
        return billeteras.size();
    }

    /*
    private final Map<String, Transaccion> transacciones = new LinkedHashMap<>();
    private final Map<String, List<String>> historialesUsuario = new LinkedHashMap<>();
    private final Map<String, List<String>> historialesBilletera = new LinkedHashMap<>();
    private long consecutivoTransaccion = 0;

    public String siguienteCodigoTransaccion() {
        consecutivoTransaccion++;
        return String.format("TX-%04d", consecutivoTransaccion);
    }

    public void registrarTransaccion(Transaccion tx, String usuarioId) {
        transacciones.put(tx.getId(), tx);
        if (usuarioId != null) {
            historialesUsuario.computeIfAbsent(usuarioId, k -> new ArrayList<>()).add(tx.getId());
        }
        if (tx.getOrigenId() != null) {
            historialesBilletera.computeIfAbsent(tx.getOrigenId(), k -> new ArrayList<>()).add(tx.getId());
        }
        if (tx.getDestinoId() != null && !tx.getDestinoId().equals(tx.getOrigenId())) {
            historialesBilletera.computeIfAbsent(tx.getDestinoId(), k -> new ArrayList<>()).add(tx.getId());
        }
    }

    public Optional<Transaccion> buscarTransaccion(String id) {
        return Optional.ofNullable(transacciones.get(id));
    }*/
}
