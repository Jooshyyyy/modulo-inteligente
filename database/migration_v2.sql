
-- Migration to split usuarios into usuarios (clients) and administradores

-- 1. Create administradores table
CREATE TABLE administradores (
    id SERIAL PRIMARY KEY,
    primer_nombre VARCHAR(50) NOT NULL,
    segundo_nombre VARCHAR(50),
    apellido_paterno VARCHAR(50) NOT NULL,
    apellido_materno VARCHAR(50),
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    numero_carnet VARCHAR(20) UNIQUE NOT NULL,
    telefono VARCHAR(20),
    direccion TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_edicion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Modify usuarios table (add columns, remove role)
-- Assuming existing usuarios are clients. If admins exist, they should be moved manually or via script if identified.
-- We will first add the new columns
ALTER TABLE usuarios ADD COLUMN fecha_nacimiento DATE;
ALTER TABLE usuarios ADD COLUMN fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE usuarios ADD COLUMN fecha_edicion TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 3. Remove rol_id and roles table dependency
-- (Optional: migrate existing admins first if any)
-- For this task, we assume we can just drop the column/constraint.
ALTER TABLE usuarios DROP COLUMN rol_id;

-- 4. Drop roles table if no longer needed
DROP TABLE IF EXISTS roles CASCADE;

-- 5. Insert default admin into new table
INSERT INTO administradores (
    primer_nombre, apellido_paterno, email, password_hash, numero_carnet, fecha_creacion
) VALUES (
    'Administrador', 'Sistema', 'admin@banco.com', '$2b$10$hash_generado', '0000000', CURRENT_TIMESTAMP
);
