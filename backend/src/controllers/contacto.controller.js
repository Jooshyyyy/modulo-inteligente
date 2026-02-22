const Contacto = require('../models/contacto.model');

const contactoController = {
    // Crear un nuevo contacto
    crearContacto: async (req, res) => {
        try {
            const { usuario_id, nombre, cuenta_bancaria, nombre_banco } = req.body;

            if (!usuario_id || !nombre || !cuenta_bancaria || !nombre_banco) {
                return res.status(400).json({ 
                    message: 'usuario_id, nombre, cuenta_bancaria y nombre_banco son obligatorios' 
                });
            }

            const nuevoContacto = await Contacto.crear(req.body);
            res.status(201).json({
                message: 'Contacto creado exitosamente',
                contacto: nuevoContacto
            });
        } catch (error) {
            console.error('Error al crear contacto:', error);
            res.status(500).json({ message: 'Error interno del servidor' });
        }
    },

    // Obtener contactos de un usuario
    obtenerContactos: async (req, res) => {
        try {
            const { usuario_id } = req.params;
            const contactos = await Contacto.obtenerPorUsuarioId(usuario_id);
            res.json(contactos);
        } catch (error) {
            console.error('Error al obtener contactos:', error);
            res.status(500).json({ message: 'Error interno del servidor' });
        }
    },

    // Actualizar un contacto
    actualizarContacto: async (req, res) => {
        try {
            const { id } = req.params;
            const contactoActualizado = await Contacto.actualizar(id, req.body);

            if (!contactoActualizado) {
                return res.status(404).json({ message: 'Contacto no encontrado' });
            }

            res.json({
                message: 'Contacto actualizado exitosamente',
                contacto: contactoActualizado
            });
        } catch (error) {
            console.error('Error al actualizar contacto:', error);
            res.status(500).json({ message: 'Error interno del servidor' });
        }
    },

    // Eliminar un contacto
    eliminarContacto: async (req, res) => {
        try {
            const { id } = req.params;
            const contactoEliminado = await Contacto.eliminar(id);

            if (!contactoEliminado) {
                return res.status(404).json({ message: 'Contacto no encontrado' });
            }

            res.json({ message: 'Contacto eliminado exitosamente' });
        } catch (error) {
            console.error('Error al eliminar contacto:', error);
            res.status(500).json({ message: 'Error interno del servidor' });
        }
    }
};

module.exports = contactoController;
