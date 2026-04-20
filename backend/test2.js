const pool = require('./src/config/database');
const Prediccion = require('./src/models/prediccion.model');

async function test() {
    try {
        const rows = await Prediccion.obtenerPorDia(1, "2026-04-08"); // user 1, date 2026-04-08
        console.log("Returned rows:", rows);
    } catch (e) {
        console.error("Error:", e);
    } finally {
        pool.end();
    }
}
test();
