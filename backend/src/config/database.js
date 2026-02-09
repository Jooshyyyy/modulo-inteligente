const { Pool } = require('pg');

const pool = new Pool ({
    host: process.env.DB_HOST,
    user: process.env.USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_Name,
    port: 5432
});

module.exports = pool;