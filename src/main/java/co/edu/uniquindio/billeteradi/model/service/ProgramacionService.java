package co.edu.uniquindio.billeteradi.model.service;

import co.edu.uniquindio.billeteradi.model.entity.OperacionProgramada;
import co.edu.uniquindio.billeteradi.model.enums.EstadoProgramacion;
import co.edu.uniquindio.billeteradi.model.enums.TipoTransaccion;
import co.edu.uniquindio.billeteradi.model.exception.ReglaNegocioException;
import co.edu.uniquindio.billeteradi.model.repository.RepositorioMemoria;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.PriorityQueue; // O tu estructura de cola/prioridad personalizada
import static co.edu.uniquindio.billeteradi.model.enums.TipoTransaccion.*;

public class ProgramacionService {
    private final RepositorioMemoria repositorio;
    private final MovimientoService movimientoService;
    private final NotificacionService notificacionService; // Opcional para RF12
    private final PriorityQueue<OperacionProgramada> pendientes;
    private long consecutivoProgramacion = 0;

    public ProgramacionService(RepositorioMemoria repositorio, MovimientoService movimientoService,
                               NotificacionService notificacionService) {
        this.repositorio = repositorio;
        this.movimientoService = movimientoService;
        this.notificacionService = notificacionService;
        Comparator<OperacionProgramada> comparador = Comparator.comparing(OperacionProgramada::getFechaEjecucion)
                .thenComparingLong(OperacionProgramada::getConsecutivo);
        this.pendientes = new PriorityQueue<>(comparador);
    }

    //RF07
    public OperacionProgramada programar(String usuarioId, String origenId, String destinoId,
                                         TipoTransaccion tipo, BigDecimal importe, LocalDateTime fechaEjecucion) {
        LocalDateTime ahora = LocalDateTime.now();
        if (fechaEjecucion == null || !fechaEjecucion.isAfter(ahora)) {
            throw new ReglaNegocioException("La fecha de ejecución debe ser estrictamente futura");
        }
        consecutivoProgramacion++;
        String idProg = String.format("PROG-%04d", consecutivoProgramacion);
        OperacionProgramada operacion = new OperacionProgramada(idProg, usuarioId, origenId, destinoId, tipo,
                importe, fechaEjecucion, consecutivoProgramacion, EstadoProgramacion.PENDIENTE);
        pendientes.offer(operacion);
        return operacion;
    }

    //RF08
    public void procesarVencidas(LocalDateTime tiempoActual) {
        while (!pendientes.isEmpty() && !pendientes.peek().getFechaEjecucion().isAfter(tiempoActual)) {
            OperacionProgramada prog = pendientes.poll();
            if (prog.getEstado() == EstadoProgramacion.CANCELADA) {
                continue;
            }try {
                switch (prog.getTipo()) {
                    case RECARGA -> movimientoService.recargar(prog.getUsuarioId(), prog.getDestinoId(),
                            prog.getImporte().toString());
                    case RETIRO -> movimientoService.retirar(prog.getUsuarioId(), prog.getOrigenId(),
                            prog.getImporte().toString());
                    case TRANSFERENCIA -> movimientoService.transferir(prog.getUsuarioId(), prog.getOrigenId(),
                            prog.getDestinoId(), prog.getImporte().toString());
                    default -> throw new IllegalArgumentException("Tipo de operación no soportado para programación");
                }
                prog.setEstado(EstadoProgramacion.EJECUTADA);
            } catch (Exception e) {
                prog.setEstado(EstadoProgramacion.FALLIDA);
                if (notificacionService != null) {
                    notificacionService.notificar(prog.getUsuarioId(), "La programación " + prog.getId() +
                            " falló: " + e.getMessage());
                }
            }
        }
    }
}
