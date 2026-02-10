require("dotenv").config();
const { Pool } = require("pg");

const pool = new Pool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,          // 👈 forzado
  password: process.env.DB_PASSWORD,  // 👈 forzado
  database: process.env.DB_NAME,
  port: process.env.DB_PORT,
});
console.log("DB_USER:", process.env.DB_USER);
console.log("DB_PASSWORD:", process.env.DB_PASSWORD);
console.log("DB_NAME:", process.env.DB_NAME);
module.exports = pool;


