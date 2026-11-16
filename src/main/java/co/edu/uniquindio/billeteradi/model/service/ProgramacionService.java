package co.edu.uniquindio.billeteradi.model.service;

import co.edu.uniquindio.billeteradi.model.entity.OperacionProgramada;
import co.edu.uniquindio.billeteradi.model.enums.EstadoProgramacion;
import co.edu.uniquindio.billeteradi.model.enums.TipoTransaccion;
import co.edu.uniquindio.billeteradi.model.exception.ReglaNegocioException;
import co.edu.uniquindio.billeteradi.model.repository.RepositorioMemoria;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue; // O tu estructura de cola/prioridad personalizada
import static co.edu.uniquindio.billeteradi.model.enums.TipoTransaccion.*;

public class ProgramacionService {
    private final RepositorioMemoria repositorio;
    private final MovimientoService movimientoService;
    private final NotificacionService notificacionService; // Opcional para RF12
    private final PriorityQueue<OperacionProgramada> pendientes;
    private final List<OperacionProgramada> historico = new ArrayList<>();
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
        historico.add(operacion);
        return operacion;
    }

    //RF08
    public int procesarVencidas(LocalDateTime tiempoActual) {
        int contador = 0;
        while (!pendientes.isEmpty() && !pendientes.peek().getFechaEjecucion().isAfter(tiempoActual)) {
            OperacionProgramada prog = pendientes.poll();
            if (prog.getEstado() == EstadoProgramacion.CANCELADA) {
                continue;
            }
            ejecutarUna(prog);
            contador++;
        }
        return contador;
    }

    public int procesarTodasPendientes() {
        int contador = 0;
        while (!pendientes.isEmpty()) {
            OperacionProgramada prog = pendientes.poll();
            if (prog.getEstado() == EstadoProgramacion.CANCELADA) {
                continue;
            }
            ejecutarUna(prog);
            contador++;
        }
        return contador;
    }

    public void ejecutarOperacion(OperacionProgramada prog) {
        if (prog == null) return;
        if (prog.getEstado() == EstadoProgramacion.CANCELADA) {
            pendientes.remove(prog);
            return;
        }
        pendientes.remove(prog);
        ejecutarUna(prog);
    }

    public void removerDePendientes(OperacionProgramada op) {
        if (op != null) {
            pendientes.remove(op);
        }
    }

    private void ejecutarUna(OperacionProgramada prog) {
        try {
            co.edu.uniquindio.billeteradi.model.entity.Transaccion tx;
            switch (prog.getTipo()) {
                case RECARGA -> tx = movimientoService.recargar(prog.getUsuarioId(), prog.getDestinoId(),
                        prog.getImporte().toString(), true);
                case RETIRO -> tx = movimientoService.retirar(prog.getUsuarioId(), prog.getOrigenId(),
                        prog.getImporte().toString(), true);
                case TRANSFERENCIA -> tx = movimientoService.transferir(prog.getUsuarioId(), prog.getOrigenId(),
                        prog.getDestinoId(), prog.getImporte().toString(), true);
                default -> throw new IllegalArgumentException("Tipo de operación no soportado para programación");
            }
            prog.setEstado(EstadoProgramacion.EJECUTADA);
            if (tx != null) {
                prog.setTransaccionGeneradaId(tx.getId());
            }
        } catch (Exception e) {
            prog.setEstado(EstadoProgramacion.FALLIDA);
            if (notificacionService != null) {
                notificacionService.notificar(prog.getUsuarioId(), "La programación " + prog.getId() +
                        " falló: " + e.getMessage());
            }
        }
    }

    public List<OperacionProgramada> listarTodas() {
        List<OperacionProgramada> todas = new ArrayList<>(historico);
        todas.sort(Comparator.comparing(OperacionProgramada::getFechaEjecucion).reversed());
        return todas;
    }

    public long getConsecutivoProgramacion() {
        return consecutivoProgramacion;
    }

    public void setConsecutivoProgramacion(long valor) {
        this.consecutivoProgramacion = valor;
    }

    public void agregarDirecto(OperacionProgramada op) {
        if (!historico.contains(op)) {
            historico.add(op);
        }
        if (op.getEstado() == EstadoProgramacion.PENDIENTE) {
            pendientes.offer(op);
        }
    }
}
