const express = require('express');
const router = express.Router();
const verificarToken = require('../middlewares/auth.middleware');
const metaController = require('../controllers/meta.controller');

router.get('/activa', verificarToken, metaController.obtenerMetaActiva);
router.get('/ia-coach', verificarToken, metaController.obtenerIaCoach);
router.post('/', verificarToken, metaController.crearMeta);
router.put('/:id/progreso', verificarToken, metaController.actualizarProgreso);
router.put('/:id/pausar', verificarToken, metaController.pausarMeta);

module.exports = router;
