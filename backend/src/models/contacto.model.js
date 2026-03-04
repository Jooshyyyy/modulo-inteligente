const pool = require('../config/database');

const Contacto = {
    // Crear un nuevo contacto
    crear: async (data) => {
        const query = `
            INSERT INTO contactos (
                usuario_id, nombre, alias, cuenta_bancaria, 
                nombre_banco, moneda, estado
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7)
            RETURNING *
        `;
        const values = [
            data.usuario_id,
            data.nombre,
            data.alias || null,
            data.cuenta_bancaria,
            data.nombre_banco,
            data.moneda || 'BOB',
            data.estado || 'ACTIVO'
        ];
        const result = await pool.query(query, values);
        return result.rows[0];
    },

    // Obtener todos los contactos de un usuario
    obtenerPorUsuarioId: async (usuario_id) => {
        const query = `
            SELECT * FROM contactos 
            WHERE usuario_id = $1 
            ORDER BY nombre ASC
        `;
        const result = await pool.query(query, [usuario_id]);
        return result.rows;
    },

    // Actualizar un contacto
    actualizar: async (id, data) => {
        // Construir query dinámicamente para actualizar solo campos proporcionados
        const fields = [];
        const values = [];
        let index = 1;

        if (data.nombre) { fields.push(`nombre = $${index++}`); values.push(data.nombre); }
        if (data.alias !== undefined) { fields.push(`alias = $${index++}`); values.push(data.alias); }
        if (data.cuenta_bancaria) { fields.push(`cuenta_bancaria = $${index++}`); values.push(data.cuenta_bancaria); }
        if (data.nombre_banco) { fields.push(`nombre_banco = $${index++}`); values.push(data.nombre_banco); }
        if (data.moneda) { fields.push(`moneda = $${index++}`); values.push(data.moneda); }
        if (data.estado) { fields.push(`estado = $${index++}`); values.push(data.estado); }

        if (fields.length === 0) return null;

        values.push(id);
        const query = `
            UPDATE contactos 
            SET ${fields.join(', ')} 
            WHERE id = $${index} 
            RETURNING *
        `;
        
        const result = await pool.query(query, values);
        return result.rows[0];
    },

    // Eliminar un contacto
    eliminar: async (id) => {
        const query = 'DELETE FROM contactos WHERE id = $1 RETURNING *';
        const result = await pool.query(query, [id]);
        return result.rows[0];
    }
};

module.exports = Contacto;
