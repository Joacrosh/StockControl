package com.controlstock.modelo;

/**
 * Representa la dirección de un movimiento de stock.
 *
 * Un enum es un tipo especial de clase en Java cuyas instancias son un
 * conjunto FIJO y conocido de valores. Acá solo puede haber dos posibles
 * tipos de movimiento, nunca un tercero: por eso un enum es mejor opción
 * que, por ejemplo, un String "ENTRADA"/"SALIDA" (que permitiría por error
 * un valor como "entrada" en minúscula, o "ENTRADAA" mal tipeado).
 *
 * El compilador garantiza que TipoMovimiento solo pueda valer ENTRADA
 * o SALIDA. Esa garantía se pierde por completo si en vez de esto se
 * usa un String suelto.
 */

public enum TipoMovimiento {
    ENTRADA,
    SALIDA
}
