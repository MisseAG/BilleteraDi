package co.edu.uniquindio.billeteradi.model.service;

import co.edu.uniquindio.billeteradi.model.entity.Usuario;
import co.edu.uniquindio.billeteradi.model.enums.NivelUsuario;
import co.edu.uniquindio.billeteradi.model.enums.TipoTransaccion;
import co.edu.uniquindio.billeteradi.model.exception.ReglaNegocioException;
import co.edu.uniquindio.billeteradi.model.repository.RepositorioMemoria;
import co.edu.uniquindio.billeteradi.model.structure.TablaNiveles;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Puntos de fidelización y nivel de usuario (RF09 y RF10).
 *
 * Política (sección 1.5 del documento):
 *   p = k * floor(m / 100)      k: recarga 1, retiro 2, transferencia 3
 *   + bono configurable si la operación es un pago programado ejecutado con éxito
 *
 * Contrato con MovimientoService, por cada operación EXITOSA:
 *   1. Llamar UNA sola vez a otorgarPorOperacion con el id del usuario que INICIA
 *      la operación (una transferencia interna no concede puntos dos veces).
 *   2. Guardar resultado.puntosOtorgados() en la transacción.
 * Las operaciones rechazadas no llaman a este servicio: no conceden puntos.
 *
 * Fuera de alcance por ahora: descontar puntos al revertir y canjes (H5 y H7).
 */
public class RecompensaService {

    /** Valor inicial del bono de pago programado; es configurable. */
    public static final long BONO_PROGRAMADA_POR_DEFECTO = 10;

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final RepositorioMemoria repositorio;
    private final TablaNiveles niveles;
    private long bonoProgramada;

    public RecompensaService(RepositorioMemoria repositorio) {
        this(repositorio, new TablaNiveles(), BONO_PROGRAMADA_POR_DEFECTO);
    }

    public RecompensaService(RepositorioMemoria repositorio, TablaNiveles niveles, long bonoProgramada) {
        this.repositorio = repositorio;
        this.niveles = niveles;
        setBonoProgramada(bonoProgramada);
    }

    // ---------------- política de puntos (función pura, sin estado) ----------------

    /** Puntos de una operación normal: k * floor(importe / 100). */
    public long calcularPuntos(TipoTransaccion tipo, BigDecimal importe) {
        return calcularPuntos(tipo, importe, false);
    }

    /**
     * @param programadaExitosa true si es un pago programado ejecutado con éxito: suma el bono
     *                          (el bono se suma aunque los puntos base sean 0).
     */
    public long calcularPuntos(TipoTransaccion tipo, BigDecimal importe, boolean programadaExitosa) {
        if (tipo == null) {
            throw new ReglaNegocioException("Debe indicar el tipo de operación");
        }
        if (!tipo.generaPuntos()) {
            throw new ReglaNegocioException("Una operación de tipo " + tipo + " no genera puntos");
        }
        if (importe == null) {
            throw new ReglaNegocioException("El importe es obligatorio");
        }
        if (importe.signum() <= 0) {
            throw new ReglaNegocioException("El importe debe ser mayor que cero");
        }
        try {
            // floor(m / 100) en BigDecimal: sin pasar por double, sin error de redondeo.
            long centenas = importe.divide(CIEN, 0, RoundingMode.FLOOR).longValueExact();
            long base = Math.multiplyExact(centenas, tipo.getMultiplicador());
            return programadaExitosa ? Math.addExact(base, bonoProgramada) : base;
        } catch (ArithmeticException e) {
            throw new ReglaNegocioException("El importe es demasiado grande para calcular puntos");
        }
    }

    // ---------------- nivel ----------------

    /** Nivel que corresponde a unos puntos acumulados netos. */
    public NivelUsuario determinarNivel(long puntosAcumuladosNetos) {
        if (puntosAcumuladosNetos < 0) {
            throw new ReglaNegocioException("Los puntos acumulados netos no pueden ser negativos");
        }
        return niveles.nivelPara(puntosAcumuladosNetos);
    }

    /** Recalcula y guarda el nivel del usuario a partir de sus puntos acumulados netos. */
    public NivelUsuario actualizarNivel(Usuario usuario) {
        NivelUsuario nivel = determinarNivel(usuario.getPuntosAcumuladosNetos());
        usuario.setNivel(nivel);
        return nivel;
    }

    // ---------------- acreditar puntos ----------------

    /**
     * Calcula los puntos de una operación exitosa y los acredita al usuario que la inicia.
     * Valida todo antes de modificar: si algo falla, el usuario queda intacto.
     */
    public ResultadoPuntos otorgarPorOperacion(String usuarioId, TipoTransaccion tipo,
                                               BigDecimal importe, boolean programadaExitosa) {
        long puntos = calcularPuntos(tipo, importe, programadaExitosa);
        return otorgarPuntos(usuarioId, puntos);
    }

    /** Acredita puntos ya calculados y actualiza el nivel. 0 puntos es válido (importes menores a 100). */
    public ResultadoPuntos otorgarPuntos(String usuarioId, long puntos) {
        if (puntos < 0) {
            throw new ReglaNegocioException("Los puntos a otorgar no pueden ser negativos");
        }
        Usuario usuario = repositorio.buscarUsuario(usuarioId == null ? "" : usuarioId.trim())
                .orElseThrow(() -> new ReglaNegocioException("El usuario '" + usuarioId + "' no existe"));

        NivelUsuario nivelAnterior = usuario.getNivel();
        try {
            usuario.acreditarPuntos(puntos);
        } catch (ArithmeticException e) {
            throw new ReglaNegocioException("Los puntos del usuario superarían el máximo permitido");
        }
        NivelUsuario nivelNuevo = actualizarNivel(usuario);

        return new ResultadoPuntos(usuario.getId(), puntos, usuario.getPuntosAcumuladosNetos(),
                usuario.getPuntosDisponibles(), nivelAnterior, nivelNuevo);
    }

    // ---------------- configuración ----------------

    public long getBonoProgramada() {
        return bonoProgramada;
    }

    public void setBonoProgramada(long bonoProgramada) {
        if (bonoProgramada < 0) {
            throw new ReglaNegocioException("El bono de pago programado no puede ser negativo");
        }
        this.bonoProgramada = bonoProgramada;
    }
}