const Administrador = require("../models/administrador.model");

const AdministradorController = {
    perfil: async (req, res) => {
        try {
            const admin = await Administrador.buscarPorCarnet(req.usuario.numero_carnet);
            
            if (!admin) {
                return res.status(404).json({ mensaje: "Administrador no encontrado" });
            }

            res.json({
                id: admin.id,
                carnet: admin.numero_carnet,
                email: admin.email,
                nombre_completo: `${admin.primer_nombre} ${admin.apellido_paterno}`
            });
        } catch (error) {
            console.error(error);
            res.status(500).json({ mensaje: "Error al obtener perfil de administrador" });
        }
    }
};

module.exports = AdministradorController;
