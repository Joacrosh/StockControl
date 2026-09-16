# `Producto`: estado y reglas locales del inventario

**Archivo documentado:** `Project/control-stock/src/main/java/com/controlstock/modelo/Producto.java`  
**Paquete:** `com.controlstock.modelo`

## Propósito

`Producto` representa un artículo inventariable: sus datos comerciales, la categoría a la que pertenece, su precio y su disponibilidad. Es la entidad central del control de stock porque guarda la cantidad actual y protege las operaciones que pueden aumentarla o disminuirla.

La clase modela reglas que pueden decidirse usando únicamente el propio producto, como “el precio no puede ser negativo” o “no se puede retirar más cantidad que la disponible”. No realiza SQL ni registra por sí sola un historial de movimientos. La capa `InventarioServicio` debe coordinar esta clase con el DAO y con `MovimientoStock`.

## Atributos

| Campo | Tipo | Significado |
| --- | --- | --- |
| `idProducto` | `int` | Clave autogenerada por la base de datos. Vale `0` mientras el producto es nuevo. |
| `codigo` | `String` | Código de negocio que identifica al producto ante el usuario. Debe existir y no estar vacío. |
| `nombre` | `String` | Nombre descriptivo obligatorio. |
| `descripcion` | `String` | Información opcional adicional. Puede ser `null`. |
| `idCategoria` | `int` | Identificador de la categoría relacionada. Equivale a una clave foránea en la base de datos. |
| `precio` | `double` | Precio del producto. No puede ser negativo. |
| `cantidadActual` | `int` | Unidades disponibles en inventario. |
| `stockMinimo` | `int` | Umbral desde el cual se considera que existe una alerta de bajo stock. |
| `activo` | `boolean` | Estado lógico del producto. Permite conservar su historial tras una baja. |

Como los campos son `private`, la única forma pública de alterar los datos es mediante los métodos que la clase expone. Es especialmente relevante para `cantidadActual`: no existe un `setCantidadActual(...)`, porque asignar un número arbitrario rompería el vínculo entre el stock y los movimientos registrados.

## Dos formas de construir un producto

### Producto nuevo

```java
Producto arroz = new Producto(
    "ARR-001", "Arroz", "Arroz largo fino",
    2, 1200.0, 30, 10
);
```

Este constructor recibe una `cantidadInicial`, la asigna a `cantidadActual` y activa el producto. El identificador no se recibe: MySQL lo asignará cuando el DAO lo inserte. La cantidad inicial puede ser cero, pero no negativa.

El valor inicial es una excepción deliberada al flujo habitual de stock. Una vez creado el producto, los cambios posteriores de cantidad deben realizarse mediante `aumentarStock(...)` o `disminuirStock(...)`, normalmente como resultado de un movimiento.

### Producto reconstruido desde persistencia

El constructor con nueve parámetros se utiliza al leer una fila existente. Recibe tanto `idProducto` como `cantidadActual` y `activo`, porque debe reproducir el estado exacto guardado en la base. Un DAO puede usarlo conceptualmente así:

```java
Producto producto = new Producto(
    id, codigo, nombre, descripcion, categoriaId,
    precio, cantidadActual, stockMinimo, activo
);
```

No es una puerta para que la UI edite libremente el stock: es un mecanismo de reconstrucción del modelo a partir de datos persistidos.

## Reglas de negocio implementadas

### Stock bajo: `tieneStockBajo()`

```java
return cantidadActual < stockMinimo;
```

Un producto está en alerta únicamente cuando su cantidad actual es menor que su mínimo. Si ambos valores son iguales, todavía no se considera bajo según la regla actual. Por ejemplo, con `cantidadActual = 10` y `stockMinimo = 10`, el resultado es `false`; con `cantidadActual = 9`, es `true`.

El resultado se calcula en cada llamada y no se guarda en un campo separado. Esto evita inconsistencias como tener `cantidadActual = 3` y un supuesto campo `stockBajo = false` desactualizado. Es una propiedad derivada, no un dato independiente.

### Ingreso de unidades: `aumentarStock(int cantidad)`

El método rechaza cantidades menores o iguales a cero con `IllegalArgumentException`. Si el valor es positivo, suma esa cantidad a `cantidadActual`.

```java
arroz.aumentarStock(15); // 30 pasa a 45
```

El método no pregunta el motivo ni persiste el cambio. El servicio debe crear el movimiento de entrada, modificar el producto y pedir al DAO que guarde ambos cambios dentro de una misma transacción.

### Egreso de unidades: `disminuirStock(int cantidad)`

El método aplica dos validaciones, en este orden:

1. La cantidad solicitada debe ser mayor que cero.
2. La cantidad no puede superar `cantidadActual`.

Si la segunda condición falla, lanza `IllegalStateException`, ya que el problema no es el tipo del argumento sino que el estado actual del producto no permite realizar la operación. Si hay 5 unidades y se intenta retirar 8, el objeto no se modifica y comunica el stock disponible en el mensaje de error.

```java
arroz.disminuirStock(4); // 45 pasa a 41
```

Esta protección es una defensa local importante: incluso si una capa superior olvida validar el stock, el producto no permite que la cantidad se vuelva negativa.

## Validación de propiedades editables

Los setters de `codigo` y `nombre` rechazan `null`, la cadena vacía y cadenas compuestas solamente por espacios mediante `isBlank()`. `setPrecio(...)` y `setStockMinimo(...)` rechazan números negativos. El precio cero y el stock mínimo cero son válidos para la implementación actual.

`setDescripcion(...)` y `setIdCategoria(...)` no agregan validación. Por tanto, una evolución recomendable es verificar que el identificador de categoría sea positivo y decidir si una descripción nula debe convertirse en cadena vacía o mantenerse como ausencia de valor.

La clase proporciona `setIdProducto(...)` para que el DAO asigne la clave generada después de un `INSERT`. No expone setter para `activo`; si se implementa la baja lógica prevista por el proyecto, será necesario añadir una operación controlada —por ejemplo, `desactivar()`— o un setter con un contrato claro.

## Dinero y `double`

El precio está modelado como `double`, apropiado para una primera versión didáctica. Sin embargo, `double` representa números binarios en punto flotante y puede producir pequeñas imprecisiones decimales, como resultados visualmente parecidos a `0.30000000000000004`.

Para cálculos monetarios reales, la opción habitual en Java es `BigDecimal`, construido desde texto o con `BigDecimal.valueOf(...)`, y una columna decimal en MySQL. No es necesario cambiarlo para entender el modelo actual, pero es una decisión relevante si se agregan totales, descuentos o impuestos.

## Ejemplo de uso en el flujo correcto

```java
Producto producto = obtenerProductoDesdeDao("ARR-001");
int cantidad = 4;

producto.disminuirStock(cantidad);
MovimientoStock salida = new MovimientoStock(
    producto.getIdProducto(), TipoMovimiento.SALIDA, cantidad, "Venta"
);

// El servicio coordina la persistencia de ambos en una transacción.
```

El orden real de las operaciones y el `commit` deben quedar bajo control de la capa de servicio y del DAO. Si se inserta el movimiento pero falla la actualización de `cantidadActual`, ambas acciones deben revertirse para que el historial y el stock sigan siendo consistentes.

## `toString()`

La redefinición de `toString()` facilita inspeccionar un producto al imprimirlo. Muestra identificador, código, nombre, cantidad actual, mínimo y estado. No muestra precio, descripción ni categoría, probablemente para mantener el mensaje corto; no es una representación completa ni un formato pensado para el usuario final.
