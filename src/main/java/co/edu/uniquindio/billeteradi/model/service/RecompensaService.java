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
 * Servicio de fidelización y cálculo de puntos (RF09 y RF10).
 */
public class RecompensaService {

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

    public long calcularPuntos(TipoTransaccion tipo, BigDecimal importe) {
        return calcularPuntos(tipo, importe, false);
    }

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
            long centenas = importe.divide(CIEN, 0, RoundingMode.FLOOR).longValueExact();
            long base = Math.multiplyExact(centenas, tipo.getMultiplicador());
            return programadaExitosa ? Math.addExact(base, bonoProgramada) : base;
        } catch (ArithmeticException e) {
            throw new ReglaNegocioException("El importe es demasiado grande para calcular puntos");
        }
    }

    public NivelUsuario determinarNivel(long puntosAcumuladosNetos) {
        if (puntosAcumuladosNetos < 0) {
            throw new ReglaNegocioException("Los puntos acumulados netos no pueden ser negativos");
        }
        return niveles.nivelPara(puntosAcumuladosNetos);
    }

    public NivelUsuario actualizarNivel(Usuario usuario) {
        NivelUsuario nivel = determinarNivel(usuario.getPuntosAcumuladosNetos());
        usuario.setNivel(nivel);
        return nivel;
    }

    public ResultadoPuntos otorgarPorOperacion(String usuarioId, TipoTransaccion tipo,
                                               BigDecimal importe, boolean programadaExitosa) {
        long puntos = calcularPuntos(tipo, importe, programadaExitosa);
        return otorgarPuntos(usuarioId, puntos);
    }

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
