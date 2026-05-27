const pool = require('../config/database');

const Presupuesto = {
    obtenerActivo: async (usuarioId) => {
        const r = await pool.query(
            `SELECT *
             FROM planes_presupuesto
             WHERE usuario_id = $1 AND estado = 'ACTIVA'
             ORDER BY id DESC
             LIMIT 1`,
            [usuarioId]
        );
        return r.rows[0] || null;
    },

    obtenerLimitesPorPlan: async (planId) => {
        const r = await pool.query(
            `SELECT l.categoria_id, l.tope_mensual, COALESCE(c.nombre, 'Otros') AS categoria_nombre
             FROM presupuesto_categoria_limites l
             LEFT JOIN categorias c ON l.categoria_id = c.id
             WHERE l.plan_id = $1
             ORDER BY l.tope_mensual DESC`,
            [planId]
        );
        return r.rows;
    },

    pausarActivos: async (usuarioId) => {
        await pool.query(
            `UPDATE planes_presupuesto
             SET estado = 'PAUSADO', actualizado_en = CURRENT_TIMESTAMP
             WHERE usuario_id = $1 AND estado = 'ACTIVA'`,
            [usuarioId]
        );
    },

    crear: async (usuarioId, payload) => {
        const { titulo, tipo, tope_mensual = null, limites = [] } = payload;
        const client = await pool.connect();
        try {
            await client.query('BEGIN');
            await client.query(
                `UPDATE planes_presupuesto
                 SET estado = 'PAUSADO', actualizado_en = CURRENT_TIMESTAMP
                 WHERE usuario_id = $1 AND estado = 'ACTIVA'`,
                [usuarioId]
            );
            const ins = await client.query(
                `INSERT INTO planes_presupuesto (usuario_id, titulo, tipo, tope_mensual, estado)
                 VALUES ($1, $2, $3, $4, 'ACTIVA')
                 RETURNING *`,
                [usuarioId, titulo, tipo, tope_mensual]
            );
            const plan = ins.rows[0];
            for (const lim of limites) {
                await client.query(
                    `INSERT INTO presupuesto_categoria_limites (plan_id, categoria_id, tope_mensual)
                     VALUES ($1, $2, $3)
                     ON CONFLICT (plan_id, categoria_id)
                     DO UPDATE SET tope_mensual = EXCLUDED.tope_mensual`,
                    [plan.id, lim.categoria_id, lim.tope_mensual]
                );
            }
            await client.query('COMMIT');
            return plan;
        } catch (e) {
            await client.query('ROLLBACK');
            throw e;
        } finally {
            client.release();
        }
    },

    pausar: async (usuarioId, planId) => {
        const r = await pool.query(
            `UPDATE planes_presupuesto
             SET estado = 'PAUSADO', actualizado_en = CURRENT_TIMESTAMP
             WHERE id = $2 AND usuario_id = $1
             RETURNING *`,
            [usuarioId, planId]
        );
        return r.rows[0] || null;
    }
};

module.exports = Presupuesto;
