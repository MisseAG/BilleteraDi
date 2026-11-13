package co.edu.uniquindio.billeteradi.model.structure;

import java.util.NoSuchElementException;

public class Pila<T> {
    private NodoPila<T> cima;
    private int tamano;

    public Pila() {
        this.cima = null;
        this.tamano = 0;
    }

    /**
     * Inserta un elemento en el tope de la pila. Complejidad: O(1).
     */
    public void push(T dato) {
        if (dato == null) {
            throw new IllegalArgumentException("No se permiten elementos nulos en la pila.");
        }
        cima = new NodoPila<>(dato, cima);
        tamano++;
    }

    /**
     * Consulta el elemento en el tope sin retirarlo. Complejidad: O(1).
     * Retorna null si la pila está vacía.
     */
    public T peek() {
        if (estaVacia()) {
            return null;
        }
        return cima.getDato();
    }

    /**
     * Retira y retorna el elemento en el tope de la pila. Complejidad: O(1).
     */
    public T pop() {
        if (estaVacia()) {
            throw new NoSuchElementException("No se puede hacer pop: la pila está vacía.");
        }
        T datoCima = cima.getDato();
        cima = cima.getSiguiente();
        tamano--;
        return datoCima;
    }

    public boolean estaVacia() {
        return cima == null;
    }

    public int tamano() {
        return tamano;
    }

    public void limpiar() {
        cima = null;
        tamano = 0;
    }
}