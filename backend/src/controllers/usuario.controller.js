const Usuario = require("../models/usuario.model");

const UsuarioController = {
  perfil: async (req, res) => {
    try {
      const usuario = await Usuario.buscarPorCarnet(req.usuario.numero_carnet);

      if (!usuario) {
        return res.status(404).json({ mensaje: "Usuario no encontrado" });
      }

      res.json({
        carnet: usuario.numero_carnet,
        email: usuario.email,
        rol: usuario.rol,
      });
    } catch (error) {
      console.error(error);
      res.status(500).json({ mensaje: "Error al obtener perfil" });
    }
  },
};

module.exports = UsuarioController;
