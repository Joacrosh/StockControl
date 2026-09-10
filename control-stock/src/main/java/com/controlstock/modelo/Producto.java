package com.controlstock.modelo;

/**
 * Representa un producto del inventario. Ver sección 8.2 del documento
 * de requisitos.
 */

public class Producto {

    private int idProducto;
    private String codigo;
    private String nombre;
    private String descripcion;
    private int idCategoria;
    private double precio;
    private int cantidadActual;
    private int stockMinimo;
    private boolean activo;

    /**
     * Constructor para un producto NUEVO. No recibe cantidadActual como
     * parámetro "editable": recibe la cantidad inicial, que se guarda,
     * pero a partir de ahí solo se modifica con aumentarStock()/
     * disminuirStock() (ver más abajo).
     */

    
    public Producto(
         String codigo, String nombre, String descripcion,
         int idCategoria, double precio, int cantidadInicial,
         int stockMinimo
    ){
        setCodigo(codigo);
        setNombre(nombre);
        this.descripcion = descripcion;
        this.idCategoria = idCategoria;
        setPrecio(precio);
        setStockMinimo(stockMinimo);
        this.activo = true;

        if (cantidadInicial < 0){
            throw new IllegalArgumentException(
            "La cantidad inicial no puede ser negativa."
        );
        }
        this.cantidadActual = cantidadInicial;
    }

    /**
     * Constructor para RECONSTRUIR un producto que ya existe en la base
     * de datos. Lo usa ProductoDAOImpl al leer una fila de 'productos'.
     */

    public Producto(
        int idProducto, String codigo, String nombre,
        String descripcion, int idCategoria, double precio,
        int cantidadActual, int stockMinimo, boolean activo
    ){
        this.idProducto = idProducto;
        setCodigo(codigo);
        setNombre(nombre);
        this.descripcion = descripcion;
        this.idCategoria = idCategoria;
        setPrecio(precio);
        setStockMinimo(stockMinimo);
        this.activo = activo;
        this.cantidadActual = cantidadActual;
    }

    // ---------- Reglas de negocio propias del producto ----------
 
    /**
     * RN-06: un producto está en alerta de stock bajo cuando su cantidad
     * actual es menor a su stock mínimo. Esto se CALCULA, no se guarda
     * en ningún campo, para que nunca pueda quedar desactualizado.
     */

    public boolean tieneStockBajo(){
        return cantidadActual < stockMinimo;
    }

    /**
     * Único camino para que la cantidad de un producto SUBA. Lo va a
     * llamar InventarioServicio al confirmar un movimiento de tipo
     * ENTRADA — nunca se llama directamente desde la interfaz de usuario.
     */
    
    public void aumentarStock(int cantidad){
        if (cantidad <= 0){
            throw new IllegalArgumentException(
                "La cantidad debe ser mayor a cero."
            );
        }
        this.cantidadActual += cantidad;
    }

    /**
     * Único camino para que la cantidad de un producto BAJE. Acá vive
     * directamente la regla RN-01 (no se puede sacar más stock del
     * disponible) — el propio objeto se protege a sí mismo, sin depender
     * de que quien lo use se acuerde de validar antes.
     */

    public void disminuirStock(int cantidad){
        if (cantidad <= 0){
            throw new IllegalArgumentException(
                "La cantidad a disminuir debe ser mayor a 0"
            );
        }
        if (cantidad > this.cantidadActual){
            throw new IllegalStateException(
                "Stock insuficiente para '" + nombre
                + "': disponible " + cantidadActual 
                + ", se solicito " + cantidad);
        }
        this.cantidadActual -= cantidad;
    }

    // -------- Getters y setters --------

    public int getIdProducto(){
        return idProducto;
    }

    /** Lo usa el DAO después del INSERT, para completar el id autogenerado. */
    public void setIdProducto(int idProducto){
        this.idProducto = idProducto;
    }


    public String getCodigo(){
        return codigo;
    }

    public void setCodigo(String codigo){
        if (codigo == null || codigo.isBlank()){
            throw new IllegalArgumentException(
                "El codigo del producto no puede estar vacio"
            );
        }
        this.codigo = codigo;
    }

    public String getNombre(){
        return nombre;
    }

    public void setNombre(String nombre){
        if (nombre == null || nombre.isBlank()){
            throw new IllegalArgumentException(
                "El nombre del producto no puede estar vacio"
            );
        }
        this.nombre = nombre;
    }

    public String getDescripcion(){
        return descripcion;
    }

    public void setDescripcion(String descripcion){
        this.descripcion = descripcion;
    }

    public int getIdCategoria(){
        return idCategoria;
    }

    public void setIdCategoria(int idCategoria){
        this.idCategoria = idCategoria;
    }


    public double getPrecio(){
        return precio;
    }

    public void setPrecio(double precio){
        if (precio < 0){
            throw new IllegalArgumentException(
                "El precio no puede ser negativo"
            );
        }
        this.precio = precio;
    }

    /**
     * Getter de solo lectura: noten que NO hay un setCantidadActual().
     * Es intencional (ver el comentario al principio del archivo).
     */

    public int getCantidadActual(){
        return cantidadActual;
    }

    public int getStockMinimo(){
        return stockMinimo;
    }

    public void setStockMinimo(int stockMinimo){
        if (stockMinimo < 0){
            throw new IllegalArgumentException(
                "El stock minimo no puede ser negativo"
            );
        }
        this.stockMinimo = stockMinimo;
    }

    public boolean isActivo(){
        return activo;
    }

    @Override
    public String toString(){
        return "Producto{idProducto=" + idProducto
                + ", codigo='" + codigo + '\''
                + ", nombre='" + nombre + '\''
                + ", cantidadActual=" + cantidadActual
                + ", stockMinimo=" + stockMinimo
                + ", activo=" + activo
                + '}'; 
    }
}