package co.edu.uniquindio.billeteradi.model.entity;

//muchachos, esta es una clase auxiliar para reporteService bo
public class RegistroConteo implements Comparable<RegistroConteo> {
    private final String id;
    private final long cantidad;

    public RegistroConteo(String id, long cantidad) {
        this.id = id;
        this.cantidad = cantidad;
    }

    public String getId() { return id; }
    public long getCantidad() { return cantidad; }

    @Override
    public int compareTo(RegistroConteo otro) {
        return Long.compare(this.cantidad, otro.cantidad);
    }
}
