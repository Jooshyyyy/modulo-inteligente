const Administrador = require("../models/administrador.model");
const bcrypt = require("bcrypt");

const AdministradorController = {
    crear: async (req, res) => {
        try {
            const datos = req.body;
            
            // Verificar si el administrador ya existe
            const existente = await Administrador.buscarPorCarnet(datos.numero_carnet);
            if (existente) {
                return res.status(400).json({ mensaje: "El número de carnet ya está registrado" });
            }

            // Encriptar contraseña
            const hash = await bcrypt.hash(datos.password, 10);

            const nuevoAdmin = await Administrador.crear({
                ...datos,
                password_hash: hash
            });

            res.status(201).json({
                mensaje: "Administrador registrado correctamente",
                administrador: nuevoAdmin
            });
        } catch (error) {
            console.error(error);
            res.status(500).json({ mensaje: "Error al crear administrador" });
        }
    },
    perfil: async (req, res) => {
        try {
            const admin = await Administrador.buscarPorCarnet(req.usuario.numero_carnet);
            
            if (!admin) {
                return res.status(404).json({ mensaje: "Administrador no encontrado" });
            }

            const { password_hash, ...datosSinPassword } = admin;
            res.json({
                ...datosSinPassword,
                nombre_completo: `${admin.primer_nombre} ${admin.apellido_paterno}`
            });
        } catch (error) {
            console.error(error);
            res.status(500).json({ mensaje: "Error al obtener perfil de administrador" });
        }
    },

    listar: async (req, res) => {
        try {
            const admins = await Administrador.listar();
            const sanitizados = admins.map(a => {
                const { password_hash, ...aSinPss } = a;
                return { ...aSinPss, nombre_completo: `${a.primer_nombre} ${a.apellido_paterno}` };
            });
            res.json(sanitizados);
        } catch (error) {
            console.error(error);
            res.status(500).json({ mensaje: "Error al listar administradores" });
        }
    },

    obtenerPorId: async (req, res) => {
        try {
            const admin = await Administrador.obtenerPorId(req.params.id);
            if (!admin) {
                return res.status(404).json({ mensaje: "Administrador no encontrado" });
            }
            const { password_hash, ...aSinPss } = admin;
            res.json({ ...aSinPss, nombre_completo: `${admin.primer_nombre} ${admin.apellido_paterno}` });
        } catch (error) {
            console.error(error);
            res.status(500).json({ mensaje: "Error al obtener administrador" });
        }
    },

    actualizar: async (req, res) => {
        try {
            const admin = await Administrador.actualizar(req.params.id, req.body);
            if (!admin) {
                return res.status(404).json({ mensaje: "Administrador no encontrado" });
            }
            res.json({ mensaje: "Administrador actualizado", admin });
        } catch (error) {
            console.error(error);
            res.status(500).json({ mensaje: "Error al actualizar administrador" });
        }
    },

    eliminar: async (req, res) => {
        try {
            const result = await Administrador.eliminar(req.params.id);
            if (!result) {
                return res.status(404).json({ mensaje: "Administrador no encontrado" });
            }
            res.json({ mensaje: "Administrador eliminado" });
        } catch (error) {
            console.error(error);
            res.status(500).json({ mensaje: "Error al eliminar administrador" });
        }
    }
};

module.exports = AdministradorController;
