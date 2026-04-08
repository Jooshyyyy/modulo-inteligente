const pool = require('../config/database');

const Prediccion = {
    obtenerPorDia: async (usuarioId, fecha) => {
        try {
            const query = `
                SELECT 
                    p.monto_proyectado,
                    p.score_confianza,
                    p.fecha_prediccion,
                    c.nombre as categoria_nombre
                FROM predicciones_gastos p
                JOIN categorias c ON p.categoria_id = c.id
                WHERE p.usuario_id = $1 AND p.fecha_prediccion = $2
                ORDER BY p.score_confianza DESC
                LIMIT 1
            `;
            const result = await pool.query(query, [usuarioId, fecha]);
            return result.rows[0];
        } catch (error) {
            console.error("Error al obtener predicción:", error);
            throw error;
        }
    }
};

module.exports = Prediccion;
