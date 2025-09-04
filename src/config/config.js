require('dotenv').config();

const config = {
  port: process.env.PORT || 3000,
  nodeEnv: process.env.NODE_ENV || 'development',
  jwt: {
    secret: process.env.JWT_SECRET || 'fallback-secret-change-this',
    expire: process.env.JWT_EXPIRE || '24h'
  },
  api: {
    version: process.env.API_VERSION || 'v1'
  }
};

module.exports = config;