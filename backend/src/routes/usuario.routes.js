const express = require("express");
const router = express.Router();

const auth = require("../middlewares/auth.middleware");
const UsuarioController = require("../controllers/usuario.controller");

router.get("/perfil", auth, UsuarioController.perfil);

module.exports = router;
