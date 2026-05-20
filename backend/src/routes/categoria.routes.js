const express = require('express');
const router = express.Router();
const verificarToken = require('../middlewares/auth.middleware');
const categoriaController = require('../controllers/categoria.controller');

router.get('/', verificarToken, categoriaController.listarCategorias);

module.exports = router;
