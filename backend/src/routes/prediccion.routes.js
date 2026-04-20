const express = require('express');
const router = express.Router();
const verificarToken = require('../middlewares/auth.middleware');
const prediccionController = require('../controllers/prediccion.controller');

router.get('/dia', verificarToken, prediccionController.obtenerPrediccionDia);
router.get('/semana', verificarToken, prediccionController.obtenerPrediccionSemana);
router.get('/semanal', verificarToken, prediccionController.obtenerPrediccionSemanal);
router.get('/mensual', verificarToken, prediccionController.obtenerPrediccionMensual);

module.exports = router;
