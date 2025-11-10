const path = require('path');
const dotenv = require('dotenv');

dotenv.config({
  path: process.env.DOTENV_PATH
    ? path.resolve(process.env.DOTENV_PATH)
    : path.resolve(process.cwd(), '.env'),
});

function toNumber(value, fallback) {
  if (value === undefined || value === null) {
    return fallback;
  }
  const parsed = Number(value);
  return Number.isNaN(parsed) ? fallback : parsed;
}

const env = {
  app: {
    port: toNumber(process.env.CHAT_PORT, 3001),
  },
  db: {
    host: process.env.DB_HOST || 'localhost',
    port: toNumber(process.env.DB_PORT, 3306),
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    connectionLimit: toNumber(process.env.DB_CONNECTION_LIMIT, 10),
    queueLimit: toNumber(process.env.DB_QUEUE_LIMIT, 0),
  },
  services: {
    springBootUrl: process.env.SPRING_BOOT_URL || 'http://localhost:8080',
  },
  redis: {
    url: process.env.REDIS_URL,
    host: process.env.REDIS_HOST || '127.0.0.1',
    port: toNumber(process.env.REDIS_PORT, 6379),
    queue: process.env.REDIS_NOTIFICATION_QUEUE || 'chat:notification-queue',
    scheduleSet: process.env.REDIS_SCHEDULE_SET || 'chat:scheduled-notifications',
    schedulerIntervalMs: toNumber(process.env.REDIS_SCHEDULER_INTERVAL_MS, 1000),
  },
};

module.exports = env;

