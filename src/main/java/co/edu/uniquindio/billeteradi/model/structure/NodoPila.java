package co.edu.uniquindio.billeteradi.model.structure;

public class NodoPila<T> {
    private T dato;
    private NodoPila<T> siguiente;

    public NodoPila(T dato, NodoPila<T> siguiente) {
        this.dato = dato;
        this.siguiente = siguiente;
    }

    public T getDato() {
        return dato;
    }

    public void setDato(T dato) {
        this.dato = dato;
    }

    public NodoPila<T> getSiguiente() {
        return siguiente;
    }

    public void setSiguiente(NodoPila<T> siguiente) {
        this.siguiente = siguiente;
    }
}