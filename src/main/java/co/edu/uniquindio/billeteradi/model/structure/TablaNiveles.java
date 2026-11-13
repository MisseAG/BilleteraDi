package co.edu.uniquindio.billeteradi.model.structure;

import co.edu.uniquindio.billeteradi.model.enums.NivelUsuario;

import java.util.Map;
import java.util.TreeMap;

/**
 * Tabla de niveles de fidelización sobre un TreeMap
 *
 * La clave es el límite INFERIOR de cada nivel y floorEntry(puntos) devuelve
 * la entrada con la mayor clave menor o igual a los puntos:
 *
 *      0 -> BRONCE   501 -> PLATA   1001 -> ORO   5001 -> PLATINO
 *
 *   500 -> floor = 0    (BRONCE)      501 -> floor = 501  (PLATA)
 *  1000 -> floor = 501  (PLATA)      1001 -> floor = 1001 (ORO)
 *  5000 -> floor = 1001 (ORO)        5001 -> floor = 5001 (PLATINO)
 *
 * Reemplaza una cadena de if/else: los umbrales son datos, no código, y cambiar
 * uno no obliga a tocar la lógica.
 */
public class TablaNiveles {

    private final TreeMap<Long, NivelUsuario> umbrales = new TreeMap<>();

    /** Tabla por defecto del enunciado (sección 1.5 del documento). */
    public TablaNiveles() {
        umbrales.put(0L, NivelUsuario.BRONCE);
        umbrales.put(501L, NivelUsuario.PLATA);
        umbrales.put(1001L, NivelUsuario.ORO);
        umbrales.put(5001L, NivelUsuario.PLATINO);
    }

    /**
     * Tabla personalizada: clave = límite inferior del nivel.
     * Debe incluir el 0, o los usuarios nuevos quedarían sin nivel.
     */
    public TablaNiveles(Map<Long, NivelUsuario> umbralesPersonalizados) {
        if (umbralesPersonalizados == null || !umbralesPersonalizados.containsKey(0L)) {
            throw new IllegalArgumentException("La tabla de niveles debe incluir un nivel que empiece en 0 puntos");
        }
        if (umbralesPersonalizados.keySet().stream().anyMatch(k -> k == null || k < 0)
                || umbralesPersonalizados.values().stream().anyMatch(v -> v == null)) {
            throw new IllegalArgumentException("Los umbrales no pueden ser negativos ni nulos");
        }
        umbrales.putAll(umbralesPersonalizados);
    }

    /** @throws IllegalArgumentException si los puntos son negativos. */
    public NivelUsuario nivelPara(long puntosAcumuladosNetos) {
        if (puntosAcumuladosNetos < 0) {
            throw new IllegalArgumentException("Los puntos acumulados netos no pueden ser negativos");
        }
        return umbrales.floorEntry(puntosAcumuladosNetos).getValue();
    }
}
