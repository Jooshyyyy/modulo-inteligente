const Usuario = require("../models/usuario.model");
const Administrador = require("../models/administrador.model");

const UsuarioController = {
  perfil: async (req, res) => {
    try {
      const { numero_carnet, rol } = req.usuario; // Obtenemos rol del token

      let usuario;

      if (rol === 'ADMINISTRADOR') {
        usuario = await Administrador.buscarPorCarnet(numero_carnet);
      } else {
        usuario = await Usuario.buscarPorCarnet(numero_carnet);
      }

      if (!usuario) {
        return res.status(404).json({ mensaje: "Usuario no encontrado" });
      }

      res.json({
        id: usuario.id,
        nombre: usuario.primer_nombre,
        apellido: usuario.apellido_paterno,
        carnet: usuario.numero_carnet,
        email: usuario.email,
        rol: rol, // Del token
      });
    } catch (error) {
      console.error(error);
      res.status(500).json({ mensaje: "Error al obtener perfil" });
    }
  },
};

module.exports = UsuarioController;
