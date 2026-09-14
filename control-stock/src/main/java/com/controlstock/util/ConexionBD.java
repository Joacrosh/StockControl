package main.java.com.controlstock.util;

import java.io.IDEException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Clase encargada de proveer la conexión a la base de datos MySQL.
 *
 * Aplica el patrón SINGLETON: solo puede existir UNA instancia de esta
 * clase en toda la aplicación. ¿Por qué nos conviene esto acá? Porque no
 * tendría sentido que cada DAO abra su propia conexión por separado; eso
 * multiplicaría conexiones innecesariamente y sería más difícil de
 * controlar. Con el Singleton, todos los DAOs piden la conexión al MISMO
 * punto central.
 *
 * Cómo se logra en código:
 *   1) El constructor es 'private': nadie por fuera de esta clase puede
 *      escribir "new ConexionBD()".
 *   2) Guardamos la única instancia permitida en un campo estático
 *      (instancia), que pertenece a la CLASE y no a un objeto particular.
 *   3) El único modo de conseguir esa instancia es a través del método
 *      estático obtenerInstancia(), que la crea la primera vez que se
 *      la piden, y devuelve siempre la misma en los pedidos siguientes.
 */

public class ConexionBD {
    // Única instancia de la clase en toda la aplicación.
    // 'static' significa que este campo vive a nivel de la clase
    // ConexionBD, no de cada objeto: hay UN solo valor de 'instancia'
    // compartido por todo el programa.
    private static Conexion instancia;

    // Los datos de conexión NO están escritos acá adentro (hardcodeados).
    // Se leen desde application.properties en tiempo de ejecución
    // (RNF-06 del documento de requisitos: credenciales fuera del código).

    private final String url;
    private final String usuario;
    private final String password;

    /**
     * Constructor PRIVADO. Es la clave del patrón Singleton: al ser
     * privado, la única forma de crear un ConexionBD es desde adentro
     * de esta misma clase (ver obtenerInstancia() más abajo).
     *
     * Acá se cargan las credenciales desde el archivo de propiedades.
     */

    private ConexionBD() {
        Properties propiedades = new Properties();
 
        // getResourceAsStream busca el archivo dentro de
        // src/main/resources, que Maven copia al classpath al compilar
        // (ver sección 3.3 del documento de arquitectura de carpetas).
        try (InputStream entrada = ConexionBD.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
 
            if (entrada == null) {
                throw new RuntimeException(
                        "No se encontró application.properties en src/main/resources. "
                                + "¿Copiaste application.properties.example y le pusiste tus datos?");
            }
 
            propiedades.load(entrada);
 
        } catch (IOException e) {
            throw new RuntimeException("Error al leer application.properties.", e);
        }
 
        this.url = propiedades.getProperty("db.url");
        this.usuario = propiedades.getProperty("db.user");
        this.password = propiedades.getProperty("db.password");
    }

    /**
     * Punto de acceso único a la instancia de ConexionBD.
     *
     * La primera vez que se llama, 'instancia' todavía es null, así que
     * se crea. Las siguientes veces, ya existe, así que se devuelve
     * directamente la misma sin volver a leer el archivo de propiedades.
     */
    public static ConexionBD obtenerInstancia() {
        if (instancia == null) {
            instancia = new ConexionBD();
        }
        return instancia;
    }
 
    /**
     * Abre y devuelve una nueva conexión JDBC a MySQL.
     *
     * OJO con esto: el Singleton es sobre la CLASE ConexionBD (una sola
     * instancia que sabe cómo conectarse), no sobre la conexión en sí.
     * Cada vez que un DAO llama a este método recibe una Connection
     * nueva, y es responsabilidad del DAO cerrarla cuando termina de
     * usarla (idealmente con try-with-resources, como se hace acá arriba
     * con el InputStream).
     */
    public Connection obtenerConexion() throws SQLException {
        return DriverManager.getConnection(url, usuario, password);
    }
}
 

 
    







