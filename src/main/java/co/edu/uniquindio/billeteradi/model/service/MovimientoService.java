package co.edu.uniquindio.billeteradi.model.service;

import co.edu.uniquindio.billeteradi.model.entity.Billetera;
import co.edu.uniquindio.billeteradi.model.entity.Transaccion;
import co.edu.uniquindio.billeteradi.model.entity.Usuario;
import co.edu.uniquindio.billeteradi.model.enums.EstadoBilletera;
import co.edu.uniquindio.billeteradi.model.enums.EstadoTransaccion;
import co.edu.uniquindio.billeteradi.model.enums.TipoTransaccion;
import co.edu.uniquindio.billeteradi.model.exception.ReglaNegocioException;
import co.edu.uniquindio.billeteradi.model.repository.RepositorioMemoria;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio encargado de gestionar los movimientos financieros (RF03, RF04, RF05).
 * Modifica saldos bajo reglas de negocio estrictas.
 */
public class MovimientoService {

    private final RepositorioMemoria repositorio;
    private final UsuarioService usuarioService;

    public MovimientoService(RepositorioMemoria repositorio, UsuarioService usuarioService) {
        this.repositorio = repositorio;
        this.usuarioService = usuarioService;
    }

    public Transaccion recargar(String usuarioId, String billeteraId, String textoImporte) {
        Usuario usuario = usuarioService.obtenerUsuario(usuarioId);
        Billetera billetera = usuarioService.obtenerBilletera(billeteraId);

        validarPertenencia(usuario, billetera);
        validarBilleteraActiva(billetera);
        BigDecimal importe = validarImporte(textoImporte);

        BigDecimal nuevoSaldo = billetera.getSaldo().add(importe);
        billetera.setSaldo(nuevoSaldo);

        Transaccion transaccion = new Transaccion(
                repositorio.siguienteCodigoTransaccion(),
                LocalDateTime.now(),
                TipoTransaccion.RECARGA,
                importe,
                null,
                billetera.getId(),
                EstadoTransaccion.EXITOSA,
                0,
                null
        );

        repositorio.registrarTransaccion(transaccion, usuario.getId());
        return transaccion;
    }

    public Transaccion retirar(String usuarioId, String billeteraId, String textoImporte) {
        Usuario usuario = usuarioService.obtenerUsuario(usuarioId);
        Billetera billetera = usuarioService.obtenerBilletera(billeteraId);

        validarPertenencia(usuario, billetera);
        validarBilleteraActiva(billetera);
        BigDecimal importe = validarImporte(textoImporte);

        if (billetera.getSaldo().compareTo(importe) < 0) {
            Transaccion transaccionRechazada = new Transaccion(
                    repositorio.siguienteCodigoTransaccion(),
                    LocalDateTime.now(),
                    TipoTransaccion.RETIRO,
                    importe,
                    billetera.getId(),
                    null,
                    EstadoTransaccion.RECHAZADA,
                    0,
                    null
            );
            repositorio.registrarTransaccion(transaccionRechazada, usuario.getId());
            throw new ReglaNegocioException("Saldo insuficiente para realizar el retiro. Saldo actual: $ " + billetera.getSaldo());
        }

        BigDecimal nuevoSaldo = billetera.getSaldo().subtract(importe);
        billetera.setSaldo(nuevoSaldo);

        Transaccion transaccion = new Transaccion(
                repositorio.siguienteCodigoTransaccion(),
                LocalDateTime.now(),
                TipoTransaccion.RETIRO,
                importe,
                billetera.getId(),
                null,
                EstadoTransaccion.EXITOSA,
                0,
                null
        );
        repositorio.registrarTransaccion(transaccion, usuario.getId());
        return transaccion;
    }

    public Transaccion transferir(String usuarioId, String billeteraOrigenId,
                                  String billeteraDestinoId, String textoImporte) {
        Usuario usuario = usuarioService.obtenerUsuario(usuarioId);
        Billetera origen = usuarioService.obtenerBilletera(billeteraOrigenId);
        Billetera destino = usuarioService.obtenerBilletera(billeteraDestinoId);

        validarPertenencia(usuario, origen);
        validarBilleteraActiva(origen);
        validarBilleteraActiva(destino);
        BigDecimal importe = validarImporte(textoImporte);

        if (origen.getId().equals(destino.getId())) {
            throw new ReglaNegocioException("No se puede transferir a la misma billetera");
        }

        if (origen.getSaldo().compareTo(importe) < 0) {
            Transaccion rechazada = new Transaccion(
                    repositorio.siguienteCodigoTransaccion(),
                    LocalDateTime.now(),
                    TipoTransaccion.TRANSFERENCIA,
                    importe,
                    origen.getId(),
                    destino.getId(),
                    EstadoTransaccion.RECHAZADA,
                    0,
                    null
            );
            repositorio.registrarTransaccion(rechazada, usuario.getId());
            throw new ReglaNegocioException(
                    "Saldo insuficiente para la transferencia. Saldo actual: $ " + origen.getSaldo());
        }

        BigDecimal nuevoSaldoOrigen = origen.getSaldo().subtract(importe);
        BigDecimal nuevoSaldoDestino = destino.getSaldo().add(importe);

        origen.setSaldo(nuevoSaldoOrigen);
        destino.setSaldo(nuevoSaldoDestino);

        Transaccion transaccion = new Transaccion(
                repositorio.siguienteCodigoTransaccion(),
                LocalDateTime.now(),
                TipoTransaccion.TRANSFERENCIA,
                importe,
                origen.getId(),
                destino.getId(),
                EstadoTransaccion.EXITOSA,
                0,
                null
        );
        repositorio.registrarTransaccion(transaccion, usuario.getId());
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
}
