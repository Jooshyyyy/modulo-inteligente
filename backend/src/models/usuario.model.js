const pool = require('../config/database')

const Usuario = {

  crear: async (data) => {
    const query = `
        INSERT INTO usuarios (
        primer_nombre, segundo_nombre,
        apellido_paterno, apellido_materno,
        email, password_hash,
        numero_carnet, telefono,
        direccion, ocupacion, 
        fecha_nacimiento
        )
        VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11)
        returning id, email, primer_nombre, apellido_paterno, numero_carnet
        `;
    const values = [
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
      data.fecha_nacimiento
    ];

    const result = await pool.query(query, values);
    return result.rows[0];
  },

  buscarPorCarnet: async (numero_carnet) => {
    const result = await pool.query(
      `SELECT * FROM usuarios WHERE numero_carnet = $1`,
      [numero_carnet]
    );
    return result.rows[0];
  }
};

module.exports = Usuario;