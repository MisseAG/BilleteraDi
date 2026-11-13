package co.edu.uniquindio.billeteradi.model.service;

import co.edu.uniquindio.billeteradi.model.enums.NivelUsuario;

/**
 * Resultado de acreditar puntos a un usuario.
 *
 * puntosOtorgados es el valor que MovimientoService debe guardar en la
 * transacción ("puntos generados")
 *
 * Los niveles anterior y nuevo permiten que H5 genere la notificación de
 * ascenso sin que RecompensaService conozca el servicio de notificaciones.
 */
public record ResultadoPuntos(
        String usuarioId,
        long puntosOtorgados,
        long puntosAcumuladosNetos,
        long puntosDisponibles,
        NivelUsuario nivelAnterior,
        NivelUsuario nivelNuevo) {

    /** true si el usuario subió de nivel con este movimiento. */
    public boolean ascendio() {
        return nivelNuevo.compareTo(nivelAnterior) > 0;
    }
}
