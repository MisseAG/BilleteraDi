package co.edu.uniquindio.billeteradi.model.service;

import co.edu.uniquindio.billeteradi.model.entity.Billetera;
import co.edu.uniquindio.billeteradi.model.entity.Transaccion;
import co.edu.uniquindio.billeteradi.model.entity.Usuario;
import co.edu.uniquindio.billeteradi.model.enums.EstadoBilletera;
import co.edu.uniquindio.billeteradi.model.enums.EstadoTransaccion;
import co.edu.uniquindio.billeteradi.model.enums.TipoTransaccion;
import co.edu.uniquindio.billeteradi.model.exception.ReglaNegocioException;
import co.edu.uniquindio.billeteradi.model.repository.RepositorioMemoria;
import co.edu.uniquindio.billeteradi.model.service.UsuarioService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Servicio encargado de gestionar los movimientos financieros (RF03, RF04, RF05, RF11).
 * Es el ÚNICO punto autorizado para modificar saldos en el modelo.
 */
public class MovimientoService {

    private final RepositorioMemoria repositorio;
    private final UsuarioService usuarioService;

    public MovimientoService(RepositorioMemoria repositorio, UsuarioService usuarioService) {
        this.repositorio = repositorio;
        this.usuarioService = usuarioService;
    }

    //RF03
    public Transaccion recargar(String usuarioId, String billeteraId, String textoImporte) {
        // 1. Validaciones previas (Validar antes de modificar)
        Usuario usuario = usuarioService.obtenerUsuario(usuarioId);
        Billetera billetera = usuarioService.obtenerBilletera(billeteraId);

        validarPertenencia(usuario, billetera);
        validarBilleteraActiva(billetera);
        BigDecimal importe = validarImporte(textoImporte);

        // 2. Preparar cálculo del nuevo saldo
        BigDecimal nuevoSaldo = billetera.getSaldo().add(importe);

        // 3. Aplicar cambios (único punto de mutación)
        billetera.setSaldo(nuevoSaldo);

        // 4. Registrar la transacción exitosa
        // Nota: Los puntos (RF09) se integrarán con RecompensaService en este mismo hito más adelante.
        Transaccion transaccion = new Transaccion(
                repositorio.siguienteCodigoTransaccion(),
                LocalDateTime.now(),
                TipoTransaccion.RECARGA,
                importe,
                null, // No hay origen en recarga
                billetera.getId(),
                EstadoTransaccion.EXITOSA,
                0, // Se actualizará al incorporar puntos
                null
        );

        repositorio.registrarTransaccion(transaccion, usuario.getId());
        return transaccion;
    }

    //RF04 bo
    public Transaccion retirar(String usuarioId, String billeteraId, String textoImporte) {
        // 1. Validaciones previas
        Usuario usuario = usuarioService.obtenerUsuario(usuarioId);
        Billetera billetera = usuarioService.obtenerBilletera(billeteraId);

        validarPertenencia(usuario, billetera);
        validarBilleteraActiva(billetera);
        BigDecimal importe = validarImporte(textoImporte);

        // Validar saldo suficiente (s >= m)
        if (billetera.getSaldo().compareTo(importe) < 0) {
            // Registrar intento fallido o rechazo según regla de negocio
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
            throw new ReglaNegocioException("Saldo insuficiente para realizar el retiro. Saldo actual: " + billetera.getSaldo());
        }

        // 2. Preparar cálculo del nuevo saldo
        BigDecimal nuevoSaldo = billetera.getSaldo().subtract(importe);

        // 3. Aplicar cambios
        billetera.setSaldo(nuevoSaldo);

        // 4. Registrar la transacción exitosa
        Transaccion transaccion = new Transaccion(repositorio.siguienteCodigoTransaccion(),
                LocalDateTime.now(), TipoTransaccion.RETIRO, importe, billetera.getId(),
                null, EstadoTransaccion.EXITOSA, 0, null);
        repositorio.registrarTransaccion(transaccion, usuario.getId());
        return transaccion;
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
        }if (importe.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("El importe debe ser estrictamente mayor a cero");
        }if (importe.scale() > 2) {
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
