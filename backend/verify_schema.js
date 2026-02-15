const pool = require('./src/config/database');

async function verifySchema() {
    try {
        console.log('Verifying schema...');

        const tablesRes = await pool.query(`
            SELECT table_name 
            FROM information_schema.tables 
            WHERE table_schema = 'public'
        `);
        const tables = tablesRes.rows.map(r => r.table_name);
        console.log('Tables found:', tables);

        if (tables.includes('usuarios')) {
            const userCols = await pool.query(`
                SELECT column_name 
                FROM information_schema.columns 
                WHERE table_name = 'usuarios'
            `);
            console.log('Columns in usuarios:', userCols.rows.map(r => r.column_name));
        }

        if (tables.includes('administradores')) {
            const adminCols = await pool.query(`
                SELECT column_name 
                FROM information_schema.columns 
                WHERE table_name = 'administradores'
            `);
            console.log('Columns in administradores:', adminCols.rows.map(r => r.column_name));
        }

    } catch (err) {
        console.error('Verification failed:', err);
    } finally {
        await pool.end();
    }
}

verifySchema();
