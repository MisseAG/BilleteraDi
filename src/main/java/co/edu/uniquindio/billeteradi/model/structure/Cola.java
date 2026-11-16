package co.edu.uniquindio.billeteradi.model.structure;

public class Cola<T>{
    private int size;
    private SimpleNode<T> firstNode;
    private SimpleNode<T> lastNode;

    public Cola(){
        this.size= 0;
        firstNode= null;
        lastNode= null;
    }

    public int getSize(){return size;}

    public boolean isEmpty(){return firstNode == null;}

    public void encolar(T data){
        SimpleNode<T> newSimpleNode= new SimpleNode<>(data);
        if(isEmpty()){
            firstNode= newSimpleNode;
            lastNode= newSimpleNode;
        }else{
            lastNode.setNext(newSimpleNode);
            lastNode= newSimpleNode;
        }size++;
    }

    public T desenconlar() {
        if (isEmpty()) {
            throw new RuntimeException("La cola está vacia crack\n");
        } else {
            T data = firstNode.getData();
            firstNode = firstNode.getNext();
            if (firstNode == null) {
                lastNode = null;
            }
            size--;
            return data;
        }

    }

    public void invertirColaRecursivo(){
        if(isEmpty()){
            System.out.println("papu, la lista está vacia\n");
            return;
        }T elemento= desenconlar();
        invertirColaRecursivo();
        encolar(elemento);
    }

    public void encolarFirst(T data){
        SimpleNode<T> newSimpleNode= new SimpleNode<>(data);
        if(isEmpty()){
            firstNode= newSimpleNode;
        }else{
            newSimpleNode.setNext(firstNode);
            firstNode= newSimpleNode;
        }size++;
    }

}

