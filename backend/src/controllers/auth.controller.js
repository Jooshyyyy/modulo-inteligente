const bcrypt = require("bcrypt");
const jwt = require("jsonwebtoken");
const Usuario = require("../models/usuario.model");
const jwtConfig = require("../config/jwt");

const AuthController = {
  registro: async (req, res) => {
    try {
      const datos = req.body;

      // Verificar si el usuario ya existe
      const existente = await Usuario.buscarPorCarnet(datos.numero_carnet);
      if (existente) {
        return res
          .status(400)
          .json({ mensaje: "El número de carnet ya está registrado" });
      }

      // Encriptar contraseña
      const hash = await bcrypt.hash(datos.password, 10);

      const nuevoUsuario = await Usuario.crear({
        ...datos,
        password_hash: hash,
        rol_id: datos.rol_id || 2, // CLIENTE por defecto
      });

      res.status(201).json({
        mensaje: "Usuario registrado correctamente",
        usuario: nuevoUsuario,
      });
    } catch (error) {
      console.error(error);
      res.status(500).json({ mensaje: "Error en el registro" });
    }
  },

  login: async (req, res) => {
    try {
      const { numero_carnet, password } = req.body;

      const usuario = await Usuario.buscarPorCarnet(numero_carnet);
      if (!usuario) {
        return res.status(401).json({ mensaje: "Credenciales inválidas" });
      }

      const valido = await bcrypt.compare(password, usuario.password_hash);
      if (!valido) {
        return res.status(401).json({ mensaje: "Credenciales inválidas" });
      }

      const token = jwt.sign(
        {
          id: usuario.id,
          rol: usuario.rol,
          numero_carnet: usuario.numero_carnet, // Agrega esto
        },
        jwtConfig.secret,
        { expiresIn: jwtConfig.expiresIn },
      );

      res.json({
        mensaje: "Login exitoso",
        token,
        usuario: {
          id: usuario.id,
          nombre: usuario.primer_nombre,
          rol: usuario.rol,
        },
      });
    } catch (error) {
      console.error(error);
      res.status(500).json({ mensaje: "Error en el login" });
    }
  },
};

module.exports = AuthController;
