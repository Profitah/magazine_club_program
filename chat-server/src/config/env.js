const path = require('path');
const dotenv = require('dotenv');

dotenv.config({
  path: process.env.DOTENV_PATH
    ? path.resolve(process.env.DOTENV_PATH)
    : path.resolve(process.cwd(), '.env'),
});

function getNumber(value, fallback) {
  if (value === undefined || value === null) {
    return fallback;
  }
  const parsed = Number(value);
  return Number.isNaN(parsed) ? fallback : parsed;
}

const env = {
  app: {
    port: getNumber(process.env.CHAT_PORT, 3001),
  },
  db: {
    host: process.env.DB_HOST || 'localhost',
    port: getNumber(process.env.DB_PORT, 3306),
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: getNumber(process.env.DB_CONNECTION_LIMIT, 10),
    queueLimit: getNumber(process.env.DB_QUEUE_LIMIT, 0),
  },
  services: {
    springBootUrl: process.env.SPRING_BOOT_URL || 'http://localhost:8080',
  },
};

module.exports = env;

