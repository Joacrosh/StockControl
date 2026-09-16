# `TipoMovimiento`: conjunto cerrado de direcciones de stock

**Archivo documentado:** `Project/control-stock/src/main/java/com/controlstock/modelo/TipoMovimiento.java`  
**Paquete:** `com.controlstock.modelo`

## Qué representa

`TipoMovimiento` es un `enum` con dos únicos valores:

```java
public enum TipoMovimiento {
    ENTRADA,
    SALIDA
}
```

Define la dirección de un cambio en el inventario. Una `ENTRADA` incorpora unidades y una `SALIDA` las retira. Esta información no se modela como un texto libre porque el dominio sólo admite esas dos alternativas.

## Por qué se usa `enum` y no `String`

Con un `String`, cada llamador podría proporcionar cualquier texto:

```java
String tipo = "entrada";
String otroTipo = "ENTRADAA";
```

El compilador no impediría esos errores. En cambio, un constructor que recibe `TipoMovimiento` sólo acepta constantes válidas:

```java
new MovimientoStock(12, TipoMovimiento.ENTRADA, 5, "Reposición");
```

No compila si se intenta pasar un texto directamente. Esta garantía es una forma de seguridad de tipos: el error se detecta al compilar, antes de ejecutar el programa o llegar a la base de datos.

En Python puede pensarse en `enum.Enum`; en C, se parece a un `enum`, aunque Java ofrece una versión más rica porque un `enum` es un tipo de clase. Puede tener métodos, campos, constructor privado y comportamiento propio si el proyecto lo necesita en el futuro.

## Uso en la lógica de inventario

El `enum` no actualiza stock por sí mismo. Lo utiliza `MovimientoStock` para describir un registro histórico y la capa de servicio para seleccionar la operación correspondiente sobre `Producto`:

```java
switch (movimiento.getTipo()) {
    case ENTRADA -> producto.aumentarStock(movimiento.getCantidad());
    case SALIDA -> producto.disminuirStock(movimiento.getCantidad());
}
```

Esta separación mantiene responsabilidades claras: `TipoMovimiento` enumera opciones permitidas, `MovimientoStock` guarda cuál se eligió, `Producto` protege su cantidad y el servicio coordina la operación completa.

## Comparación y control de flujo

Las constantes de un `enum` son instancias únicas. Por eso se pueden comparar con `==` de manera segura:

```java
if (tipo == TipoMovimiento.SALIDA) {
    // aplicar una disminución
}
```

Esto difiere de `String`, cuyos valores normalmente se comparan con `.equals(...)`, no con `==`. Usar `==` con cadenas compara referencias y puede producir resultados inesperados; con enums es correcto y habitual.

Los `enum` funcionan naturalmente con `switch`, y el compilador ayuda a detectar casos no considerados. Como actualmente hay sólo dos valores, la lógica de negocio puede tratar explícitamente ambas alternativas.

## Persistencia en MySQL

Al guardar un movimiento, el DAO debe elegir una representación consistente para la columna de tipo. Las opciones usuales son almacenar el nombre de la constante (`ENTRADA` o `SALIDA`) como texto, o usar un `ENUM` de MySQL con esos mismos valores. Al leerlo, se puede reconstruir con:

```java
TipoMovimiento tipo = TipoMovimiento.valueOf(valorLeido);
```

`valueOf` distingue mayúsculas de minúsculas y lanza una excepción si el texto no corresponde a una constante. Por eso la base debe conservar exactamente los nombres definidos por el enum, o el DAO debe convertir y validar explícitamente.

No conviene guardar `ordinal()` —la posición numérica de la constante— porque cambiar el orden o insertar una nueva constante haría que datos anteriores cambien de significado.

## Posibles ampliaciones

Si el dominio necesitara más variantes, por ejemplo `AJUSTE_POSITIVO`, `AJUSTE_NEGATIVO` o `DEVOLUCION`, se agregarían como nuevas constantes. Antes de hacerlo habría que actualizar también las reglas de servicio, las restricciones de la base y todo `switch` que tome decisiones según el tipo.

Para la versión actual, mantener sólo `ENTRADA` y `SALIDA` expresa correctamente el alcance del sistema y reduce estados inválidos.
