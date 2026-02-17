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
        const query = `SELECT id, primer_nombre, apellido_paterno, email FROM administradores WHERE id = $1`;
        const result = await pool.query(query, [id]);
        return result.rows[0];
    }
};

module.exports = Administrador;
