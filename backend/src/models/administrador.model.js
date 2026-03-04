const pool = require('../config/database');

const Administrador = {
    // Crear un nuevo administrador
    crear: async (data) => {
        const query = `
            INSERT INTO administradores (
                primer_nombre, segundo_nombre,
                apellido_paterno, apellido_materno,
                email, password_hash,
                numero_carnet, telefono
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
            RETURNING id, email, primer_nombre
        `;
        
        const values = [
            data.primer_nombre,
            data.segundo_nombre || null,
            data.apellido_paterno,
            data.apellido_materno || null,
            data.email,
            data.password_hash,
            data.numero_carnet,
            data.telefono || null
        ];

        const result = await pool.query(query, values);
        return result.rows[0];
    },

    // Buscar administrador por número de carnet
    buscarPorCarnet: async (numero_carnet) => {
        const query = `SELECT * FROM administradores WHERE numero_carnet = $1`;
        const result = await pool.query(query, [numero_carnet]);
        return result.rows[0];
    },

    // Buscar por ID
    obtenerPorId: async (id) => {
        const query = `SELECT id, primer_nombre, segundo_nombre, apellido_paterno, apellido_materno, email, numero_carnet, telefono FROM administradores WHERE id = $1`;
        const result = await pool.query(query, [id]);
        return result.rows[0];
    },

    // Listar todos los administradores
    listar: async () => {
        const query = `SELECT id, primer_nombre, segundo_nombre, apellido_paterno, apellido_materno, email, numero_carnet, telefono FROM administradores ORDER BY id ASC`;
        const result = await pool.query(query);
        return result.rows;
    },

    // Actualizar administrador
    actualizar: async (id, data) => {
        const query = `
            UPDATE administradores 
            SET primer_nombre = COALESCE($1, primer_nombre),
                segundo_nombre = COALESCE($2, segundo_nombre),
                apellido_paterno = COALESCE($3, apellido_paterno),
                apellido_materno = COALESCE($4, apellido_materno),
                email = COALESCE($5, email),
                telefono = COALESCE($6, telefono)
            WHERE id = $7
            RETURNING id, primer_nombre, apellido_paterno, email
        `;
        const values = [
            data.primer_nombre, data.segundo_nombre,
            data.apellido_paterno, data.apellido_materno,
            data.email, data.telefono, id
        ];
        const result = await pool.query(query, values);
        return result.rows[0];
    },

    // Eliminar administrador
    eliminar: async (id) => {
        const query = `DELETE FROM administradores WHERE id = $1 RETURNING id`;
        const result = await pool.query(query, [id]);
        return result.rows[0];
    }
};

module.exports = Administrador;
