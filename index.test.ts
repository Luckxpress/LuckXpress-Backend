import request from 'supertest';
import app from './index';

describe('LuckXpress Backend API', () => {
  test('GET / should return welcome message', async () => {
    const response = await request(app)
      .get('/')
      .expect(200);
    
    expect(response.body.message).toBe('Welcome to LuckXpress Backend API');
  });
});