const pool = require('../config/database');

const Usuario = {
    // Crear un nuevo usuario (Cliente)
    crear: async (data) => {
        const query = `
            INSERT INTO usuarios (
                primer_nombre, segundo_nombre,
                apellido_paterno, apellido_materno,
                email, password_hash,
                numero_carnet, telefono,
                direccion, ocupacion,
                fecha_nacimiento
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
            RETURNING id, email, primer_nombre, apellido_paterno
        `;
        
        const values = [
            data.primer_nombre,
            data.segundo_nombre || null,
            data.apellido_paterno,
            data.apellido_materno || null,
            data.email,
            data.password_hash,
            data.numero_carnet,
            data.telefono || null,
            data.direccion || null,
            data.ocupacion || null,
            data.fecha_nacimiento
        ];

        const result = await pool.query(query, values);
        return result.rows[0];
    },

    // Buscar usuario por número de carnet
    buscarPorCarnet: async (numero_carnet) => {
        const query = `SELECT * FROM usuarios WHERE numero_carnet = $1`;
        const result = await pool.query(query, [numero_carnet]);
        return result.rows[0];
    },

    // Buscar usuario por email (opcional, útil para validaciones)
    buscarPorEmail: async (email) => {
        const query = `SELECT * FROM usuarios WHERE email = $1`;
        const result = await pool.query(query, [email]);
        return result.rows[0];
    },

    // Obtener perfil por ID
    obtenerPorId: async (id) => {
        const query = `SELECT id, primer_nombre, segundo_nombre, apellido_paterno, apellido_materno, email, numero_carnet, telefono, direccion, ocupacion, fecha_nacimiento FROM usuarios WHERE id = $1`;
        const result = await pool.query(query, [id]);
        return result.rows[0];
    },

    // Listar todos los usuarios
    listar: async () => {
        const query = `SELECT id, primer_nombre, segundo_nombre, apellido_paterno, apellido_materno, email, numero_carnet, telefono, direccion, ocupacion, fecha_nacimiento FROM usuarios ORDER BY id ASC`;
        const result = await pool.query(query);
        return result.rows;
    },

    // Actualizar usuario
    actualizar: async (id, data) => {
        const query = `
            UPDATE usuarios 
            SET primer_nombre = COALESCE($1, primer_nombre),
                segundo_nombre = COALESCE($2, segundo_nombre),
                apellido_paterno = COALESCE($3, apellido_paterno),
                apellido_materno = COALESCE($4, apellido_materno),
                email = COALESCE($5, email),
                telefono = COALESCE($6, telefono),
                direccion = COALESCE($7, direccion),
                ocupacion = COALESCE($8, ocupacion)
            WHERE id = $9
            RETURNING id, primer_nombre, apellido_paterno, email
        `;
        const values = [
            data.primer_nombre, data.segundo_nombre,
            data.apellido_paterno, data.apellido_materno,
            data.email, data.telefono, data.direccion,
            data.ocupacion, id
        ];
        const result = await pool.query(query, values);
        return result.rows[0];
    },

    // Eliminar usuario
    eliminar: async (id) => {
        const query = `DELETE FROM usuarios WHERE id = $1 RETURNING id`;
        const result = await pool.query(query, [id]);
        return result.rows[0];
    }
};

module.exports = Usuario;