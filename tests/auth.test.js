const request = require('supertest');
const app = require('../src/app');

describe('Auth Endpoints', () => {
  describe('POST /api/v1/auth/register', () => {
    it('should register a new user and return tokens', async () => {
      const userData = {
        email: 'test' + Date.now() + '@example.com',
        password: 'password123',
        firstName: 'John',
        lastName: 'Doe'
      };

      const response = await request(app)
        .post('/api/v1/auth/register')
        .send(userData)
        .expect(201);

      expect(response.body.success).toBe(true);
      expect(response.body.message).toBe('User registered successfully');
      expect(response.body.data.user.email).toBe(userData.email);
      expect(response.body.data.tokens.accessToken).toBeDefined();
      expect(response.body.data.tokens.refreshToken).toBeDefined();
    });

    it('should not register user with missing fields', async () => {
      const userData = {
        email: 'testmissing' + Date.now() + '@example.com'
        // missing password, firstName, lastName
      };

      const response = await request(app)
        .post('/api/v1/auth/register')
        .send(userData)
        .expect(400);

      expect(response.body.success).toBe(false);
      expect(response.body.message).toContain('required');
    });
  });

  describe('POST /api/v1/auth/login', () => {
    const loginEmail = 'login' + Date.now() + '@example.com';
    
    beforeEach(async () => {
      // Register a user first
      await request(app)
        .post('/api/v1/auth/register')
        .send({
          email: loginEmail,
          password: 'password123',
          firstName: 'Jane',
          lastName: 'Doe'
        });
    });

    it('should login with valid credentials', async () => {
      const loginData = {
        email: loginEmail,
        password: 'password123'
      };

      const response = await request(app)
        .post('/api/v1/auth/login')
        .send(loginData)
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.message).toBe('Login successful');
      expect(response.body.data.tokens.accessToken).toBeDefined();
    });

    it('should not login with invalid credentials', async () => {
      const loginData = {
        email: loginEmail,
        password: 'wrongpassword'
      };

      const response = await request(app)
        .post('/api/v1/auth/login')
        .send(loginData)
        .expect(401);

      expect(response.body.success).toBe(false);
      expect(response.body.message).toBe('Invalid credentials');
    });
  });

  describe('POST /api/v1/auth/generate-token', () => {
    it('should generate tokens with valid payload', async () => {
      const tokenData = {
        userId: 'user_123',
        email: 'token@example.com',
        role: 'user'
      };

      const response = await request(app)
        .post('/api/v1/auth/generate-token')
        .send(tokenData)
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.message).toBe('Tokens generated successfully');
      expect(response.body.data.tokens.accessToken).toBeDefined();
      expect(response.body.data.tokens.refreshToken).toBeDefined();
    });

    it('should not generate tokens with missing fields', async () => {
      const tokenData = {
        userId: 'user_123'
        // missing email
      };

      const response = await request(app)
        .post('/api/v1/auth/generate-token')
        .send(tokenData)
        .expect(400);

      expect(response.body.success).toBe(false);
      expect(response.body.message).toContain('required');
    });
  });

  describe('GET /api/v1/auth/profile', () => {
    let accessToken;

    beforeEach(async () => {
      // Register and get token
      const response = await request(app)
        .post('/api/v1/auth/register')
        .send({
          email: 'profile' + Date.now() + '@example.com',
          password: 'password123',
          firstName: 'Profile',
          lastName: 'User'
        });
      
      accessToken = response.body.data.tokens.accessToken;
    });

    it('should get user profile with valid token', async () => {
      const response = await request(app)
        .get('/api/v1/auth/profile')
        .set('Authorization', `Bearer ${accessToken}`)
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.data.user.email).toContain('@example.com');
    });

    it('should not get profile without token', async () => {
      const response = await request(app)
        .get('/api/v1/auth/profile')
        .expect(401);

      expect(response.body.success).toBe(false);
      expect(response.body.message).toBe('Access token required');
    });
  });
});