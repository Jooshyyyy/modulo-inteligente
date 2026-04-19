const express = require('express');
const router = express.Router();
const verificarToken = require('../middlewares/auth.middleware');
const prediccionController = require('../controllers/prediccion.controller');

router.get('/dia', verificarToken, prediccionController.obtenerPrediccionDia);
router.get('/semana', verificarToken, prediccionController.obtenerPrediccionSemana);

module.exports = router;
