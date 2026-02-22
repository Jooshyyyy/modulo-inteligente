-- Agregar el estado a las cuentas
ALTER TABLE cuentas ADD COLUMN IF NOT EXISTS estado VARCHAR(20) DEFAULT 'ACTIVA';

-- Agregar campos a movimientos
ALTER TABLE movimientos 
ADD COLUMN IF NOT EXISTS tipo_transaccion VARCHAR(20), -- 'MOVIMIENTO_BANCARIO', 'QR', 'TARJETA'
ADD COLUMN IF NOT EXISTS cuenta_destino_id INT REFERENCES cuentas(id), -- Solo para TRANSFERENCIAS
ADD COLUMN IF NOT EXISTS estado VARCHAR(20) DEFAULT 'COMPLETADO', -- PENDIENTE, COMPLETADO, RECHAZADO
ADD COLUMN IF NOT EXISTS numero_transaccion VARCHAR(50), -- Número único de la transacción
ADD COLUMN IF NOT EXISTS usuario_accion_id INT REFERENCES usuarios(id);
