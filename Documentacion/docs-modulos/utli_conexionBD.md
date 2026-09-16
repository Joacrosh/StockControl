# Módulo util.ConexionBD

## Sistema de Control de Stock — Documentación técnica

Fecha: 10 de septiembre de 2026
Paquete: `com.controlstock.util`
Clase: `ConexionBD`

## 1. Propósito

Este módulo es el único punto de la aplicación responsable de abrir conexiones JDBC contra la base de datos MySQL. Ninguna otra clase del sistema (DAO, servicio o UI) debería crear una conexión por su cuenta: todas pasan por `ConexionBD.obtenerInstancia().obtenerConexion()`.

Centralizar esto en un solo lugar cumple dos objetivos que ya estaban definidos en el documento de Requisitos y Arquitectura:

- RNF-06 (seguridad básica): las credenciales de conexión no viven en el código fuente, sino en un archivo de configuración externo que no se versiona.
- Arquitectura en capas: la capa DAO depende de esta utilidad para obtener su conexión, pero nunca construye la cadena de conexión ni maneja las credenciales directamente.

## 2. Contexto: qué es JDBC

JDBC (Java Database Connectivity) es la API estándar de Java para comunicarse con bases de datos relacionales. El código Java nunca le habla directamente a MySQL: le habla a JDBC, y JDBC delega en un driver específico del motor de base de datos que se esté usando. En este proyecto, ese driver es `mysql-connector-j`, declarado como dependencia en el `pom.xml`.

Las piezas de JDBC utilizadas en este módulo son:

| Elemento | Rol |
|---|---|
| `DriverManager` | Clase de utilidad (métodos estáticos) que localiza el driver adecuado y abre la conexión física contra la base. |
| `Connection` | Representa una conexión abierta y activa. Es el objeto que después reciben los DAO para ejecutar sentencias SQL. |
| `SQLException` | Excepción que JDBC lanza cuando falla cualquier operación contra la base (credenciales inválidas, base caída, SQL inválido, etc.). |

## 3. Patrón de diseño aplicado: Singleton

`ConexionBD` implementa el patrón Singleton, que garantiza que exista una única instancia de la clase en toda la aplicación.

Justificación: no tendría sentido que cada DAO leyera el archivo de configuración y mantuviera su propia copia de las credenciales por separado. Con el Singleton, todos los DAO piden las credenciales al mismo punto central, que las lee una sola vez.

Mecanismo de implementación:

1. El constructor es `private`. Ninguna clase externa puede escribir `new ConexionBD()`.
2. Existe un campo estático `instancia` que guarda la única instancia permitida. Al ser `static`, pertenece a la clase y no a un objeto en particular: hay un solo valor compartido por todo el programa.
3. El método estático `obtenerInstancia()` es el único camino para conseguir esa instancia: la crea la primera vez que se la piden y devuelve siempre la misma en los pedidos siguientes.

Aclaración importante: el Singleton aplica sobre el objeto `ConexionBD` (que centraliza la lectura de credenciales), no sobre la conexión JDBC en sí. Cada llamada a `obtenerConexion()` devuelve una `Connection` nueva. Es responsabilidad de quien la use (normalmente un DAO) cerrarla cuando termina, idealmente con try-with-resources.

## 4. Manejo de configuración

Las credenciales se leen desde `application.properties`, un archivo ubicado en `src/main/resources` que Maven copia automáticamente al classpath durante la compilación. Esto permite acceder a él desde código con `getResourceAsStream(...)`, sin necesidad de una ruta absoluta en el sistema de archivos.

El archivo versionado en el repositorio es una plantilla sin datos reales:

```
db.url=jdbc:mysql://localhost:3306/control_stock
db.user=TU_USUARIO
db.password=TU_PASSWORD
```

Cada integrante del equipo debe copiar este archivo como `application.properties` (sin el sufijo `.example`) en la misma carpeta, y completar sus propios datos de conexión local. Ese archivo con credenciales reales está listado en `.gitignore`, por lo que nunca se sube al repositorio.

## 5. Recorrido del código

### 5.1 Campos de la clase

```java
private static ConexionBD instancia;
private final String url;
private final String usuario;
private final String password;
```

`instancia` guarda la única instancia del Singleton. Los tres campos `final` guardan los datos de conexión leídos una sola vez, en el momento de construcción, y no cambian después.

### 5.2 Constructor privado

```java
private ConexionBD() {
    Properties propiedades = new Properties();

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
```

Funcionamiento paso a paso:

1. `ConexionBD.class.getClassLoader()` obtiene el cargador de clases de la JVM, que sabe dónde están los recursos del proyecto compilado.
2. `getResourceAsStream("application.properties")` busca ese archivo en el classpath y devuelve un flujo de lectura sobre él, o `null` si no lo encuentra.
3. El bloque está escrito como try-with-resources: el recurso declarado entre paréntesis (`entrada`) se cierra automáticamente al salir del bloque, sin necesidad de un `finally` explícito.
4. Si `entrada` es `null` (el archivo no existe), se lanza una excepción con un mensaje explícito. Sin este chequeo, el error resultante más adelante sería un `NullPointerException` sin ninguna pista de la causa real.
5. `propiedades.load(entrada)` interpreta el contenido del archivo (líneas `clave=valor`) y lo carga en el objeto `Properties`, que funciona como un diccionario de texto a texto.
6. Finalmente se extraen las tres claves esperadas y se asignan a los campos de la instancia.

Nota sobre el manejo de excepciones: `IOException` es una excepción de tipo *checked*, lo que significa que el compilador obliga a manejarla explícitamente (con `try/catch` o declarándola con `throws`). Esto aplica a operaciones que dependen de factores externos al programa, como la existencia de un archivo en disco. Al no poder declarar `throws` en un constructor sin afectar a quien lo invoca de forma indirecta (el método estático `obtenerInstancia()`), se optó por capturar la excepción y relanzarla como `RuntimeException`, que es *unchecked* y no obliga a quien llama a manejarla explícitamente.

### 5.3 Método de acceso al Singleton

```java
public static ConexionBD obtenerInstancia() {
    if (instancia == null) {
        instancia = new ConexionBD();
    }
    return instancia;
}
```

La primera vez que se invoca este método, `instancia` todavía vale `null`, por lo que se crea una nueva instancia (ejecutando el constructor descrito arriba, incluida la lectura del archivo de propiedades). En cualquier invocación posterior, `instancia` ya tiene un valor, así que se devuelve directamente sin volver a leer el archivo.

### 5.4 Apertura de la conexión

```java
public Connection obtenerConexion() throws SQLException {
    return DriverManager.getConnection(url, usuario, password);
}
```

`DriverManager.getConnection(...)` intenta abrir una conexión real contra MySQL usando la URL y las credenciales guardadas. Esta llamada puede fallar por distintos motivos: el servidor de base de datos no está corriendo, las credenciales son incorrectas, la base indicada en la URL no existe, o el driver JDBC no está disponible en el classpath. En cualquiera de esos casos se lanza `SQLException`, que también es *checked*: por eso el método la declara con `throws SQLException` en lugar de capturarla, delegando la decisión de cómo manejarla a quien invoque este método (típicamente, un DAO).

## 6. Forma de uso esperada

```java
try (Connection conexion = ConexionBD.obtenerInstancia().obtenerConexion()) {
    // acá se ejecutan las sentencias SQL correspondientes
} catch (SQLException e) {
    // manejo del error de conexión o de ejecución
}
```

Cada DAO obtiene su propia `Connection` a través de esta clase, la usa dentro de un bloque try-with-resources para garantizar su cierre, y maneja `SQLException` según corresponda a la operación que esté realizando.

## 7. Requisitos previos para que el módulo funcione

Antes de poder probar `obtenerConexion()` con éxito es necesario:

1. Tener un servidor MySQL corriendo de forma local.
2. Haber creado la base de datos indicada en `db.url` (por ejemplo, `CREATE DATABASE control_stock;`). No es necesario tener las tablas creadas todavía: esta clase solo abre la conexión, no depende del esquema.
3. Haber copiado `application.properties.example` como `application.properties` en `src/main/resources`, con los datos reales del usuario y contraseña de MySQL local.
4. Tener declarada la dependencia `mysql-connector-j` en el `pom.xml`, de lo contrario `DriverManager` no va a encontrar un driver capaz de interpretar la URL `jdbc:mysql://...`.

## 8. Relación con el resto de la arquitectura

Según la arquitectura en capas definida en el documento de Requisitos y Arquitectura (sección 9), `ConexionBD` pertenece a la capa `util` y es utilizada exclusivamente por la capa `dao`. La capa de servicio y la interfaz de usuario nunca deberían importar ni referenciar esta clase directamente: acceden a los datos siempre a través de los DAO correspondientes.
