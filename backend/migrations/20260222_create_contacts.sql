-- Migración para crear la tabla de contactos
CREATE TABLE IF NOT EXISTS contactos (
    id SERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    nombre VARCHAR(255) NOT NULL,
    alias VARCHAR(100),
    cuenta_bancaria VARCHAR(50) NOT NULL,
    nombre_banco VARCHAR(100) NOT NULL,
    moneda VARCHAR(10) DEFAULT 'BOB',
    estado VARCHAR(20) DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_edicion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Trigger para actualizar fecha_edicion automáticamente
CREATE OR REPLACE FUNCTION update_fecha_edicion_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.fecha_edicion = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_contactos_fecha_edicion
    BEFORE UPDATE ON contactos
    FOR EACH ROW
    EXECUTE PROCEDURE update_fecha_edicion_column();
