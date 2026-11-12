package co.edu.uniquindio.billeteradi.model.exception;

/**
 * Se lanza cuando una operación incumple una regla de negocio o recibe datos
 * inválidos (id repetido, usuario inexistente, nombre vacío...).
 *
 * Es no verificada (RuntimeException) a propósito: los controladores de
 * JavaFX la capturan en un solo lugar y muestran getMessage() al operario,
 * sin obligar a cada método intermedio a declarar "throws".
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
