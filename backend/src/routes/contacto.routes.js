const express = require('express');
const router = express.Router();
const contactoController = require('../controllers/contacto.controller');

// Crear un contacto
router.post('/', contactoController.crearContacto);

// Obtener todos los contactos de un usuario
router.get('/usuario/:usuario_id', contactoController.obtenerContactos);

// Actualizar un contacto
router.put('/:id', contactoController.actualizarContacto);

// Eliminar un contacto
router.delete('/:id', contactoController.eliminarContacto);

module.exports = router;
