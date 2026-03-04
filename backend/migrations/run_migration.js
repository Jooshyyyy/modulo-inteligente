const { Pool } = require('pg');
const fs = require('fs');
const path = require('path');
require('dotenv').config();

const pool = new Pool({
  host: process.env.DB_HOST,
  port: process.env.DB_PORT,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  ssl: {
    rejectUnauthorized: false
  }
});

async function runMigration() {
  const sqlPath = path.join(__dirname, '20260222_expand_banking.sql');
  const sql = fs.readFileSync(sqlPath, 'utf8');

  try {
    console.log('Running migration...');
    await pool.query(sql);
    console.log('Migration completed successfully.');
  } catch (err) {
    console.error('Error running migration:', err);
  } finally {
    await pool.end();
  }
}

runMigration();
