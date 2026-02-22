const express = require("express");
const router = express.Router();

const auth = require("../middlewares/auth.middleware");
const AdministradorController = require("../controllers/administrador.controller");

router.get("/perfil", auth, AdministradorController.perfil);
router.get("/", auth, AdministradorController.listar);
router.get("/:id", auth, AdministradorController.obtenerPorId);
router.put("/:id", auth, AdministradorController.actualizar);
router.delete("/:id", auth, AdministradorController.eliminar);

module.exports = router;
