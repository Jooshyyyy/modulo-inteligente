const express = require('express');
const router = express.Router();
const CuentaController = require('../controllers/cuenta.controller');

const auth = require("../middlewares/auth.middleware");

router.get('/mis-cuentas', auth, CuentaController.misCuentas);
router.post('/crear', auth, CuentaController.crear);
router.get('/mi-saldo/:id', auth, CuentaController.getSaldo);

module.exports = router;