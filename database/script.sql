CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(30) UNIQUE NOT NULL
);
INSERT INTO roles (nombre) VALUES
('ADMINISTRADOR'),
('CLIENTE');
CREATE TABLE usuarios (
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
    ocupacion VARCHAR(100),

    rol_id INT NOT NULL REFERENCES roles(id),

    estado BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO usuarios (
    primer_nombre,
    apellido_paterno,
    email,
    password_hash,
    numero_carnet,
    rol_id
)
VALUES (
    'Administrador',
    'Sistema',
    'admin@banco.com',
    '$2b$10$hash_generado',
    '0000000',
    1
);

CREATE TABLE cuentas (
    id SERIAL PRIMARY KEY,
    usuario_id INT REFERENCES usuarios(id) ON DELETE CASCADE,
    numero_cuenta VARCHAR(20) UNIQUE NOT NULL,
    tipo_cuenta VARCHAR(20), -- 'Caja de Ahorros', 'Corriente'
    saldo DECIMAL(12, 2) DEFAULT 0.00,
    moneda VARCHAR(5) DEFAULT 'BOB',
    fecha_apertura TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);