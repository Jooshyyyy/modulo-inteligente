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

module.exports = { obtenerPrediccionDia, obtenerPrediccionSemanal, obtenerPrediccionMensual };
