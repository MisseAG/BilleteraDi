package co.edu.uniquindio.billeteradi.model.enums;

public enum TipoTransaccion {
    RECARGA(1),
    RETIRO(2),
    TRANSFERENCIA(3),
    REVERSION(0);          // movimiento compensatorio: no genera puntos

    private final int multiplicador;
    TipoTransaccion(int multiplicador) { this.multiplicador = multiplicador; }

    public int getMultiplicador() { return multiplicador; }
    public boolean generaPuntos() { return multiplicador > 0; }
}
