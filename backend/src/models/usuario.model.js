const pool = require('../config/database')

const Usuario = {

    crear: async (data) => {
        const query=`
        INSERT INTO usuarios (
        primer_nombre, segundo_nombre,
        apellido_paterno, apellido_materno,
        email, password_hash,
        numero_carnet, telefono,
        direccion, ocupacion, 
        rol_id
        )
        VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11)
        returning id,email
        `;
    const values=[
        data.primer_nombre,
        data.segundo_nombre,
        data.apellido_paterno,
        data.apellido_materno,
        data.email,
        data.password_hash,
        data.numero_carnet,
        data.telefono,
        data.direccion,
        data.ocupacion,
        data.rol_id
    ];

    const result = await pool.query(query, values);
    return result.rows[0];
    },

    buscarPorCarnet: async (numero_carnet) => {
    const result = await pool.query(
      `SELECT u.*, r.nombre AS rol
       FROM usuarios u
       JOIN roles r ON r.id = u.rol_id
       WHERE u.numero_carnet = $1`,
      [numero_carnet]
    );
    return result.rows[0];
  }
};

module.exports = Usuario;