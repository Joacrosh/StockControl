-- ============================================================
-- Sistema de Control de Stock
-- Esquema inicial para MySQL 8
-- ============================================================

CREATE DATABASE IF NOT EXISTS control_stock
    CHARACTER SET utf8mb4
    COLLATE utf8MB4_0900_ai_ci;

USE control_stock;

-- ============================================================
-- Tabla: categorias
-- Agrupa los productos por rubro comercial.
-- La baja se realiza de forma lógica mediante el campo activo.
-- ============================================================

CREATE TABLE categorias (
    id_categoria INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    nombre VARCHAR(100) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_categorias_nombre UNIQUE (nombre),

    CONSTRAINT chk_categorias_nombre_no_vacio
        CHECK (CHAR_LENGTH(TRIM(nombre)) > 0)
) ENGINE = InnoDB;



-- ============================================================
-- Tabla: productos
-- Contiene los datos comerciales y el stock disponible actual.
--
-- cantidad_actual se conserva para consultar stock rápidamente.
-- Solamente debe cambiarse como parte de una transacción que
-- también inserte un movimiento de stock.
-- ============================================================


CREATE TABLE productos (
    id_producto INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT NULL,

    id_categoria INT UNSIGNED NOT NULL,

    precio DECIMAL(12, 2) NOT NULL,
    cantidad_actual INT UNSIGNED NOT NULL DEFAULT 0,
    stock_minimo INT UNSIGNED NOT NULL DEFAULT 0,

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_productos_codigo UNIQUE (codigo),

    CONSTRAINT chk_productos_codigo_no_vacio
        CHECK (CHAR_LENGTH(TRIM(codigo)) > 0),

    CONSTRAINT chk_productos_nombre_no_vacio
        CHECK (CHAR_LENGTH(TRIM(nombre)) > 0),

    CONSTRAINT chk_productos_precio_no_negativo
        CHECK (precio >= 0),

    CONSTRAINT chk_productos_cantidad_actual_no_negativa
        CHECK (cantidad_actual >= 0),

    CONSTRAINT chk_productos_stock_minimo_no_negativo
        CHECK (stock_minimo >= 0),

    CONSTRAINT fk_productos_categoria
        FOREIGN KEY (id_categoria)
        REFERENCES categorias(id_categoria)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    INDEX idx_productos_categoria (id_categoria),
    INDEX idx_productos_activo_stock (activo, cantidad_actual, stock_minimo)
) ENGINE = InnoDB;

-- ============================================================
-- Tabla: movimientos_stock
-- Historial inmutable de cada entrada o salida de mercadería.
--
-- La cantidad siempre es positiva. El sentido del movimiento lo
-- determina la columna tipo, no el signo de cantidad.
-- ============================================================

CREATE TABLE movimientos_stock (
    id_movimiento INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    id_producto INT UNSIGNED NOT NULL,

    tipo ENUM('ENTRADA', 'SALIDA') NOT NULL,
    cantidad INT UNSIGNED NOT NULL,

    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    motivo VARCHAR(255) NULL,

    CONSTRAINT chk_movimientos_cantidad_positiva
        CHECK (cantidad > 0),

    CONSTRAINT fk_movimientos_producto
        FOREIGN KEY (id_producto)
        REFERENCES productos(id_producto)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    INDEX idx_movimientos_producto_fecha (id_producto, fecha)
) ENGINE = InnoDB;