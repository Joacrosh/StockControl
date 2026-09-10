package com.controlstock.modelo;

/**
 * Representa un rubro o categoría comercial al que pertenecen los productos
 * (por ejemplo: "Bebidas", "Limpieza"). Ver sección 8.1 del documento de
 * requisitos.
 */

public class Categoria {
    // Todos los campos son 'private': es la base del encapsulamiento.
    // Nadie fuera de esta clase puede leer ni modificar estos valores
    // directamente; tienen que pasar por los métodos get/set de más abajo.
    // Esto permite, por ejemplo, que setNombre() rechace un nombre vacío
    // sin importar desde qué parte del programa se intente asignar.

    private int idCategoria;
    private String nombre;
    private boolean activo;

    /**
     * Constructor para dar de alta una categoría NUEVA, todavía sin
     * persistir. El idCategoria queda en 0 porque lo va a asignar MySQL
     * (columna AUTO_INCREMENT) recién cuando el DAO haga el INSERT.
     */
    
    public Categoria(String nombre){
        setNombre(nombre);
        this.activo = true; // toda categoria nueva nace activa
    }

    /**
     * Constructor para RECONSTRUIR una categoría que ya existe en la base
     * de datos. Lo va a usar CategoriaDAOImpl cuando lea una fila de la
     * tabla 'categorias' y arme el objeto Java correspondiente.
     */

    public Categoria(int idCategoria, String nombre, boolean activo){
        this.idCategoria = idCategoria;
        setNombre(nombre);
        this.activo = activo;
    }

    // ---------- Getters y setters ----------

    public int getIdCategoria(){
        return idCategoria;
    }

    /**
     * Lo usa el DAO justo después de insertar la categoría en la base,
     * para completar el objeto con el id autogenerado por MySQL.
    */
   
    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
    }

    public String getNombre(){
        return nombre;
    }

    public void setNombre(String nombre){
        // Esto es lo que justifica tener un setter en vez de un campo
        // público: acá se puede validar ANTES de aceptar el valor.
        
        if (nombre == null || nombre.isBlank()){
            throw new IllegalArgumentException(
                "El nombre de la categoria no puede estar vacio"
            );
        }
        this.nombre = nombre; 
    }

    public boolean isActivo(){
        return activo;
    }

    public void setActivo(boolean activo){
        this.activo = activo;
    }

    /**
     * toString() no es obligatorio, pero es una convención muy fuerte en
     * Java: sin esto, imprimir un objeto (ej. System.out.println(categoria))
     * muestra algo ilegible como "Categoria@1b6d3586". Sobreescribirlo
     * ayuda muchísimo mientras están debuggeando a mano.
     */
    
    @Override 
    public String toString(){
        return "Categoria{idCategoria=" + idCategoria
            + ", nombre" + nombre + '\''
            + ", activo" + activo
            + '}';
            
    }




}