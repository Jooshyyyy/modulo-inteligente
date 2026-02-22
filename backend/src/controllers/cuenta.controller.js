const Cuenta = require('../models/cuenta.model');
const Movimiento = require('../models/movimiento.model');

const CuentaController = {
    // Listar las cuentas del usuario autenticado (máximo 3)
    misCuentas: async (req, res) => {
        try {
            const usuario_id = req.usuario.id;
            const cuentas = await Cuenta.listarPorUsuarioId(usuario_id);
            res.json(cuentas);
        } catch (error) {
            console.error(error);
            res.status(500).json({ mensaje: "Error al obtener tus cuentas" });
        }
    },

    // Crear una nueva cuenta (Valida máximo 3)
    crear: async (req, res) => {
        try {
            const usuario_id = req.usuario.id;
            const { tipo_cuenta, moneda } = req.body;

            // 1. Validar límite de 3 cuentas
            const cantidad = await Cuenta.contarPorUsuarioId(usuario_id);
            if (cantidad >= 3) {
                return res.status(400).json({ mensaje: "Ya tienes el máximo de 3 cuentas permitidas" });
            }

            // 2. Generar número de cuenta único (simple por ahora)
            const numero_cuenta = `CTA-${Math.floor(Math.random() * 1000000000)}`;

            const nuevaCuenta = await Cuenta.crear({
                usuario_id,
                numero_cuenta,
                tipo_cuenta: tipo_cuenta || 'AHORRO',
                moneda: moneda || 'BOB',
                saldo: 0.00,
                estado: 'ACTIVA'
            });

            res.status(201).json({ mensaje: "Cuenta creada exitosamente", cuenta: nuevaCuenta });
        } catch (error) {
            console.error(error);
            res.status(500).json({ mensaje: "Error al crear la cuenta" });
        }
    },

    // Obtener saldo y datos de cuenta específica
    getSaldo: async (req, res) => {
        try {
            const { id } = req.params;
            const cuenta = await Cuenta.obtenerPorId ? await Cuenta.obtenerPorId(id) : await Cuenta.buscarPorUsuarioId(id); // Fallback for old tests
            
            if (!cuenta) return res.status(404).json({ mensaje: "Cuenta no encontrada" });
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