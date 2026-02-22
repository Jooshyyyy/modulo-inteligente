const Movimiento = require('../models/movimiento.model');
const Cuenta = require('../models/cuenta.model');

const MovimientoController = {
    // Crear un movimiento (INGRESO, EGRESO, o TRANSFERENCIA)
    crear: async (req, res) => {
        try {
            const { cuenta_id, monto, tipo, concepto, tipo_transaccion, cuenta_destino_id } = req.body;
            const usuario_id = req.usuario.id;

            // 1. Validar que la cuenta origen pertenezca al usuario autenticado
            // Obtenemos la cuenta por su ID (o número si prefieres, pero aquí usamos ID de DB)
            const cuentasUsuario = await Cuenta.listarPorUsuarioId(usuario_id);
            const cuentaPropia = cuentasUsuario.find(c => c.id === parseInt(cuenta_id));

            if (!cuentaPropia) {
                return res.status(403).json({ mensaje: "No tienes permiso sobre esta cuenta o la cuenta no existe" });
            }

            if (tipo === 'TRANSFERENCIA') {
                if (!cuenta_destino_id) {
                    return res.status(400).json({ mensaje: "Se requiere una cuenta de destino para transferencias" });
                }
                
                // Validar que no sea la misma cuenta
                if (parseInt(cuenta_id) === parseInt(cuenta_destino_id)) {
                    return res.status(400).json({ mensaje: "No puedes transferir a la misma cuenta" });
                }

                // Validar saldo suficiente
                if (parseFloat(cuentaPropia.saldo) < parseFloat(monto)) {
                    return res.status(400).json({ mensaje: "Saldo insuficiente" });
                }

                const result = await Movimiento.registrarTransferencia({
                    cuenta_id,
                    cuenta_destino_id,
                    monto,
                    tipo_transaccion: tipo_transaccion || 'MOVIMIENTO_BANCARIO',
                    numero_transaccion: `TRX-${Date.now()}`
                });

                return res.json({ mensaje: "Transferencia realizada con éxito", nuevoSaldo: result.nuevoSaldoOrigen });
            }

            // Para INGRESO o EGRESO simple
            if (tipo === 'EGRESO' && parseFloat(cuentaPropia.saldo) < parseFloat(monto)) {
                return res.status(400).json({ mensaje: "Saldo insuficiente para el retiro" });
            }

            const movimiento = await Movimiento.registrar({
                cuenta_id,
                monto,
                tipo,
                concepto: concepto || (tipo === 'INGRESO' ? 'Depósito' : 'Retiro'),
                tipo_transaccion: tipo_transaccion || 'MOVIMIENTO_BANCARIO',
                usuario_accion_id: usuario_id,
                numero_transaccion: `TRX-${Date.now()}`
            });

            // Actualizar saldo en la cuenta
            const nuevoSaldo = tipo === 'INGRESO' ? (parseFloat(cuentaPropia.saldo) + parseFloat(monto)) : (parseFloat(cuentaPropia.saldo) - parseFloat(monto));
            await Cuenta.actualizarSaldo(cuentaPropia.id, nuevoSaldo);

            res.status(201).json({ mensaje: "Movimiento registrado", movimiento, nuevoSaldo });
        } catch (error) {
            console.error(error);
            res.status(500).json({ mensaje: "Error al procesar movimiento" });
        }
    },

    // Listar movimientos del usuario
    listar: async (req, res) => {
        try {
            const usuario_id = req.usuario.id;
            const movimientos = await Movimiento.obtenerPorUsuarioId(usuario_id);
            res.json(movimientos);
        } catch (error) {
            console.error(error);
            res.status(500).json({ mensaje: "Error al listar movimientos" });
        }
    }
};

module.exports = MovimientoController;
