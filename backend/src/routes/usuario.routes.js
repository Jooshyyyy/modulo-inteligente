const express = require("express");
const router = express.Router();

const auth = require("../middlewares/auth.middleware");
const UsuarioController = require("../controllers/usuario.controller");

router.get("/perfil", auth, UsuarioController.perfil);
router.get("/", auth, UsuarioController.listar);
router.get("/:id", auth, UsuarioController.obtenerPorId);
router.put("/:id", auth, UsuarioController.actualizar);
router.delete("/:id", auth, UsuarioController.eliminar);

module.exports = router;
