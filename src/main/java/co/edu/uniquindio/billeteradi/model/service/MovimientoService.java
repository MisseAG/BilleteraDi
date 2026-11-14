package co.edu.uniquindio.billeteradi.model.service;

import co.edu.uniquindio.billeteradi.model.entity.Billetera;
import co.edu.uniquindio.billeteradi.model.entity.Transaccion;
import co.edu.uniquindio.billeteradi.model.entity.Usuario;
import co.edu.uniquindio.billeteradi.model.enums.EstadoBilletera;
import co.edu.uniquindio.billeteradi.model.enums.EstadoTransaccion;
import co.edu.uniquindio.billeteradi.model.enums.TipoTransaccion;
import co.edu.uniquindio.billeteradi.model.exception.ReglaNegocioException;
import co.edu.uniquindio.billeteradi.model.repository.RepositorioMemoria;
import co.edu.uniquindio.billeteradi.model.structure.Pila;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Servicio encargado de gestionar los movimientos financieros (RF03, RF04, RF05).
 * Modifica saldos bajo reglas de negocio estrictas.
 */
public class MovimientoService {

    private final RepositorioMemoria repositorio;
    private final UsuarioService usuarioService;
    private final RecompensaService recompensaService;

    public MovimientoService(RepositorioMemoria repositorio, UsuarioService usuarioService) {
        this.repositorio = repositorio;
        this.usuarioService = usuarioService;
        this.recompensaService = new RecompensaService(repositorio);
    }

    public MovimientoService(RepositorioMemoria repositorio, UsuarioService usuarioService,
                             RecompensaService recompensaService) {
        this.repositorio = repositorio;
        this.usuarioService = usuarioService;
        this.recompensaService = recompensaService;
    }

    public Transaccion recargar(String usuarioId, String billeteraId, String textoImporte) {
        Usuario usuario = usuarioService.obtenerUsuario(usuarioId);
        validarUsuarioActivo(usuario);
        Billetera billetera = usuarioService.obtenerBilletera(billeteraId);

        validarPertenencia(usuario, billetera);
        BigDecimal importe = validarImporte(textoImporte);
        // ValidarBilleteraActiva no registra el rechazo
        // si la billetera está bloqueada
        // validarBilleteraActiva(billetera);
        validarBilleteraActivaParaOperar(
                usuario.getId(),
                TipoTransaccion.RECARGA,
                importe,
                null,
                billetera.getId(),
                billetera
        );

        BigDecimal nuevoSaldo = billetera.getSaldo().add(importe);
        billetera.setSaldo(nuevoSaldo);

        ResultadoPuntos resultadoPuntos = recompensaService.otorgarPorOperacion(
                usuario.getId(), TipoTransaccion.RECARGA, importe, false);

        Transaccion transaccion = new Transaccion(
                repositorio.siguienteCodigoTransaccion(),
                LocalDateTime.now(),
                TipoTransaccion.RECARGA,
                importe,
                null,
                billetera.getId(),
                EstadoTransaccion.EXITOSA,
                resultadoPuntos.puntosOtorgados(),
                null
        );

        repositorio.registrarTransaccion(transaccion, usuario.getId());
        repositorio.obtenerPilaReversion(usuario.getId()).push(transaccion.getId());
        return transaccion;
    }

    public Transaccion retirar(String usuarioId, String billeteraId, String textoImporte) {
        Usuario usuario = usuarioService.obtenerUsuario(usuarioId);
        validarUsuarioActivo(usuario);
        Billetera billetera = usuarioService.obtenerBilletera(billeteraId);

        validarPertenencia(usuario, billetera);
        BigDecimal importe = validarImporte(textoImporte);

        validarBilleteraActivaParaOperar(
                usuario.getId(),
                TipoTransaccion.RETIRO,
                importe,
                billetera.getId(),
                null,
                billetera
        );

        if (billetera.getSaldo().compareTo(importe) < 0) {
            registrarYRechazar(
                    usuario.getId(),
                    TipoTransaccion.RETIRO,
                    importe,
                    billetera.getId(),
                    null,
                    "Saldo insuficiente para realizar el retiro. Saldo actual: $ " + billetera.getSaldo()
            );
        }


        BigDecimal nuevoSaldo = billetera.getSaldo().subtract(importe);
        billetera.setSaldo(nuevoSaldo);

        ResultadoPuntos resultadoPuntos = recompensaService.otorgarPorOperacion(
                usuario.getId(), TipoTransaccion.RETIRO, importe, false);

        Transaccion transaccion = new Transaccion(
                repositorio.siguienteCodigoTransaccion(),
                LocalDateTime.now(),
                TipoTransaccion.RETIRO,
                importe,
                billetera.getId(),
                null,
                EstadoTransaccion.EXITOSA,
                resultadoPuntos.puntosOtorgados(),
                null
        );

        repositorio.registrarTransaccion(transaccion, usuario.getId());
        repositorio.obtenerPilaReversion(usuario.getId()).push(transaccion.getId());
        return transaccion;
    }

    public Transaccion transferir(String usuarioId, String billeteraOrigenId,
                                  String billeteraDestinoId, String textoImporte) {
        Usuario usuario = usuarioService.obtenerUsuario(usuarioId);
        validarUsuarioActivo(usuario);
        Billetera origen = usuarioService.obtenerBilletera(billeteraOrigenId);
        Billetera destino = usuarioService.obtenerBilletera(billeteraDestinoId);

        validarPertenencia(usuario, origen);
        BigDecimal importe = validarImporte(textoImporte);

        if (origen.getId().equals(destino.getId())) {
            registrarYRechazar(
                    usuario.getId(),
                    TipoTransaccion.TRANSFERENCIA,
                    importe,
                    origen.getId(),
                    destino.getId(),
                    "No se puede transferir a la misma billetera"
            );
        }

        // Validar que el propietario de la billetera destino esté activo

        Usuario propietarioDestino = usuarioService.obtenerUsuario(destino.getPropietarioId());
        if (!propietarioDestino.isActivo()){
            registrarYRechazar(
                    usuario.getId(),
                    TipoTransaccion.TRANSFERENCIA,
                    importe,
                    origen.getId(),
                    destino.getId(),
                    "No se puede transferir al usuario: " + propietarioDestino.getId() + " porque está inactivo"
            );
        }


        // Validar billetera origen
        validarBilleteraActivaParaOperar(
                usuario.getId(),
                TipoTransaccion.TRANSFERENCIA,
                importe,
                origen.getId(),
                destino.getId(),
                origen
        );
        // Validar Billetera destino
        validarBilleteraActivaParaOperar(
                usuario.getId(),
                TipoTransaccion.TRANSFERENCIA,
                importe,
                origen.getId(),
                destino.getId(),
                destino
        );

        if (origen.getSaldo().compareTo(importe) < 0) {
            registrarYRechazar(
                    usuario.getId(),
                    TipoTransaccion.TRANSFERENCIA,
                    importe,
                    origen.getId(),
                    destino.getId(),
                    "Saldo insuficiente para la transferencia. Saldo actual: $ " + origen.getSaldo()
            );
        }

        BigDecimal nuevoSaldoOrigen = origen.getSaldo().subtract(importe);
        BigDecimal nuevoSaldoDestino = destino.getSaldo().add(importe);

        origen.setSaldo(nuevoSaldoOrigen);
        destino.setSaldo(nuevoSaldoDestino);

        ResultadoPuntos resultadoPuntos = recompensaService.otorgarPorOperacion(
                usuario.getId(), TipoTransaccion.TRANSFERENCIA, importe, false);

        Transaccion transaccion = new Transaccion(
                repositorio.siguienteCodigoTransaccion(),
                LocalDateTime.now(),
                TipoTransaccion.TRANSFERENCIA,
                importe,
                origen.getId(),
                destino.getId(),
                EstadoTransaccion.EXITOSA,
                resultadoPuntos.puntosOtorgados(),
                null
        );

        repositorio.registrarTransaccion(transaccion, usuario.getId());
        repositorio.obtenerPilaReversion(usuario.getId()).push(transaccion.getId());
        return transaccion;
    }

    public List<Transaccion> listarTransacciones(String usuarioId) {
        return repositorio.listarTransaccionesDeUsuario(usuarioId);
    }

    public static BigDecimal validarImporte(String textoImporte) {
        if (textoImporte == null || textoImporte.trim().isEmpty()) {
            throw new ReglaNegocioException("El importe no puede estar vacío");
        }
        BigDecimal importe;
        try {
            importe = new BigDecimal(textoImporte.trim());
        } catch (NumberFormatException e) {
            throw new ReglaNegocioException("El importe ingresado no tiene un formato numérico válido");
        }
        if (importe.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("El importe debe ser estrictamente mayor a cero");
        }
        if (importe.scale() > 2) {
            throw new ReglaNegocioException("El importe no puede tener más de dos decimales");
        }
        return importe;
    }


    private void validarUsuarioActivo(Usuario usuario) {
        if (!usuario.isActivo()) {
            throw new ReglaNegocioException("El usuario '" + usuario.getId() + "' se encuentra inactivo y no puede operar");
        }
    }

    private void validarPertenencia(Usuario usuario, Billetera billetera) {
        if (!billetera.getPropietarioId().equals(usuario.getId())) {
            throw new ReglaNegocioException("La billetera '" + billetera.getId() + "' no pertenece al usuario seleccionado");
        }
    }

    private void validarBilleteraActiva(Billetera billetera) {
        if (billetera.getEstado() != EstadoBilletera.ACTIVA) {
            throw new ReglaNegocioException("La billetera '" + billetera.getId() + "' se encuentra bloqueada y no puede operar");
        }
    }

    private void validarBilleteraActivaParaOperar(String usuarioId, TipoTransaccion tipo, BigDecimal importe,
                                                  String origenId, String destinoId, Billetera billetera) {
        if (billetera.getEstado() != EstadoBilletera.ACTIVA) {
            registrarYRechazar(
                    usuarioId,
                    tipo,
                    importe,
                    origenId,
                    destinoId,
                    "La billetera '" + billetera.getId() + "' se encuentra bloqueada y no puede operar"
            );
        }
    }

    private void registrarYRechazar(String usuarioId, TipoTransaccion tipo, BigDecimal importe,
                                    String origenId, String destinoId, String mensajeError) {
        Transaccion rechazada = new Transaccion(
                repositorio.siguienteCodigoTransaccion(),
                LocalDateTime.now(),
                tipo,
                importe,
                origenId,
                destinoId,
                EstadoTransaccion.RECHAZADA,
                0,
                null
        );
        repositorio.registrarTransaccion(rechazada, usuarioId);
        throw new ReglaNegocioException(mensajeError);
    }

    //RF06
    public List<Transaccion> consultarHistorial(String usuarioId, String billeteraId, TipoTransaccion tipo,
                                                EstadoTransaccion estado, LocalDateTime inicio, LocalDateTime fin) {
        List<String> idsTransacciones;
        if (billeteraId != null && !billeteraId.trim().isEmpty()) {
            idsTransacciones = repositorio.getHistorialesBilletera().getOrDefault(billeteraId.trim(), List.of());
        } else if (usuarioId != null && !usuarioId.trim().isEmpty()) {
            idsTransacciones = repositorio.getHistorialesUsuario().getOrDefault(usuarioId.trim(), List.of());
        } else {
            idsTransacciones = new ArrayList<>(repositorio.getTransaccionesMap().keySet());
            // Si no se especifica usuario ni billetera, se evalua sobre todas las transacciones registradas
        }List<Transaccion> resultado = new ArrayList<>();
        for (String txId : idsTransacciones) {
            Optional<Transaccion> optTx = repositorio.buscarTransaccion(txId);
            if (optTx.isPresent()) {
                Transaccion tx = optTx.get();
                if (tipo != null && tx.getTipo() != tipo) continue;
                if (estado != null && tx.getEstado() != estado) continue;
                if (inicio != null && tx.getFecha().isBefore(inicio)) continue;
                if (fin != null && tx.getFecha().isAfter(fin)) continue;
                resultado.add(tx);
            }
        }return resultado;
    }

    public Transaccion consultarUltimaReversible(String usuarioId){
        Usuario usuario = usuarioService.obtenerUsuario(usuarioId);
        Pila<String> pila = repositorio.obtenerPilaReversion(usuario.getId());

        while (!pila.estaVacia()) {
            String txId = pila.peek();
            Transaccion tx = repositorio.buscarTransaccion(txId).orElse(null);

            if (tx == null || tx.getEstado() == EstadoTransaccion.REVERTIDA) {
                pila.pop();
            } else {
                return tx;
            }
        }
        return null;
    }

    public Transaccion revertirUltima(String usuarioId){
        Usuario usuario = usuarioService.obtenerUsuario(usuarioId);
        validarUsuarioActivo(usuario);
        Pila<String> pila = repositorio.obtenerPilaReversion(usuario.getId());

        Transaccion original = consultarUltimaReversible(usuario.getId());

        if (original == null) {
            throw new ReglaNegocioException("El usuario no tiene operaciones disponibles para revertir");
        }

        if (original.getEstado() != EstadoTransaccion.EXITOSA) {
            throw new ReglaNegocioException("Solo se pueden revertir transacciones exitosas");
        }

        if (original.getTipo() == TipoTransaccion.REVERSION) {
            throw new ReglaNegocioException("Un movimiento compensatorio no puede ser revertido");
        }

        long puntosARevertir = original.getPuntosGenerados();
        if (usuario.getPuntosDisponibles() < puntosARevertir) {
            throw new ReglaNegocioException(
              "No es posible revertir: puntos disponibles insuficientes ("
              + usuario.getPuntosDisponibles() + " puntos disponibles, se requieren " + puntosARevertir + ")"
            );
        }

        BigDecimal importe = original.getImporte();
        Billetera origenCompensacion = null;
        Billetera destinoCompensacion = null;

        switch (original.getTipo()) {
            case RECARGA -> {
                Billetera billetera = usuarioService.obtenerBilletera(original.getDestinoId());
                validarBilleteraActiva(billetera);
                if (billetera.getSaldo().compareTo(importe) < 0) {
                    throw new ReglaNegocioException(
                            "Fondos insuficientes para revertir la recarga. Saldo actual: $ " + billetera.getSaldo());
                }
                origenCompensacion = billetera;
            }
            case RETIRO -> {
                Billetera billetera = usuarioService.obtenerBilletera(original.getOrigenId());
                validarBilleteraActiva(billetera);
                destinoCompensacion = billetera;
            }
            case TRANSFERENCIA -> {
                Billetera origenOriginal = usuarioService.obtenerBilletera(original.getOrigenId());
                Billetera destinoOriginal = usuarioService.obtenerBilletera(original.getDestinoId());
                validarBilleteraActiva(origenOriginal);
                validarBilleteraActiva(destinoOriginal);

                if (destinoOriginal.getSaldo().compareTo(importe) < 0) {
                    throw new ReglaNegocioException(
                            "Fondos insuficientes en la billetera destino para revertir la transferencia. Saldo destino: $ " + destinoOriginal.getSaldo()
                    );
                }
                origenCompensacion = destinoOriginal;
                destinoCompensacion = origenOriginal;
            }
        }

        if (origenCompensacion != null) {
            origenCompensacion.setSaldo(origenCompensacion.getSaldo().subtract(importe));
        }

        if (destinoCompensacion != null) {
            destinoCompensacion.setSaldo(destinoCompensacion.getSaldo().add(importe));
        }

        recompensaService.revertirPuntos(usuario.getId(), puntosARevertir);

        Transaccion compensacion = new Transaccion(
                repositorio.siguienteCodigoTransaccion(),
                LocalDateTime.now(),
                TipoTransaccion.REVERSION,
                importe,
                origenCompensacion != null ? origenCompensacion.getId() : null,
                destinoCompensacion != null ? destinoCompensacion.getId() : null,
                EstadoTransaccion.EXITOSA,
                0,
                original.getId()
        );

        original.setEstado(EstadoTransaccion.REVERTIDA);
        repositorio.registrarTransaccion(compensacion, usuario.getId());
        pila.pop();

        return compensacion;
    }
}