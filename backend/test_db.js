const pool = require('./src/config/database');

async function test() {
    try {
        const res = await pool.query('SELECT * FROM predicciones_gastos LIMIT 2;');
        console.log("Data exists:", res.rows);
    } catch (e) {
        console.error("Error query:", e);
    } finally {
        pool.end();
    }
}

test();
