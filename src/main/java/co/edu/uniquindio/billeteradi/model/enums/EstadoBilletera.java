package co.edu.uniquindio.billeteradi.model.enums;

/**
 * Estado operativo de una billetera. En H3, MovimientoService solo
 * permitirá operar con billeteras ACTIVA.
 */
public enum EstadoBilletera {
    ACTIVA,
    BLOQUEADA
}
