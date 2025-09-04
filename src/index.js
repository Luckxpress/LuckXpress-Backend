const app = require('./app');
const config = require('./config/config');

const server = app.listen(config.port, () => {
  // eslint-disable-next-line no-console
  console.log(`
🚀 LuckXpress Backend Server Started!
─────────────────────────────────────
📍 Server URL: http://localhost:${config.port}
🌍 Environment: ${config.nodeEnv}
📋 API Version: ${config.api.version}
⚡ Status: Ready to generate access tokens!

📚 Available endpoints:
   GET  /                           - API information
   GET  /health                     - Health check
   POST /api/${config.api.version}/auth/register       - User registration
   POST /api/${config.api.version}/auth/login          - User login
   POST /api/${config.api.version}/auth/generate-token - Generate access token
   POST /api/${config.api.version}/auth/refresh-token  - Refresh access token
   GET  /api/${config.api.version}/auth/profile        - Get user profile (requires token)
─────────────────────────────────────
  `);
});

// Graceful shutdown
process.on('SIGTERM', () => {
  // eslint-disable-next-line no-console
  console.log('SIGTERM received, shutting down gracefully...');
  server.close(() => {
    // eslint-disable-next-line no-console
    console.log('Process terminated');
  });
});

process.on('SIGINT', () => {
  // eslint-disable-next-line no-console
  console.log('SIGINT received, shutting down gracefully...');
  server.close(() => {
    // eslint-disable-next-line no-console
    console.log('Process terminated');
  });
});

module.exports = server;