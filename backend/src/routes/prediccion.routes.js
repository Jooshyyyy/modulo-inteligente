const express = require('express');
const router = express.Router();
const verificarToken = require('../middlewares/auth.middleware');
const prediccionController = require('../controllers/prediccion.controller');

router.get('/dia', verificarToken, prediccionController.obtenerPrediccionDia);
<<<<<<< HEAD
router.get('/semana', verificarToken, prediccionController.obtenerPrediccionSemana);
=======
router.get('/semanal', verificarToken, prediccionController.obtenerPrediccionSemanal);
router.get('/mensual', verificarToken, prediccionController.obtenerPrediccionMensual);
>>>>>>> 799f6e6ae5ec037d2af0f8d4ba89853c260b5b0b

module.exports = router;
