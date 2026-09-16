# `Categoria`: representación de un rubro de productos

**Archivo documentado:** `Project/control-stock/src/main/java/com/controlstock/modelo/Categoria.java`  
**Paquete:** `com.controlstock.modelo`

## Responsabilidad dentro del sistema

`Categoria` representa un rubro comercial al que se pueden asociar productos, por ejemplo, “Bebidas”, “Limpieza” o “Almacén”. Su objetivo es agrupar productos con un significado de negocio; no almacena productos ni consulta la base de datos.

Según la arquitectura del proyecto, esta clase pertenece a la capa de modelo. Por ello debe contener el estado de una categoría y las validaciones que le corresponden a esa entidad. No debe contener sentencias SQL, recibir datos de consola ni decidir cómo se registra una categoría. Esas responsabilidades pertenecen al DAO, a la capa de servicio y a la interfaz, respectivamente.

En Python sería comparable a una clase que concentra atributos y métodos de validación. En C, se parece a un `struct` junto con funciones que controlan cómo se modifica ese `struct`. Java permite reunir ambas partes en una sola clase y ocultar sus datos internos.

## Estado que conserva el objeto

| Campo | Tipo | Propósito |
| --- | --- | --- |
| `idCategoria` | `int` | Identificador único asignado por la base de datos. Una categoría nueva comienza con el valor `0`. |
| `nombre` | `String` | Nombre visible del rubro. Es obligatorio y no puede estar vacío. |
| `activo` | `boolean` | Indica si la categoría puede seguir utilizándose. |

Los tres campos son `private`. Esto implementa encapsulamiento: otro objeto no puede ejecutar `categoria.nombre = "..."` como podría hacerse con un atributo público. Debe usar métodos públicos de la clase. La ventaja no es solamente ocultar datos: permite que la clase valide cada cambio antes de aceptar un valor inválido.

## Ciclo de vida e identificador

Una categoría nueva se crea con:

```java
Categoria bebidas = new Categoria("Bebidas");
```

En ese momento, todavía no existe una fila asociada en MySQL. El identificador conserva el valor por defecto de un `int`, que es `0`, y la categoría nace activa. Más adelante, el DAO inserta la fila en la tabla y MySQL genera el identificador mediante `AUTO_INCREMENT`. El DAO recupera ese identificador y llama a `setIdCategoria(...)` para completar el mismo objeto Java.

Cuando una categoría ya está almacenada, el DAO la reconstruye con el segundo constructor. En lugar de generar datos nuevos, reproduce el estado de una fila existente:

```java
Categoria categoria = new Categoria(3, "Bebidas", true);
```

Esta diferencia entre “crear” y “reconstruir” es importante. El primer constructor expresa las reglas para una alta nueva; el segundo permite convertir datos leídos de la persistencia en un objeto del modelo.

## Constructores

### `Categoria(String nombre)`

Representa una categoría que aún no fue persistida. Ejecuta `setNombre(nombre)` para reutilizar la validación y asigna `true` a `activo`.

No recibe un identificador porque este no debería ser elegido por la interfaz ni por la capa de servicio: corresponde a la base de datos asegurar su unicidad.

### `Categoria(int idCategoria, String nombre, boolean activo)`

Representa una categoría existente. Recibe el identificador y el estado que ya estaban guardados. Aun en este caso se usa `setNombre(nombre)`, por lo cual el objeto no puede reconstruirse con un nombre nulo, vacío o compuesto solamente por espacios.

## Acceso y modificación de los datos

Los métodos `getIdCategoria()`, `getNombre()` e `isActivo()` devuelven el estado actual. La forma `isActivo()` es la convención habitual de Java para un getter booleano; equivale conceptualmente a una pregunta: “¿está activa?”.

`setIdCategoria(int idCategoria)` está pensado para uso de la capa DAO después de un `INSERT`. No valida el valor recibido. En el flujo previsto esto es aceptable porque el valor proviene de la clave autogenerada por MySQL, aunque una evolución posible sería rechazar valores no positivos para reforzar el contrato.

`setActivo(boolean activo)` permite cambiar el estado. Es útil para implementar una baja lógica: se conserva la categoría y sus referencias históricas, pero se puede impedir que sea elegida para nuevos productos. El modelo no decide cuándo corresponde desactivarla; esa decisión pertenece al servicio.

## Validación del nombre

El método `setNombre(String nombre)` aplica esta condición:

```java
if (nombre == null || nombre.isBlank()) {
    throw new IllegalArgumentException(...);
}
```

`null` significa que no hay ningún objeto `String`. `isBlank()` devuelve `true` tanto para `""` como para una cadena formada únicamente por espacios, tabulaciones o saltos de línea. Por lo tanto, valores como los siguientes se rechazan:

```java
new Categoria(null);
new Categoria("");
new Categoria("   ");
```

`IllegalArgumentException` es una excepción estándar de Java que comunica que el argumento suministrado por quien llamó al método no cumple el contrato. Es una excepción no comprobada (`unchecked`): el compilador no obliga a capturarla, pero la interfaz o el servicio deben decidir cómo traducirla a un mensaje adecuado para el usuario.

En Python, una validación equivalente podría lanzar `ValueError`. En C, normalmente se debería devolver un código de error y verificarlo manualmente en cada llamada.

## Representación para depuración

La clase redefine `toString()`. Este método se ejecuta automáticamente cuando se imprime una instancia:

```java
System.out.println(bebidas);
```

En vez de una representación técnica poco útil, como `Categoria@1b6d3586`, debería devolver los valores relevantes de la categoría. Esto resulta útil al depurar, registrar mensajes o inspeccionar colecciones.

La implementación actual construye el texto manualmente. Conviene revisar el formato si se modifica la clase: falta el signo `=` antes de `nombre` y falta una comilla de apertura, por lo que la salida puede verse como `nombreBebidas'`. Es un detalle de presentación, no afecta el estado ni la lógica de negocio.

## Relación con las demás capas

El flujo esperado es el siguiente:

```text
UI solicita una categoría
        ↓
Servicio decide si la operación es válida
        ↓
CategoriaDAO / CategoriaDAOImpl persiste o recupera datos
        ↓
Categoria representa cada fila como objeto Java
```

La clase no conoce al DAO ni a MySQL. Esa independencia permite crear y probar una `Categoria` sin iniciar una base de datos:

```java
Categoria limpieza = new Categoria("Limpieza");
assert limpieza.isActivo();
assert limpieza.getIdCategoria() == 0;
```

## Límites actuales y extensiones posibles

La clase asegura que el nombre no sea vacío, pero no verifica unicidad. Dos objetos Java podrían construirse con el mismo nombre. La regla de que no haya categorías duplicadas debe aplicarse en el servicio y reforzarse en la base de datos con una restricción `UNIQUE`.

Tampoco contiene una lista de productos. El vínculo se expresa del lado de `Producto`, mediante su `idCategoria`. Esto mantiene el modelo sencillo y evita cargar todos los productos cada vez que se consulta una categoría.
