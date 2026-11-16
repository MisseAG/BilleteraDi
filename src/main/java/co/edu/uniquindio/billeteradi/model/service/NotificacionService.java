package co.edu.uniquindio.billeteradi.model.service;

import co.edu.uniquindio.billeteradi.model.entity.Billetera;
import co.edu.uniquindio.billeteradi.model.entity.Notificacion;
import co.edu.uniquindio.billeteradi.model.enums.NivelUsuario;
import co.edu.uniquindio.billeteradi.model.enums.TipoTransaccion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class NotificacionService {

    public NotificacionService() {
    }
    private static final int TOPE_RECIENTES = 20;
    public static final BigDecimal UMBRAL_SALDO_BAJO_DEFECTO = new BigDecimal("50.00");

    private final Queue<Notificacion> colaDespacho = new ArrayDeque<>();
    private final Map<String, ArrayDeque<Notificacion>> notificacionesPorUsuario = new HashMap<>();
    private long consecutivoNotificacion = 0;
    private BigDecimal umbralSaldoBajo = UMBRAL_SALDO_BAJO_DEFECTO;

    public Notificacion encolar(String usuarioId, String mensaje) {
        consecutivoNotificacion++;
        String id = String.format("NOTIF-%04d", consecutivoNotificacion);
        Notificacion notificacion = new Notificacion(id, usuarioId, mensaje, LocalDateTime.now());

        colaDespacho.offer(notificacion);

        ArrayDeque<Notificacion> dequeUsuario = notificacionesPorUsuario.computeIfAbsent(
                usuarioId, k -> new ArrayDeque<>());
        if (dequeUsuario.size() >= TOPE_RECIENTES) {
            dequeUsuario.pollFirst(); // Descarta la más antigua al superar el tope N=20
        }
        dequeUsuario.addLast(notificacion);
        return notificacion;
    }

    public Notificacion notificar(String usuarioId, String mensaje) {
        return encolar(usuarioId, mensaje);
    }

    // Extrae y despacha la siguiente notificación pendiente en orden FIFO
    public Optional<Notificacion> despachar() {
        return Optional.ofNullable(colaDespacho.poll());
    }

    // Despacha todas las notificaciones encoladas actualmente en orden de llegada
    public List<Notificacion> despacharTodas() {
        List<Notificacion> despachadas = new ArrayList<>(colaDespacho.size());
        Notificacion n;
        while ((n = colaDespacho.poll()) != null) {
            despachadas.add(n);
        }
        return despachadas;
    }

    // ---------------- Eventos de negocio RF12 ----------------

    // Rechazo
    public Notificacion notificarRechazo(String usuarioId, TipoTransaccion tipo,
                                         BigDecimal importe, String motivo) {
        String mensaje = "Operación rechazada (" + tipo.name() + " por $ "
                + importe.toPlainString() + "): " + motivo;
        return encolar(usuarioId, mensaje);
    }

    // Saldo Bajo
    public Optional<Notificacion> verificarYNotificarSaldoBajo(String usuarioId, Billetera billetera) {
        if (billetera == null || billetera.getSaldo().compareTo(umbralSaldoBajo) > 0) {
            return Optional.empty();
        }
        String mensaje = "Saldo bajo en billetera '" + billetera.getNombre()
                + "' (" + billetera.getId() + "): $ " + billetera.getSaldo().toPlainString();
        return Optional.of(encolar(usuarioId, mensaje));
    }

    // Cambio de nivel
    public Notificacion notificarCambioNivel(String usuarioId, NivelUsuario anterior,
                                             NivelUsuario nuevo, long puntosAcumuladosNetos) {
        String mensaje = "Ascendiste de nivel " + anterior.name()
                + " a " + nuevo.name() + " (" + puntosAcumuladosNetos + " pts acumulados).";
        return encolar(usuarioId, mensaje);
    }

    // ---------------- Consultas para la interfaz ----------------

    //  Devuelve las notificaciones recientes del usuario

    public List<Notificacion> obtenerNotificacionesUsuario(String usuarioId) {
        ArrayDeque<Notificacion> deque = notificacionesPorUsuario.get(usuarioId);
        if (deque == null || deque.isEmpty()) {
            return List.of();
        }
        List<Notificacion> recientes = new ArrayList<>(deque.size());
        Iterator<Notificacion> it = deque.descendingIterator();
        while (it.hasNext()) {
            recientes.add(it.next());
        }
        return Collections.unmodifiableList(recientes);
    }

    public long contarNoLeidas(String usuarioId) {
        ArrayDeque<Notificacion> deque = notificacionesPorUsuario.get(usuarioId);
        if (deque == null) return 0;
        return deque.stream().filter(n -> !n.isLeida()).count();
    }

    public void marcarTodasComoLeidas(String usuarioId) {
        ArrayDeque<Notificacion> deque = notificacionesPorUsuario.get(usuarioId);
        if (deque != null) {
            deque.forEach(n -> n.setLeida(true));
        }
    }

    public Queue<Notificacion> getColaDespacho() {return colaDespacho;}

    public BigDecimal getUmbralSaldoBajo() {return umbralSaldoBajo;}

    public void setUmbralSaldoBajo(BigDecimal umbralSaldoBajo) {
        if (umbralSaldoBajo == null || umbralSaldoBajo.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El umbral de saldo bajo no puede ser negativo");
        }
        this.umbralSaldoBajo = umbralSaldoBajo;
    }

}
