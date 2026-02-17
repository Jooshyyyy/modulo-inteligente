const Cuenta = require('../models/cuenta.model');
const Movimiento = require('../models/movimiento.model');

const CuentaController = {
    // Obtener saldo y datos de cuenta
    getSaldo: async (req, res) => {
        try {
            const { usuario_id } = req.params;
            const cuenta = await Cuenta.buscarPorUsuarioId(usuario_id);
            
            if (!cuenta) return res.status(404).json({ mensaje: "Sin cuenta" });
            res.json(cuenta);
        } catch (error) {
            console.error(error);
            res.status(500).json({ mensaje: "Error al obtener cuenta" });
        }
    },

    // Obtener los últimos movimientos
    getMovimientos: async (req, res) => {
        try {
            const { cuenta_id } = req.params;
            const movimientos = await Movimiento.obtenerPorCuentaId(cuenta_id);
            res.json(movimientos);
        } catch (error) {
            console.error(error);
            res.status(500).json({ mensaje: "Error al obtener movimientos" });
        }
    }
};

module.exports = CuentaController;