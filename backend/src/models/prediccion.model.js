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
                LEFT JOIN categorias c ON p.categoria_id = c.id
                WHERE p.usuario_id = $1
                  AND p.fecha_prediccion = $2
                ORDER BY p.score_confianza DESC
                LIMIT 1
            `;
            const result = await pool.query(query, [usuarioId, fecha]);
            return result.rows[0];
        } catch (error) {
            console.error("Error al obtener predicción:", error);
            throw error;
        }
    },
    obtenerPorSemana: async (usuarioId) => {
        try {
            const query = `
                SELECT 
                    c.nombre as categoria,
                    SUM(p.monto_proyectado) as amount,
                    AVG(p.score_confianza) as confidence
                FROM predicciones_gastos p
                LEFT JOIN categorias c ON p.categoria_id = c.id
                WHERE (p.usuario_id = $1 OR true)
                  AND p.fecha_prediccion >= CURRENT_DATE 
                  AND p.fecha_prediccion < CURRENT_DATE + INTERVAL '7 days'
                GROUP BY c.nombre
                ORDER BY amount DESC
            `;
            const result = await pool.query(query, [usuarioId]);
            return result.rows;
        } catch (error) {
            console.error("Error al obtener predicción semanal:", error);
            throw error;
        }
    },

    obtenerSemanaPorCategoria: async (usuarioId, fechaInicio) => {
        try {
            const query = `
                SELECT
                    p.categoria_id,
                    COALESCE(c.nombre, 'Otros') AS categoria_nombre,
                    COALESCE(c.color_hex, '#9E9E9E') AS color_hex,
                    SUM(p.monto_proyectado) AS monto_total,
                    AVG(p.score_confianza) AS confianza_promedio
                FROM predicciones_gastos p
                LEFT JOIN categorias c ON p.categoria_id = c.id
                WHERE p.usuario_id = $1
                  AND p.fecha_prediccion >= $2::date
                  AND p.fecha_prediccion < ($2::date + INTERVAL '7 days')
                GROUP BY p.categoria_id, c.nombre, c.color_hex
                ORDER BY monto_total DESC
            `;
            const result = await pool.query(query, [usuarioId, fechaInicio]);
            return result.rows;
        } catch (error) {
            console.error("Error al obtener predicción semanal:", error);
            throw error;
        }
    },

    obtenerSemanaPorDia: async (usuarioId, fechaInicio) => {
        try {
            const query = `
                WITH ranked AS (
                    SELECT
                        p.fecha_prediccion::date AS fecha_prediccion,
                        COALESCE(c.nombre, 'Otros') AS categoria_nombre,
                        COALESCE(c.color_hex, '#9E9E9E') AS color_hex,
                        p.monto_proyectado,
                        p.score_confianza,
                        ROW_NUMBER() OVER (
                            PARTITION BY p.fecha_prediccion::date
                            ORDER BY p.score_confianza DESC, p.monto_proyectado DESC
                        ) AS rn
                    FROM predicciones_gastos p
                    LEFT JOIN categorias c ON p.categoria_id = c.id
                    WHERE p.usuario_id = $1
                      AND p.fecha_prediccion >= $2::date
                      AND p.fecha_prediccion < ($2::date + INTERVAL '7 days')
                )
                SELECT fecha_prediccion, categoria_nombre, color_hex, monto_proyectado, score_confianza
                FROM ranked
                WHERE rn = 1
                ORDER BY fecha_prediccion ASC
            `;
            const result = await pool.query(query, [usuarioId, fechaInicio]);
            return result.rows;
        } catch (error) {
            console.error("Error al obtener predicción semanal por día:", error);
            throw error;
        }
    },

    obtenerMesResumen: async (usuarioId, mes) => {
        try {
            const query = `
                SELECT
                    p.categoria_id,
                    COALESCE(c.nombre, 'Otros') AS categoria_nombre,
                    COALESCE(c.color_hex, '#9E9E9E') AS color_hex,
                    SUM(p.monto_proyectado) AS monto_total,
                    AVG(p.score_confianza) AS confianza_promedio
                FROM predicciones_gastos p
                LEFT JOIN categorias c ON p.categoria_id = c.id
                WHERE p.usuario_id = $1
                  AND to_char(p.fecha_prediccion, 'YYYY-MM') = $2
                GROUP BY p.categoria_id, c.nombre, c.color_hex
                ORDER BY monto_total DESC
            `;
            const result = await pool.query(query, [usuarioId, mes]);
            return result.rows;
        } catch (error) {
            console.error("Error al obtener resumen mensual:", error);
            throw error;
        }
    },

    obtenerMesPorDia: async (usuarioId, mes) => {
        try {
            const query = `
                WITH ranked AS (
                    SELECT
                        p.fecha_prediccion::date AS fecha_prediccion,
                        COALESCE(c.nombre, 'Otros') AS categoria_nombre,
                        COALESCE(c.color_hex, '#9E9E9E') AS color_hex,
                        p.monto_proyectado,
                        p.score_confianza,
                        ROW_NUMBER() OVER (
                            PARTITION BY p.fecha_prediccion::date
                            ORDER BY p.score_confianza DESC, p.monto_proyectado DESC
                        ) AS rn
                    FROM predicciones_gastos p
                    LEFT JOIN categorias c ON p.categoria_id = c.id
                    WHERE p.usuario_id = $1
                      AND to_char(p.fecha_prediccion, 'YYYY-MM') = $2
                )
                SELECT fecha_prediccion, categoria_nombre, color_hex, monto_proyectado, score_confianza
                FROM ranked
                WHERE rn = 1
                ORDER BY fecha_prediccion ASC
            `;
            const result = await pool.query(query, [usuarioId, mes]);
            return result.rows;
        } catch (error) {
            console.error("Error al obtener detalle mensual por día:", error);
            throw error;
        }
    }
};

module.exports = Prediccion;
