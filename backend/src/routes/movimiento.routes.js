const express = require('express');
const router = express.Router();
const MovimientoController = require('../controllers/movimiento.controller');
const auth = require("../middlewares/auth.middleware");

router.post('/crear', auth, MovimientoController.crear);
router.get('/', auth, MovimientoController.listar);

module.exports = router;
