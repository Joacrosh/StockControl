package main.java.com.controlstock.modelo;

import java.time.LocalDateTime;


/**
 * Representa un movimiento de entrada o salida de stock. Ver sección 8.3
 * del documento de requisitos.
 *
 * A diferencia de Categoria y Producto, esta clase es prácticamente
 * INMUTABLE: fuera del id (que lo completa el DAO tras el INSERT), no
 * tiene ningún setter público. Un movimiento, una vez creado, no se
 * modifica — es un registro histórico. Si algo salió mal, se corrige
 * con un movimiento nuevo que lo compense, nunca editando el anterior.
 */

public class MovimientoStock{

    private int idMovimiento;
    private final int idProducto;
    private final TipoMovimiento tipo;
    private final int cantidad;
    private final LocalDateTime fecha;
    private final String motivo;

    /**
     * Constructor para un movimiento NUEVO. La fecha la genera el propio
     * sistema en este mismo instante (RN-05: la fecha no la ingresa el
     * usuario, así que ni siquiera se recibe como parámetro).
     */

    public MovimientoStock(
        int idProducto, TipoMovimiento tipo,
        int cantidad, String motivo
    ){
    
        this.idProducto = idProducto;
        this.tipo = validarTipo(tipo);
        this.cantidad = validarCantidad(cantidad);
        this.motivo = motivo;
        this.fecha = LocalDateTime.now();
    }

    /**
     * Constructor para RECONSTRUIR un movimiento que ya existe en la base
     * de datos (acá sí se recibe la fecha real, porque no se genera de
     * nuevo: es la que ya estaba guardada).
     */

    public MovimientoStock(
        int idMovimiento, int idProducto,
        TipoMovimiento tipo, int cantidad,
        LocalDateTime fecha, String motivo
    ){

        this.idMovimiento = idMovimiento;
        this.idProducto = idProducto;
        this.tipo = validarTipo(tipo);
        this.cantidad = validarCantidad(cantidad);
        this.fecha = fecha;
        this.motivo = motivo;
    }

    // Métodos privados de validación, reutilizados por los dos constructores.
    // No están expuestos hacia afuera porque son un detalle interno.
    
    private static TipoMovimiento validarTipo(TipoMovimiento tipo){
        if (tipo == null) {
            throw new IllegalArgumentException(
                "El tipo de movimiento es obligatorio."
            );
        }
        return tipo;
    }

    private static int validarCantidad(int cantidad){
        if (cantidad <= 0){
            throw new IllegalArgumentException(
                "La cantidad de un movimiento debe ser mayor a cero."
            );
        }
        return cantidad;
    }

    // ---------- Solo getters: no hay forma de modificar un movimiento ----------
   
    public int getIdMovimiento(){
        return idMovimiento;
    }

    public void setIdMovimiento(int idMovimiento){
        this.idMovimiento = idMovimiento;
    }

    public int getIdProducto(){
        return idProducto;
    }

    public TipoMovimiento getTipo(){
        return tipo;
    }

    public int getCantidad(){
        return cantidad;
    }

     public LocalDateTime getFecha() {
        return fecha;
    }
 
    public String getMotivo() {
        return motivo;
    }
 
    @Override
    public String toString() {
        return "MovimientoStock{idMovimiento=" + idMovimiento
                + ", idProducto=" + idProducto
                + ", tipo=" + tipo
                + ", cantidad=" + cantidad
                + ", fecha=" + fecha
                + ", motivo='" + motivo + '\''
                + '}';
    }
   
}

