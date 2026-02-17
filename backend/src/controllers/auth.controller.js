const bcrypt = require("bcrypt");
const jwt = require("jsonwebtoken");
const Usuario = require("../models/usuario.model");
const Administrador = require("../models/administrador.model");
const jwtConfig = require("../config/jwt");

const AuthController = {
  registro: async (req, res) => {
    try {
      const datos = req.body;
      console.log("DATOS RECIBIDOS:", datos);
      
      // Verificar si el usuario ya existe
      const existente = await Usuario.buscarPorCarnet(datos.numero_carnet);
      if (existente) {
        return res.status(400).json({ mensaje: "El número de carnet ya está registrado" });
      }

      // Encriptar contraseña
      const hash = await bcrypt.hash(datos.password, 10);

      const nuevoUsuario = await Usuario.crear({
        ...datos,
        password_hash: hash
      });

      res.status(201).json({
        mensaje: "Usuario registrado correctamente",
        usuario: {
            id: nuevoUsuario.id,
            email: nuevoUsuario.email,
            nombre: nuevoUsuario.primer_nombre,
            apellido: nuevoUsuario.apellido_paterno,
            rol: "CLIENTE"
        }
      });
    } catch (error) {
      console.error(error);
      res.status(500).json({ mensaje: "Error en el registro" });
    }
  },

  login: async (req, res) => {
    try {
      const { numero_carnet, password } = req.body;

      // 1. Buscar en Usuarios
      let usuario = await Usuario.buscarPorCarnet(numero_carnet);
      let rol = "CLIENTE";

      // 2. Si no es usuario, buscar en Administradores
      if (!usuario) {
        usuario = await Administrador.buscarPorCarnet(numero_carnet);
        rol = "ADMIN";
      }

      if (!usuario) {
        return res.status(401).json({ mensaje: "Credenciales inválidas" });
      }

      // Verificar contraseña
      const valido = await bcrypt.compare(password, usuario.password_hash);
      if (!valido) {
        return res.status(401).json({ mensaje: "Credenciales inválidas" });
      }

      const token = jwt.sign(
        {
          id: usuario.id,
          rol: rol,
          numero_carnet: usuario.numero_carnet,
        },
        jwtConfig.secret,
        { expiresIn: jwtConfig.expiresIn }
      );

      res.json({
        mensaje: "Login exitoso",
        token,
        usuario: {
          id: usuario.id,
          nombre: usuario.primer_nombre,
          apellido: usuario.apellido_paterno,
          rol: rol,
          email: usuario.email
        },
      });
    } catch (error) {
      console.error(error);
      res.status(500).json({ mensaje: "Error en el login" });
    }
  },
};

module.exports = AuthController;
