const pool = require('../config/database');

const Categoria = {
    // Obtener el ID numérico que maneja BD para una categoría dada por Python ('Vivienda' -> 3)
    obtenerIdPorNombre: async (nombreCategoria) => {
        try {
            const query = `SELECT id FROM categorias WHERE nombre = $1`;
            const result = await pool.query(query, [nombreCategoria]);
            
            if (result.rows.length === 0) {
                // Failsafe: Si no encuentra categoría exacta (muy raro), se manda a 'Otros'.
                const failsafeQuery = `SELECT id FROM categorias WHERE nombre = 'Otros'`;
                const failsafeResult = await pool.query(failsafeQuery);
                return failsafeResult.rows.length > 0 ? failsafeResult.rows[0].id : null;
            }
            return result.rows[0].id;
        } catch (error) {
            console.error("Error al buscar categoría por nombre:", error);
            throw error;
        }
    }
};

module.exports = Categoria;
