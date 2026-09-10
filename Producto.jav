package modelo;

public class Producto {

    //1. atributos privados

    private int id;
    private String nombre;
    private double precio;
    private int cantidadStock;

    //2. Constructor (crea un producto nuevo)

    public Producto (int id, String nombre, double precio, int cantidadStock ){

        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.cantidadStock = cantidadStock;}

 //3. Getters y Setters (para leer o modificar los datos)

 public int getId(){
            return id;

            public void setId(int id){
                this.id = id;

            }
        }

        public String getNombre(){
            return nombre;

            public void setNombre(String nombre){
                this.nombre = nombre;

            }
        }
    }

         public double getPrecio(){
            return precio;

            public void setPrecio(double precio){
                this.precio = precio;

            }

     public int getCantidadStock(){
        return cantidadStock;

        public void setCantidadStock(int cantidadStock){
            this.cantidadStock = cantidadStock;

        }
     } 
     
     //Un metodo util para mostrar los datos del producto

     @Override // sobreescribimos el metodo toString de la clase Object

     public String toString(){
        return "ID: " + id + " | Nombre: " + nombre + " | Precio: " + precio + " | Cantidad en Stock: " + cantidadStock;
     }

     


}