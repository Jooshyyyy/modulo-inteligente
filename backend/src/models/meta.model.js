const pool = require('../config/database');

const Meta = {
    obtenerActiva: async (usuarioId) => {
        const r = await pool.query(
            `SELECT *
             FROM metas_financieras
             WHERE usuario_id = $1 AND estado = 'ACTIVA'
             ORDER BY id DESC
             LIMIT 1`,
            [usuarioId]
        );
        return r.rows[0] || null;
    },

    pausarActivas: async (usuarioId) => {
        await pool.query(
            `UPDATE metas_financieras
             SET estado = 'PAUSADA', actualizado_en = CURRENT_TIMESTAMP
             WHERE usuario_id = $1 AND estado = 'ACTIVA'`,
            [usuarioId]
        );
    },

    crear: async (usuarioId, payload) => {
        const {
            titulo,
            descripcion = null,
            plantilla = null,
            monto_objetivo,
            fecha_limite,
            monto_acumulado = 0
        } = payload;
        const r = await pool.query(
            `INSERT INTO metas_financieras (
                usuario_id, titulo, descripcion, plantilla,
                monto_objetivo, monto_acumulado, fecha_limite, estado
            ) VALUES ($1, $2, $3, $4, $5, $6, $7, 'ACTIVA')
            RETURNING *`,
            [usuarioId, titulo, descripcion, plantilla, monto_objetivo, monto_acumulado, fecha_limite]
        );
        return r.rows[0];
    },

    actualizarAcumulado: async (usuarioId, metaId, montoAcumulado) => {
        const r = await pool.query(
            `UPDATE metas_financieras
             SET monto_acumulado = $3, actualizado_en = CURRENT_TIMESTAMP
             WHERE id = $2 AND usuario_id = $1
             RETURNING *`,
            [usuarioId, metaId, montoAcumulado]
        );
        return r.rows[0] || null;
    },

    pausar: async (usuarioId, metaId) => {
        const r = await pool.query(
            `UPDATE metas_financieras
             SET estado = 'PAUSADA', actualizado_en = CURRENT_TIMESTAMP
             WHERE id = $2 AND usuario_id = $1
             RETURNING *`,
            [usuarioId, metaId]
        );
        return r.rows[0] || null;
    }
};

module.exports = Meta;
