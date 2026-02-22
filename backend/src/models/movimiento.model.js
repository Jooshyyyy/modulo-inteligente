const pool = require('../config/database');

const Movimiento = {
    // Registrar un nuevo movimiento
    registrar: async (data) => {
        const query = `
            INSERT INTO movimientos (
                cuenta_id, monto, tipo, concepto, 
                tipo_transaccion, cuenta_destino_id, estado, 
                numero_transaccion, usuario_accion_id
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
            RETURNING *
        `;
        const values = [
            data.cuenta_id,
            data.monto,
            data.tipo, // 'INGRESO' o 'EGRESO'
            data.concepto,
            data.tipo_transaccion || 'MOVIMIENTO_BANCARIO',
            data.cuenta_destino_id || null,
            data.estado || 'COMPLETADO',
            data.numero_transaccion || null,
            data.usuario_accion_id || null
        ];
        const result = await pool.query(query, values);
        return result.rows[0];
    },

    // Registrar una transferencia entre cuentas (Transacción lógica)
    registrarTransferencia: async (data) => {
        const client = await pool.connect();
        try {
            await client.query('BEGIN');

            // 1. Restar de origen
            const updateOrigen = `UPDATE cuentas SET saldo = saldo - $1 WHERE id = $2 RETURNING saldo`;
            const resOrigen = await client.query(updateOrigen, [data.monto, data.cuenta_id]);
            
            if (resOrigen.rows.length === 0) throw new Error('Cuenta origen no encontrada');

            // 2. Sumar a destino
            const updateDestino = `UPDATE cuentas SET saldo = saldo + $1 WHERE id = $2 RETURNING saldo`;
            const resDestino = await client.query(updateDestino, [data.monto, data.cuenta_destino_id]);

            if (resDestino.rows.length === 0) throw new Error('Cuenta destino no encontrada');

            // 3. Registrar movimiento de egreso en origen
            const insertEgreso = `
                INSERT INTO movimientos (cuenta_id, monto, tipo, concepto, tipo_transaccion, cuenta_destino_id, estado, numero_transaccion)
                VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
            `;
            await client.query(insertEgreso, [
                data.cuenta_id, data.monto, 'EGRESO', `Transferencia a cuenta ${data.cuenta_destino_id}`, 
                data.tipo_transaccion, data.cuenta_destino_id, 'COMPLETADO', data.numero_transaccion
            ]);

            // 4. Registrar movimiento de ingreso en destino
            const insertIngreso = `
                INSERT INTO movimientos (cuenta_id, monto, tipo, concepto, tipo_transaccion, cuenta_destino_id, estado, numero_transaccion)
                VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
            `;
            await client.query(insertIngreso, [
                data.cuenta_destino_id, data.monto, 'INGRESO', `Transferencia recibida de cuenta ${data.cuenta_id}`, 
                data.tipo_transaccion, data.cuenta_id, 'COMPLETADO', data.numero_transaccion
            ]);

            await client.query('COMMIT');
            return { ok: true, nuevoSaldoOrigen: resOrigen.rows[0].saldo };
        } catch (error) {
            await client.query('ROLLBACK');
            throw error;
        } finally {
            client.release();
        }
    },

    // Obtener movimientos de un usuario (de todas sus cuentas)
    obtenerPorUsuarioId: async (usuario_id) => {
        const query = `
            SELECT m.*, c.numero_cuenta 
            FROM movimientos m
            JOIN cuentas c ON m.cuenta_id = c.id
            WHERE c.usuario_id = $1
            ORDER BY m.fecha DESC
        `;
        const result = await pool.query(query, [usuario_id]);
        return result.rows;
    },

    // Obtener movimientos de una específica
    obtenerPorCuentaId: async (cuenta_id, limite = 10) => {
        const query = `SELECT * FROM movimientos WHERE cuenta_id = $1 ORDER BY fecha DESC LIMIT $2`;
        const result = await pool.query(query, [cuenta_id, limite]);
        return result.rows;
    }
};

module.exports = Movimiento;
