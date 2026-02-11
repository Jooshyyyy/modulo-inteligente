const pool = require('../config/database');

const CuentaController = {
    // Obtener saldo y datos de cuenta
    getSaldo: async (req, res) => {
        try {
            const { usuario_id } = req.params;
            const query = `SELECT id, numero_cuenta, tipo_cuenta, saldo, moneda FROM cuentas WHERE usuario_id = $1 LIMIT 1`;
            const result = await pool.query(query, [usuario_id]);
            
            if (result.rows.length === 0) return res.status(404).json({ mensaje: "Sin cuenta" });
            res.json(result.rows[0]);
        } catch (error) {
            res.status(500).json({ mensaje: "Error" });
        }
    },

    // Obtener los últimos movimientos
    getMovimientos: async (req, res) => {
        try {
            const { cuenta_id } = req.params;
            const query = `SELECT concepto, monto, tipo, fecha FROM movimientos WHERE cuenta_id = $1 ORDER BY fecha DESC LIMIT 5`;
            const result = await pool.query(query, [cuenta_id]);
            res.json(result.rows);
        } catch (error) {
            res.status(500).json({ mensaje: "Error" });
        }
    }
};

module.exports = CuentaController;