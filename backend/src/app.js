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

// rutas test
const testRoutes = require("./routes/test.routes");
app.use("/api/test", testRoutes);

module.exports = app;
