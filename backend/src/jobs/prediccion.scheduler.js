const cron = require("node-cron");
const { spawn } = require("child_process");
const path = require("path");
const pool = require("../config/database");

const USER_ID = Number(process.env.PREDICTION_USER_ID || 10);
const CONFIDENCE_THRESHOLD = Number(process.env.PREDICTION_MIN_CONFIDENCE || 0.35);
const PYTHON_BIN = process.env.PYTHON_BIN || "python";
const iaDir = path.resolve(__dirname, "../../ia/prediccion");
const predictionScript = path.join(iaDir, "main.py");
const trainingScript = path.join(iaDir, "entrenamiento.py");

function runPython(scriptPath, args = []) {
    return new Promise((resolve, reject) => {
        const child = spawn(PYTHON_BIN, [scriptPath, ...args], { cwd: iaDir });
        let output = "";
        let errorOutput = "";

        child.stdout.on("data", (chunk) => {
            const text = chunk.toString();
            output += text;
            process.stdout.write(`[IA] ${text}`);
        });

        child.stderr.on("data", (chunk) => {
            const text = chunk.toString();
            errorOutput += text;
            process.stderr.write(`[IA] ${text}`);
        });

        child.on("close", (code) => {
            if (code !== 0) {
                reject(new Error(`Proceso Python falló (${code}): ${errorOutput || output}`));
                return;
            }
            resolve(output);
        });
    });
}

function getTodayISO() {
    return new Date().toISOString().slice(0, 10);
}

async function generarPrediccionSemanal(fechaInicio = getTodayISO()) {
    await runPython(predictionScript, [
        "--usuario-id", String(USER_ID),
        "--fecha-inicio", fechaInicio,
        "--dias", "7"
    ]);
}

async function reentrenarModelos() {
    await runPython(trainingScript);
}

async function validarMostrarDashboard() {
    const query = `
        SELECT MAX(score_confianza) AS max_conf
        FROM predicciones_gastos
        WHERE usuario_id = $1
          AND fecha_prediccion = CURRENT_DATE
    `;
    const result = await pool.query(query, [USER_ID]);
    const conf = Number(result.rows[0]?.max_conf || 0);
    const estado = conf >= CONFIDENCE_THRESHOLD ? "MOSTRAR" : "OCULTAR";
    console.log(`[IA-VALIDACION] usuario=${USER_ID} confianza=${conf.toFixed(3)} estado=${estado}`);
}

function iniciarSchedulerPredicciones() {
    // Cada lunes 00:00: reentrena y genera predicción semanal.
    cron.schedule("0 0 * * 1", async () => {
        try {
            await reentrenarModelos();
            await generarPrediccionSemanal();
            console.log("[IA-SCHEDULER] Predicción semanal generada.");
        } catch (error) {
            console.error("[IA-SCHEDULER] Error en ciclo semanal:", error.message);
        }
    });

    // Diario 03:00: valida si se debe mostrar predicción.
    cron.schedule("0 3 * * *", async () => {
        try {
            await validarMostrarDashboard();
        } catch (error) {
            console.error("[IA-SCHEDULER] Error validando predicción diaria:", error.message);
        }
    });

    console.log("[IA-SCHEDULER] Reglas activas: lunes 00:00 semanal, diario 03:00 validación.");
}

module.exports = {
    iniciarSchedulerPredicciones,
    generarPrediccionSemanal,
    reentrenarModelos,
    validarMostrarDashboard
};
