const Usuario = require("../models/usuario.model");
const Administrador = require("../models/administrador.model");

const UsuarioController = {
  perfil: async (req, res) => {
    try {
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
      // No devolver el password_hash
      const { password_hash, ...datosSinPassword } = usuario;
      res.json({
          ...datosSinPassword,
          nombre_completo: `${usuario.primer_nombre} ${usuario.apellido_paterno}`
      });
    } catch (error) {
      console.error(error);
      res.status(500).json({ mensaje: "Error al obtener perfil" });
    }
  },

  listar: async (req, res) => {
    try {
      const usuarios = await Usuario.listar();
      const sanitizados = usuarios.map(u => {
          const { password_hash, ...uSinPss } = u;
          return { ...uSinPss, nombre_completo: `${u.primer_nombre} ${u.apellido_paterno}` };
      });
      res.json(sanitizados);
    } catch (error) {
      console.error(error);
      res.status(500).json({ mensaje: "Error al listar usuarios" });
    }
  },

  obtenerPorId: async (req, res) => {
    try {
      const usuario = await Usuario.obtenerPorId(req.params.id);
      if (!usuario) {
        return res.status(404).json({ mensaje: "Usuario no encontrado" });
      }
      const { password_hash, ...uSinPss } = usuario;
      res.json({ ...uSinPss, nombre_completo: `${usuario.primer_nombre} ${usuario.apellido_paterno}` });
    } catch (error) {
      console.error(error);
      res.status(500).json({ mensaje: "Error al obtener usuario" });
    }
  },

  actualizar: async (req, res) => {
    try {
      const usuario = await Usuario.actualizar(req.params.id, req.body);
      if (!usuario) {
        return res.status(404).json({ mensaje: "Usuario no encontrado" });
      }
      res.json({ mensaje: "Usuario actualizado", usuario });
    } catch (error) {
      console.error(error);
      res.status(500).json({ mensaje: "Error al actualizar usuario" });
    }
  },

  eliminar: async (req, res) => {
    try {
      const result = await Usuario.eliminar(req.params.id);
      if (!result) {
        return res.status(404).json({ mensaje: "Usuario no encontrado" });
      }
      res.json({ mensaje: "Usuario eliminado" });
    } catch (error) {
      console.error(error);
      res.status(500).json({ mensaje: "Error al eliminar usuario" });
    }
  }
};

module.exports = UsuarioController;
