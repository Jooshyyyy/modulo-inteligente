const pool = require('../config/database');

const Movimiento = {
    // Registrar un nuevo movimiento
    registrar: async (data) => {
        const query = `
            INSERT INTO movimientos (
                cuenta_id, monto, tipo, concepto
            )
            VALUES ($1, $2, $3, $4)
            RETURNING *
        `;
        const values = [
            data.cuenta_id,
            data.monto,
            data.tipo, // 'INGRESO' o 'EGRESO'
            data.concepto
        ];
        const result = await pool.query(query, values);
        return result.rows[0];
    },

    // Obtener movimientos de una cuenta
    obtenerPorCuentaId: async (cuenta_id, limite = 10) => {
        const query = `
            SELECT * FROM movimientos 
            WHERE cuenta_id = $1 
            ORDER BY fecha DESC 
            LIMIT $2
        `;
        const result = await pool.query(query, [cuenta_id, limite]);
        return result.rows;
    }
};

module.exports = Movimiento;
