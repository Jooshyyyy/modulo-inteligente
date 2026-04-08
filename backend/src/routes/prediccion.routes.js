const express = require('express');
const router = express.Router();
const verificarToken = require('../middlewares/auth.middleware');
const prediccionController = require('../controllers/prediccion.controller');

router.get('/dia', verificarToken, prediccionController.obtenerPrediccionDia);

module.exports = router;
