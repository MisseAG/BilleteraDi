package co.edu.uniquindio.billeteradi.model.service;

import co.edu.uniquindio.billeteradi.model.entity.Billetera;
import co.edu.uniquindio.billeteradi.model.entity.RegistroConteo;
import co.edu.uniquindio.billeteradi.model.entity.Transaccion;
import co.edu.uniquindio.billeteradi.model.enums.EstadoTransaccion;
import co.edu.uniquindio.billeteradi.model.enums.TipoBilletera;
import co.edu.uniquindio.billeteradi.model.enums.TipoTransaccion;
import co.edu.uniquindio.billeteradi.model.exception.ReglaNegocioException;
import co.edu.uniquindio.billeteradi.model.repository.RepositorioMemoria;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class AnaliticaService {
    private final RepositorioMemoria repositorio;
    private final TreeMap<BigDecimal, List<String>> transaccionesPorMonto = new TreeMap<>();

    public AnaliticaService(RepositorioMemoria repositorio) {
        this.repositorio = repositorio;
    }

    public List<Transaccion> obtenerMovimientosEfectivosPorPeriodo(LocalDateTime inicio, LocalDateTime fin) {
        validarPeriodo(inicio, fin);
        List<Transaccion> cronologica = new ArrayList<>(repositorio.getTransaccionesMap().values());
        if (cronologica.isEmpty()) {
            return new ArrayList<>();
        }int indiceInicio = 0;
        if (inicio != null) {
            indiceInicio = buscarLimiteInferior(cronologica, inicio);
        }List<Transaccion> resultado = new ArrayList<>();
        for (int i = indiceInicio; i < cronologica.size(); i++) {
            Transaccion tx = cronologica.get(i);
            if (fin != null && tx.getFecha().isAfter(fin)) {
                break;
            }
            if (esMovimientoEfectivo(tx)) {
                resultado.add(tx);
            }
        }return resultado;
    }

    //Reporte 1: Frecuencia por tipo de movimiento.
    public Map<TipoTransaccion, Long> reporteFrecuenciaPorTipo(LocalDateTime inicio, LocalDateTime fin) {
        List<Transaccion> movimientos = obtenerMovimientosEfectivosPorPeriodo(inicio, fin);
        Map<TipoTransaccion, Long> conteo = new EnumMap<>(TipoTransaccion.class); //esto porque trabajamos con enums
        for (Transaccion tx : movimientos) {
            TipoTransaccion tipo = tx.getTipo();
            Long actual = conteo.get(tipo);
            if (actual == null) {
                conteo.put(tipo, 1L);
            } else {
                conteo.put(tipo, actual + 1L);
            }
        }return conteo;
    }

    //Reporte 2: Categorías más activas (participaciones de billeteras por tipo)
    public Map<TipoBilletera, Long> reporteCategoriasMasActivas(LocalDateTime inicio, LocalDateTime fin) {
        List<Transaccion> movimientos = obtenerMovimientosEfectivosPorPeriodo(inicio, fin);
        Map<TipoBilletera, Long> conteo = new EnumMap<>(TipoBilletera.class);
        for (Transaccion tx : movimientos) {
            contarParticipacionBilletera(tx.getOrigenId(), conteo);
            if (tx.getDestinoId() != null && !tx.getDestinoId().equals(tx.getOrigenId())) {
                contarParticipacionBilletera(tx.getDestinoId(), conteo);
            }
        }return conteo;
    }

    //Reporte 3: Billeteras con mayor uso.
    public List<RegistroConteo> reporteBilleterasMayorUso(LocalDateTime inicio, LocalDateTime fin, int k) {
        if (k <= 0) {
            return new ArrayList<>();
        }
        List<Transaccion> movimientos = obtenerMovimientosEfectivosPorPeriodo(inicio, fin);
        Map<String, Long> frecuencias = new HashMap<>();
        for (Transaccion tx : movimientos) {
            sumarEnMapa(frecuencias, tx.getOrigenId());
            if (tx.getDestinoId() != null && !tx.getDestinoId().equals(tx.getOrigenId())) {
                sumarEnMapa(frecuencias, tx.getDestinoId());
            }
        }return obtenerTopK(frecuencias, k);
    }

    //Reporte 4: Usuarios con mayor actividad.
    public List<RegistroConteo> reporteUsuariosMayorActividad(LocalDateTime inicio, LocalDateTime fin, int k) {
        if (k <= 0) {
            return new ArrayList<>();
        }
        List<Transaccion> movimientos = obtenerMovimientosEfectivosPorPeriodo(inicio, fin);
        Map<String, Long> actividad = new HashMap<>();
        for (Transaccion tx : movimientos) {
            String usuarioId = obtenerUsuarioIniciador(tx);
            sumarEnMapa(actividad, usuarioId);
        }return obtenerTopK(actividad, k);
    }

    //Reporte 5: Top-K Usuarios con más transferencias salientes.
    public List<RegistroConteo> reporteUsuariosMasTransferencias(LocalDateTime inicio, LocalDateTime fin, int k) {
        if (k <= 0) {
            return new ArrayList<>();
        }
        List<Transaccion> movimientos = obtenerMovimientosEfectivosPorPeriodo(inicio, fin);
        Map<String, Long> transferencias = new HashMap<>();
        for (Transaccion tx : movimientos) {
            if (tx.getTipo() == TipoTransaccion.TRANSFERENCIA && tx.getOrigenId() != null) {
                Optional<Billetera> optBilletera = repositorio.buscarBilletera(tx.getOrigenId());
                if (optBilletera.isPresent()) {
                    String propietarioId = optBilletera.get().getPropietarioId();
                    sumarEnMapa(transferencias, propietarioId);
                }
            }
        }return obtenerTopK(transferencias, k);
    }

    private void sumarEnMapa(Map<String, Long> mapa, String clave) {
        if (clave == null) {
            return;
        }
        Long valorActual = mapa.get(clave);
        if (valorActual == null) {
            mapa.put(clave, 1L);
        }else {
            mapa.put(clave, valorActual + 1L);
        }
    }

    private void contarParticipacionBilletera(String billeteraId, Map<TipoBilletera, Long> conteo) {
        if (billeteraId == null) {
            return;
        }
        Optional<Billetera> optBilletera = repositorio.buscarBilletera(billeteraId);
        if (optBilletera.isPresent()) {
            TipoBilletera tipo = optBilletera.get().getTipo();
            Long valorActual = conteo.get(tipo);
            if (valorActual == null) {
                conteo.put(tipo, 1L);
            } else {
                conteo.put(tipo, valorActual + 1L);
            }
        }
    }

    //por si no saben que es topK, es una técnica para obtener únicamente los k mejores elementos
    private List<RegistroConteo> obtenerTopK(Map<String, Long> conteos, int k) {
        PriorityQueue<RegistroConteo> minHeap = new PriorityQueue<>();
        for (String id : conteos.keySet()) {
            long cantidad = conteos.get(id);
            RegistroConteo registro = new RegistroConteo(id, cantidad);
            minHeap.offer(registro);
            if (minHeap.size() > k) {
                minHeap.poll(); // Elimina el de menor cantidad para conservar solo los k mejores
            }
        }List<RegistroConteo> resultado = new ArrayList<>();
        while (!minHeap.isEmpty()) {
            resultado.add(minHeap.poll());
        }
        Collections.reverse(resultado);
        return resultado;
    }

    private void validarPeriodo(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio != null && fin != null && inicio.isAfter(fin)) {
            throw new ReglaNegocioException("La fecha de inicio no puede ser posterior a la fecha de fin.");
        }
    }

    private boolean esMovimientoEfectivo(Transaccion tx) {
        return tx.getEstado() == EstadoTransaccion.EXITOSA
                && tx.getTipo() != TipoTransaccion.REVERSION;
    }

    private int buscarLimiteInferior(List<Transaccion> lista, LocalDateTime fechaObjetivo) {
        int bajo = 0;
        int alto = lista.size() - 1;
        int resultado = lista.size();
        while (bajo <= alto) {
            int medio = bajo + (alto - bajo) / 2;
            LocalDateTime fechaMedio = lista.get(medio).getFecha();
            if (!fechaMedio.isBefore(fechaObjetivo)) {
                resultado = medio;
                alto = medio - 1;
            } else {
                bajo = medio + 1;
            }
        }return resultado;
    }

    private String obtenerUsuarioIniciador(Transaccion tx) {
        String billeteraReferencia;
        if (tx.getOrigenId() != null) {
            billeteraReferencia = tx.getOrigenId();
        } else {
            billeteraReferencia = tx.getDestinoId();
        }
        if (billeteraReferencia == null) {
            return null;
        }
        Optional<Billetera> optBilletera = repositorio.buscarBilletera(billeteraReferencia);
        if (optBilletera.isPresent()) {
            return optBilletera.get().getPropietarioId();
        }return null;
    }

    public void registrarTransaccionEnMontos(Transaccion tx) {
        if (tx == null) {
            return;
        }
        if (tx.getEstado() != EstadoTransaccion.EXITOSA || tx.getTipo() == TipoTransaccion.REVERSION) {
            return;
        }
        BigDecimal monto = tx.getImporte();
        List<String> grupo = transaccionesPorMonto.get(monto);
        if (grupo == null) {
            grupo = new ArrayList<>();
            transaccionesPorMonto.put(monto, grupo);
        }
        if (!grupo.contains(tx.getId())) {
            grupo.add(tx.getId());
        }
    }

    public void eliminarTransaccionDeMontos(Transaccion tx) {
        if (tx == null) {
            return;
        }
        BigDecimal monto = tx.getImporte();
        List<String> grupo = transaccionesPorMonto.get(monto);
        if (grupo != null) {
            grupo.remove(tx.getId());
            if (grupo.isEmpty()) {
                transaccionesPorMonto.remove(monto);
            }
        }
    }

    //RF18 bo
    public List<Transaccion> consultarMayoresMontos(int limite) {
        List<Transaccion> resultado = new ArrayList<>();
        if (limite <= 0) {
            return resultado;
        }
        //se usa navigableMap porque permite buscar claves y valores que cumplen con criterios de búsqueda específicos
        //ya que permite acceder a las claves y valores de un map de manera ordenada, no sé si cambiarlo
        NavigableMap<BigDecimal, List<String>> mapaDescendente = transaccionesPorMonto.descendingMap();
        for (Map.Entry<BigDecimal, List<String>> entrada : mapaDescendente.entrySet()) {
            List<String> idsTransacciones = entrada.getValue();
            for (String txId : idsTransacciones) {
                Optional<Transaccion> optTx = repositorio.buscarTransaccion(txId);
                if (optTx.isPresent()) {
                    Transaccion tx = optTx.get();
                    // Verificación adicional de seguridad: que siga siendo efectiva[cite: 1]
                    if (tx.getEstado() == EstadoTransaccion.EXITOSA && tx.getTipo() != TipoTransaccion.REVERSION) {
                        resultado.add(tx);
                        if (resultado.size() >= limite) {
                            return resultado;
                        }
                    }
                }
            }
        }return resultado;
    }

}
