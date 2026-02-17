const express = require('express');
const router = express.Router();
const CuentaController = require('../controllers/cuenta.controller');

router.get('/mi-saldo/:usuario_id', CuentaController.getSaldo);
router.get('/mis-movimientos/:cuenta_id', CuentaController.getMovimientos);

module.exports = router;