package co.edu.uniquindio.billeteradi.model.service;

import co.edu.uniquindio.billeteradi.model.entity.Notificacion;
import java.time.LocalDateTime;
import java.util.*;

public class NotificacionService {
    private static final int TOPE_RECIENTES = 20;
    private final Queue<Notificacion> colaDespacho = new ArrayDeque<>();
    private final Map<String, ArrayDeque<Notificacion>> notificacionesPorUsuario = new HashMap<>();
    private long consecutivoNotificacion = 0;

    public Notificacion notificar(String usuarioId, String mensaje) {
        consecutivoNotificacion++;
        String id = String.format("NOTIF-%04d", consecutivoNotificacion);
        Notificacion notificacion = new Notificacion(id, usuarioId, mensaje, LocalDateTime.now());
        colaDespacho.offer(notificacion);
        ArrayDeque<Notificacion> dequeUsuario = notificacionesPorUsuario.computeIfAbsent(usuarioId,
                k -> new ArrayDeque<>());
        if (dequeUsuario.size() >= TOPE_RECIENTES) {
            dequeUsuario.pollFirst(); // Remueve la más antigua si supera el tope
        }dequeUsuario.addLast(notificacion);
        return notificacion;
    }

    public List<Notificacion> obtenerNotificacionesUsuario(String usuarioId) {
        ArrayDeque<Notificacion> deque = notificacionesPorUsuario.get(usuarioId);
        if (deque == null) {
            return List.of();
        }return List.copyOf(deque);
    }

    public Queue<Notificacion> getColaDespacho() {return colaDespacho;}


}
