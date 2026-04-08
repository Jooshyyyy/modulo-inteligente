const Prediccion = require('../models/prediccion.model');

const obtenerPrediccionDia = async (req, res) => {
    try {
        const usuarioId = req.usuario.id;
        const { fecha } = req.query;
        
        if (!fecha) {
            return res.status(400).json({ mensaje: "Fecha es requerida (YYYY-MM-DD)" });
        }

        let predDb = await Prediccion.obtenerPorDia(usuarioId, fecha);
        
        if (!predDb) {
            return res.status(404).json({ mensaje: "No hay predicción disponible para esta fecha." });
        }

        const total = parseFloat(predDb.monto_proyectado);
        const mainCategory = predDb.categoria_nombre || "Categoría General";
        const porcentajeConfianza = Math.floor(parseFloat(predDb.score_confianza) * 100);
        
        let nivel = "Gasto moderado esperado";
        if (total > 200) nivel = "Gasto alto esperado";
        else if (total < 50) nivel = "Gasto ligero esperado";

        const response = {
            total: total.toFixed(2),
            nivel: nivel,
            diferenciaPrevia: "Analizado por IA",
            probabilidades: [
                { 
                    nombre: `${mainCategory}`, 
                    hora: "Durante el día", 
                    porcentaje: porcentajeConfianza, 
                    monto: total.toFixed(2) 
                }
            ]
        };

        res.json(response);
    } catch (e) {
        console.error("Controller Error:", e);
        res.status(500).json({ mensaje: "Error al obtener predicciones."});
    }
}

module.exports = { obtenerPrediccionDia };
