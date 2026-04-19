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
const obtenerPrediccionSemana = async (req, res) => {
    try {
        const usuarioId = req.usuario.id;
        
        let prediccionesDB = await Prediccion.obtenerPorSemana(usuarioId);
        
        if (!prediccionesDB || prediccionesDB.length === 0) {
            return res.status(404).json({ mensaje: "No hay predicción disponible para esta semana." });
        }

        let totalSuma = 0;
        let totalConfianza = 0;

        const predictedExpenses = prediccionesDB.map((item, index) => {
            const amount = parseFloat(item.amount);
            const conf = Math.floor(parseFloat(item.confidence) * 100);
            
            totalSuma += amount;
            totalConfianza += conf;

            let icon = "otros";
            const catLower = (item.categoria || "").toLowerCase();
            if (catLower.includes("alimentaci") || catLower.includes("comida")) icon = "alimentacion";
            else if (catLower.includes("transporte") || catLower.includes("movilidad")) icon = "transporte";
            else if (catLower.includes("compra") || catLower.includes("supermercado")) icon = "compras";
            else if (catLower.includes("vivienda") || catLower.includes("hogar")) icon = "vivienda";
            else if (catLower.includes("entretenimiento")) icon = "entretenimiento";

            return {
                category: item.categoria || "Otros",
                description: "Predicción sugerida",
                amount: amount,
                icon: icon,
                time: "Semana", 
                confidence: conf
            };
        });

        const avgConfidence = Math.floor(totalConfianza / predictedExpenses.length);
        
        let status = 'medium';
        if (avgConfidence > 85) status = 'high';
        else if (avgConfidence < 60) status = 'low';

        // Estructura de json requerida
        const response = {
            predictedAmount: totalSuma.toFixed(2),
            confidence: avgConfidence,
            status: status,
            comparison: 0,
            lastWeekAmount: 0,
            predictedExpenses: predictedExpenses
        };

        res.json(response);
    } catch (e) {
        console.error("Controller Error (Semana):", e);
        res.status(500).json({ mensaje: "Error al obtener predicciones semanales."});
    }
}

module.exports = { obtenerPrediccionDia, obtenerPrediccionSemana };
