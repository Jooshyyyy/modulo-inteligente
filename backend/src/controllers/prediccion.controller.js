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
        
        let nivel = "Patrón moderado (modelo de gasto)";
        if (total > 200) nivel = "Alerta de pico — gasto elevado proyectado";
        else if (total < 50) nivel = "Día tranquilo — baja actividad de gasto esperada";

        const response = {
            total: total.toFixed(2),
            nivel: nivel,
            diferenciaPrevia: `IA personal · confianza ~${porcentajeConfianza}% · categoría dominante inferida`,
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

const obtenerPrediccionSemanal = async (req, res) => {
    try {
        const usuarioId = req.usuario.id;
        const { fecha_inicio } = req.query;
        const inicio = fecha_inicio || new Date().toISOString().slice(0, 10);
        const [rows, detalleDias] = await Promise.all([
            Prediccion.obtenerSemanaPorCategoria(usuarioId, inicio),
            Prediccion.obtenerSemanaPorDia(usuarioId, inicio)
        ]);

        const categorias = rows.map((row) => ({
            categoriaId: row.categoria_id,
            categoria: row.categoria_nombre,
            colorHex: row.color_hex,
            monto: Number(row.monto_total || 0),
            confianza: Number(row.confianza_promedio || 0)
        }));

        const total = categorias.reduce((acc, item) => acc + item.monto, 0);
        const dias = detalleDias.map((row) => ({
            fecha: row.fecha_prediccion,
            categoria: row.categoria_nombre,
            colorHex: row.color_hex,
            monto: Number(row.monto_proyectado || 0),
            confianza: Number(row.score_confianza || 0)
        }));

        res.json({
            fechaInicio: inicio,
            total: Number(total.toFixed(2)),
            categorias,
            dias
        });
    } catch (e) {
        console.error("Controller Error semanal:", e);
        res.status(500).json({ mensaje: "Error al obtener predicción semanal." });
    }
};

const obtenerPrediccionMensual = async (req, res) => {
    try {
        const usuarioId = req.usuario.id;
        const { mes } = req.query;
        const mesObjetivo = mes || new Date().toISOString().slice(0, 7);

        const [rows, detalleDias] = await Promise.all([
            Prediccion.obtenerMesResumen(usuarioId, mesObjetivo),
            Prediccion.obtenerMesPorDia(usuarioId, mesObjetivo)
        ]);

        const categorias = rows.map((row) => ({
            categoriaId: row.categoria_id,
            categoria: row.categoria_nombre,
            colorHex: row.color_hex,
            monto: Number(row.monto_total || 0),
            confianza: Number(row.confianza_promedio || 0)
        }));

        const dias = detalleDias.map((row) => ({
            fecha: row.fecha_prediccion,
            categoria: row.categoria_nombre,
            colorHex: row.color_hex,
            monto: Number(row.monto_proyectado || 0),
            confianza: Number(row.score_confianza || 0)
        }));

        const total = categorias.reduce((acc, item) => acc + item.monto, 0);
        res.json({
            mes: mesObjetivo,
            total: Number(total.toFixed(2)),
            categorias,
            dias
        });
    } catch (e) {
        console.error("Controller Error mensual:", e);
        res.status(500).json({ mensaje: "Error al obtener predicción mensual." });
    }
};

module.exports = { obtenerPrediccionDia, obtenerPrediccionSemana, obtenerPrediccionSemanal, obtenerPrediccionMensual };
