const pool = require('../config/database');

const Cuenta = {
    // Crear nueva cuenta
    crear: async (data) => {
        const query = `
            INSERT INTO cuentas (
                usuario_id, numero_cuenta, tipo_cuenta, saldo, moneda
            )
            VALUES ($1, $2, $3, $4, $5)
            RETURNING *
        `;
        const values = [
            data.usuario_id,
            data.numero_cuenta,
            data.tipo_cuenta,
            data.saldo || 0.00,
            data.moneda || 'BOB'
        ];
        const result = await pool.query(query, values);
        return result.rows[0];
    },

    // Buscar cuenta por Usuario ID
    buscarPorUsuarioId: async (usuario_id) => {
        const query = `SELECT * FROM cuentas WHERE usuario_id = $1`;
        const result = await pool.query(query, [usuario_id]);
        return result.rows[0]; // Asume una cuenta por usuario por ahora, o devuelve la primera
    },

    // Buscar por número de cuenta
    buscarPorNumero: async (numero_cuenta) => {
        const query = `SELECT * FROM cuentas WHERE numero_cuenta = $1`;
        const result = await pool.query(query, [numero_cuenta]);
        return result.rows[0];
    },

    // Actualizar saldo
    actualizarSaldo: async (id, nuevoSaldo) => {
        const query = `UPDATE cuentas SET saldo = $1, fecha_edicion = CURRENT_TIMESTAMP WHERE id = $2 RETURNING saldo`;
        const result = await pool.query(query, [nuevoSaldo, id]);
        return result.rows[0];
    }
};

module.exports = Cuenta;
