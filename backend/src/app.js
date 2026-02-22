const express = require("express");
const cors = require("cors");

const app = express();

app.use(cors());
app.use(express.json());

// rutas auth
const authRoutes = require("./routes/auth.routes");
app.use("/api/auth", authRoutes);

// rutas usuario (perfil)
const usuarioRoutes = require("./routes/usuario.routes");
app.use("/api/usuarios", usuarioRoutes);

// rutas administrador
const administradorRoutes = require("./routes/administrador.routes");
app.use("/api/administradores", administradorRoutes);

// rutas test
const testRoutes = require("./routes/test.routes");
app.use("/api/test", testRoutes);

// rutas cuentas
const cuentaRoutes = require("./routes/cuenta.routes");
app.use("/api/cuentas", cuentaRoutes);

// rutas movimientos
const movimientoRoutes = require("./routes/movimiento.routes");
app.use("/api/movimientos", movimientoRoutes);

// rutas contactos
const contactoRoutes = require("./routes/contacto.routes");
app.use("/api/contactos", contactoRoutes);

module.exports = app;
