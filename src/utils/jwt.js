const jwt = require('jsonwebtoken');
const config = require('../config/config');

/**
 * Generate JWT access token
 * @param {Object} payload - The payload to include in the token
 * @param {string} payload.userId - User ID
 * @param {string} payload.email - User email
 * @param {string} [payload.role] - User role
 * @returns {string} JWT token
 */
const generateAccessToken = (payload) => {
  try {
    const token = jwt.sign(
      payload,
      config.jwt.secret,
      { 
        expiresIn: config.jwt.expire,
        issuer: 'luckxpress-backend',
        audience: 'luckxpress-client'
      }
    );
    return token;
  } catch (error) {
    throw new Error('Token generation failed: ' + error.message);
  }
};

/**
 * Verify JWT access token
 * @param {string} token - JWT token to verify
 * @returns {Object} Decoded token payload
 */
const verifyAccessToken = (token) => {
  try {
    const decoded = jwt.verify(token, config.jwt.secret, {
      issuer: 'luckxpress-backend',
      audience: 'luckxpress-client'
    });
    return decoded;
  } catch (error) {
    throw new Error('Token verification failed: ' + error.message);
  }
};

/**
 * Generate refresh token (longer lived)
 * @param {Object} payload - The payload to include in the token
 * @returns {string} JWT refresh token
 */
const generateRefreshToken = (payload) => {
  try {
    const token = jwt.sign(
      payload,
      config.jwt.secret,
      { 
        expiresIn: '7d',
        issuer: 'luckxpress-backend',
        audience: 'luckxpress-client'
      }
    );
    return token;
  } catch (error) {
    throw new Error('Refresh token generation failed: ' + error.message);
  }
};

module.exports = {
  generateAccessToken,
  verifyAccessToken,
  generateRefreshToken
};