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
 * Repositorio en memoria para usuarios, billeteras y transacciones.
 * Utiliza LinkedHashMap para garantizar búsquedas en O(1) y orden de inserción.
 */
public class RepositorioMemoria {

    private final Map<String, Usuario> usuarios = new LinkedHashMap<>();
    private final Map<String, Billetera> billeteras = new LinkedHashMap<>();
    private final Map<String, List<String>> billeterasPorUsuario = new LinkedHashMap<>();
    private final Map<String, Transaccion> transacciones = new LinkedHashMap<>();
    private final Map<String, List<String>> historialesUsuario = new LinkedHashMap<>();
    private final Map<String, List<String>> historialesBilletera = new LinkedHashMap<>();

    private long consecutivoBilletera = 0;
    private long consecutivoTransaccion = 0;

    // ---------------- Usuarios ----------------

    public boolean agregarUsuario(Usuario usuario) {
        return usuarios.putIfAbsent(usuario.getId(), usuario) == null;
    }

    public Optional<Usuario> buscarUsuario(String id) {
        return Optional.ofNullable(usuarios.get(id));
    }

    public boolean existeUsuario(String id) {
        return usuarios.containsKey(id);
    }

    public List<Usuario> listarUsuarios() {
        return Collections.unmodifiableList(new ArrayList<>(usuarios.values()));
    }

    public int totalUsuarios() {
        return usuarios.size();
    }

    // ---------------- Billeteras ----------------

    public String siguienteCodigoBilletera() {
        consecutivoBilletera++;
        return String.format("BIL-%04d", consecutivoBilletera);
    }

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

    public boolean agregarBilleteraDirecto(Billetera billetera) {
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

    public List<Billetera> listarBilleterasDeUsuario(String usuarioId) {
        List<String> ids = billeterasPorUsuario.getOrDefault(usuarioId, List.of());
        List<Billetera> resultado = new ArrayList<>(ids.size());
        for (String id : ids) {
            resultado.add(billeteras.get(id));
        }
        return Collections.unmodifiableList(resultado);
    }

    public List<Billetera> listarTodasBilleteras() {
        return Collections.unmodifiableList(new ArrayList<>(billeteras.values()));
    }

    public int totalBilleteras() {
        return billeteras.size();
    }

    // ---------------- Transacciones ----------------

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
    }

    public List<Transaccion> listarTransaccionesDeUsuario(String usuarioId) {
        List<String> txIds = historialesUsuario.getOrDefault(usuarioId, List.of());
        List<Transaccion> resultado = new ArrayList<>(txIds.size());
        for (String txId : txIds) {
            Transaccion tx = transacciones.get(txId);
            if (tx != null) resultado.add(tx);
        }
        return Collections.unmodifiableList(resultado);
    }

    public List<Transaccion> listarTodasTransacciones() {
        return Collections.unmodifiableList(new ArrayList<>(transacciones.values()));
    }

    // ---------------- Consecutivos ----------------

    public long getConsecutivoBilletera() {
        return consecutivoBilletera;
    }

    public void setConsecutivoBilletera(long valor) {
        this.consecutivoBilletera = valor;
    }

    public long getConsecutivoTransaccion() {
        return consecutivoTransaccion;
    }

    public void setConsecutivoTransaccion(long valor) {
        this.consecutivoTransaccion = valor;
    }
}
