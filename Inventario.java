package modelo; // Paquete para las clases de modelo

import java.util.ArrayList; // Importa la clase ArrayList para manejar listas dinámicas

public class Inventario 
 {
    // 1. Aquí se USA por primera vez (declaración)
    private ArrayList<Producto> listaProductos; 

    public Inventario() {
        // 2. Y aquí se USA por segunda vez (inicialización)
        this.listaProductos = new ArrayList<>(); 
    }
    



}