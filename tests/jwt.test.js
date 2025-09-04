const { generateAccessToken, verifyAccessToken, generateRefreshToken } = require('../src/utils/jwt');

describe('JWT Utilities', () => {
  const testPayload = {
    userId: 'user_123',
    email: 'test@example.com',
    role: 'user'
  };

  describe('generateAccessToken', () => {
    it('should generate a valid access token', () => {
      const token = generateAccessToken(testPayload);
      
      expect(typeof token).toBe('string');
      expect(token.split('.')).toHaveLength(3); // JWT has 3 parts
    });

    it('should throw error with invalid payload', () => {
      expect(() => {
        generateAccessToken(null);
      }).toThrow();
    });
  });

  describe('verifyAccessToken', () => {
    it('should verify a valid token', () => {
      const token = generateAccessToken(testPayload);
      const decoded = verifyAccessToken(token);
      
      expect(decoded.userId).toBe(testPayload.userId);
      expect(decoded.email).toBe(testPayload.email);
      expect(decoded.role).toBe(testPayload.role);
      expect(decoded.iss).toBe('luckxpress-backend');
      expect(decoded.aud).toBe('luckxpress-client');
    });

    it('should throw error with invalid token', () => {
      expect(() => {
        verifyAccessToken('invalid.token.here');
      }).toThrow();
    });

    it('should throw error with empty token', () => {
      expect(() => {
        verifyAccessToken('');
      }).toThrow();
    });
  });

  describe('generateRefreshToken', () => {
    it('should generate a valid refresh token', () => {
      const token = generateRefreshToken(testPayload);
      
      expect(typeof token).toBe('string');
      expect(token.split('.')).toHaveLength(3);
    });

    it('should generate different tokens for access and refresh', () => {
      const accessToken = generateAccessToken(testPayload);
      const refreshToken = generateRefreshToken(testPayload);
      
      expect(accessToken).not.toBe(refreshToken);
    });
  });
});