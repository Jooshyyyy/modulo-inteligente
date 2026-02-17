const Usuario = require("../models/usuario.model");
const Administrador = require("../models/administrador.model");

const UsuarioController = {
  perfil: async (req, res) => {
    try {
      // El middleware de auth ya nos da req.usuario con { id, rol, numero_carnet }
      const { numero_carnet, rol } = req.usuario;
      
      let usuario;
      
      if (rol === "ADMIN") {
          usuario = await Administrador.buscarPorCarnet(numero_carnet);
      } else {
          usuario = await Usuario.buscarPorCarnet(numero_carnet);
      }

      if (!usuario) {
        return res.status(404).json({ mensaje: "Usuario no encontrado" });
      }

      const response = {
        id: usuario.id,
        carnet: usuario.numero_carnet,
        email: usuario.email,
        nombre_completo: `${usuario.primer_nombre} ${usuario.apellido_paterno}`,
        rol: rol
      };
      
      // Añadir campos específicos si es cliente
      if (rol !== "ADMIN") {
          response.ocupacion = usuario.ocupacion;
          response.direccion = usuario.direccion;
          response.telefono = usuario.telefono;
      }

      res.json(response);
    } catch (error) {
      console.error(error);
      res.status(500).json({ mensaje: "Error al obtener perfil" });
    }
  },
};

module.exports = UsuarioController;
