const express = require("express");
const router = express.Router();

router.get("/estado", (req, res) => {
  res.json({
    ok: true,
    backend: "activo",
    timestamp: new Date(),
  });
});

module.exports = router;
