# `MovimientoStock`: registro histórico de entradas y salidas

**Archivo documentado:** `Project/control-stock/src/main/java/com/controlstock/modelo/MovimientoStock.java`  
**Paquete:** `com.controlstock.modelo`

## Rol dentro del inventario

`MovimientoStock` representa un hecho ocurrido en el inventario: una entrada o una salida de cierta cantidad de un producto, en una fecha determinada y con un motivo opcional. Es el historial que permite explicar por qué cambió el stock actual.

Un movimiento no reemplaza la cantidad disponible de `Producto`. Ambos datos cumplen funciones distintas:

| Dato | Dónde está | Pregunta que responde |
| --- | --- | --- |
| `cantidadActual` | `Producto` | ¿Cuántas unidades hay ahora? |
| Movimiento de stock | `MovimientoStock` | ¿Qué entrada o salida ocurrió, cuándo y por qué? |

Al registrar una operación, el sistema debe crear un movimiento y actualizar la cantidad del producto en la misma transacción de base de datos. Si una de las dos persistencias falla, se realiza `rollback`. De lo contrario, el historial y el valor actual pueden dejar de coincidir.

## Inmutabilidad práctica

Salvo por `idMovimiento`, los campos que describen un movimiento son `final` y no tienen setters públicos. Un campo `final` se asigna una sola vez, normalmente en el constructor, y no puede recibir otro valor después.

Esta decisión modela una regla de auditoría: un movimiento ya registrado es un hecho histórico. Si se cometió un error, no debería editarse la entrada original; se registra un movimiento compensatorio que deje evidencia de ambas operaciones.

El identificador es la excepción porque se desconoce al crear un movimiento nuevo. Tras insertar la fila, el DAO puede obtener la clave generada por MySQL y asignarla mediante `setIdMovimiento(...)`.

## Campos

| Campo | Tipo | Significado |
| --- | --- | --- |
| `idMovimiento` | `int` | Identificador único autogenerado. Comienza en `0` para objetos nuevos. |
| `idProducto` | `int` | Identificador del producto al que pertenece el hecho. |
| `tipo` | `TipoMovimiento` | Dirección: `ENTRADA` o `SALIDA`. |
| `cantidad` | `int` | Número de unidades afectadas; siempre debe ser mayor que cero. |
| `fecha` | `LocalDateTime` | Fecha y hora en que se creó o se reconstruyó el movimiento. |
| `motivo` | `String` | Texto que explica la causa, por ejemplo “Compra a proveedor” o “Venta”. |

`idProducto` es un identificador, no una referencia directa a un objeto `Producto`. Esto evita que cada movimiento cargue un producto completo y se alinea con la relación mediante clave foránea que se espera en MySQL.

## Constructores y fecha

### Movimiento nuevo

```java
MovimientoStock entrada = new MovimientoStock(
    7, TipoMovimiento.ENTRADA, 20, "Compra a proveedor"
);
```

Este constructor genera la fecha con `LocalDateTime.now()`. La interfaz no debería elegirla: así se evita que se carguen fechas arbitrarias por error o conveniencia. El identificador queda en `0` hasta que la persistencia lo asigne.

### Movimiento recuperado desde la base

```java
MovimientoStock movimiento = new MovimientoStock(
    81, 7, TipoMovimiento.SALIDA, 3,
    fechaLeida, "Venta"
);
```

Al leer un historial, no se debe generar una fecha nueva. El segundo constructor recibe la fecha guardada para reconstruir el evento real tal como ocurrió.

## Validaciones locales

Los dos constructores reutilizan métodos privados estáticos:

```java
private static TipoMovimiento validarTipo(TipoMovimiento tipo)
private static int validarCantidad(int cantidad)
```

`validarTipo` rechaza `null`; de otro modo no se sabría si el movimiento aumenta o reduce stock. `validarCantidad` rechaza cero y números negativos. El signo no se usa para expresar la dirección: la cantidad siempre es positiva y la dirección vive exclusivamente en `TipoMovimiento`. Esto evita combinaciones ambiguas como una “SALIDA de -5”.

Los métodos son `private` porque son detalles internos de construcción, y `static` porque no dependen del estado de una instancia ya creada. Se invocan desde el constructor antes de que el objeto esté completamente construido.

Actualmente `idProducto`, `fecha` y `motivo` no se validan. La capa de servicio debe garantizar que el producto exista y esté activo. Como fortalecimiento futuro, el modelo podría rechazar un identificador no positivo, una fecha nula y un motivo vacío si la regla del negocio lo exige.

## Getters y ausencia de setters

La clase permite consultar toda la información con getters, incluidos `getTipo()`, `getCantidad()` y `getFecha()`. No permite modificar producto, tipo, cantidad, fecha ni motivo una vez creado el objeto. Esto hace más segura la utilización del historial: una llamada accidental no puede cambiar silenciosamente una salida en entrada ni alterar una cantidad.

`LocalDateTime` es inmutable: tampoco puede modificarse su contenido a través del objeto retornado por `getFecha()`. Por ello no hace falta devolver una copia defensiva, como podría ocurrir con clases mutables antiguas como `java.util.Date`.

## Relación con `Producto` y `TipoMovimiento`

La secuencia lógica de una salida es:

```text
Usuario solicita una salida
        ↓
Servicio obtiene el Producto
        ↓
Producto.disminuirStock(cantidad) valida disponibilidad
        ↓
Se crea MovimientoStock con tipo SALIDA
        ↓
DAO guarda movimiento y nueva cantidad en una misma transacción
```

Para una entrada se usa `Producto.aumentarStock(...)` y `TipoMovimiento.ENTRADA`. `MovimientoStock` registra lo ocurrido, pero no llama directamente a esos métodos; coordinar objetos y persistencia es responsabilidad de `InventarioServicio`.

## Representación de texto

`toString()` devuelve una representación útil para depurar con todos los campos relevantes. No es un formato estable de exportación ni debe usarse para guardar datos. Para persistir o transmitir un movimiento se deben usar los campos individuales a través del DAO o de una futura capa de serialización.

## Observación de compilación necesaria

El archivo declara `import java.time.LocalDate;`, pero el campo, los constructores y los getters utilizan `LocalDateTime`. Para que el proyecto compile, la importación debe ser:

```java
import java.time.LocalDateTime;
```

`LocalDate` representa solamente una fecha (por ejemplo, `2026-09-10`); `LocalDateTime` agrega la hora (por ejemplo, `2026-09-10T14:30:00`). La documentación describe `LocalDateTime` porque es el tipo usado en la implementación. Esta corrección es necesaria en el código fuente, aunque no se realizó aquí porque este trabajo sólo agrega documentación.
