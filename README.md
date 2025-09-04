# LuckXpress Backend

A robust Node.js/Express backend API with JWT access token generation and authentication features.

## Features

- 🔐 JWT Access Token Generation
- 🔑 User Authentication (Register/Login)
- 🔄 Token Refresh Mechanism
- 🛡️ Password Hashing with bcrypt
- 🚀 Express.js RESTful API
- ✅ Comprehensive Test Coverage
- 🔒 Security Middleware (Helmet, CORS)
- 📝 Request Logging
- 🌍 Environment Configuration

## Quick Start

### Prerequisites

- Node.js 14+ 
- npm or yarn

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd LuckXpress-Backend
```

2. Install dependencies:
```bash
npm install
```

3. Create environment file:
```bash
cp .env.example .env
```

4. Update the `.env` file with your configuration:
```env
PORT=3000
NODE_ENV=development
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production
JWT_EXPIRE=24h
API_VERSION=v1
```

5. Start the server:
```bash
# Development mode with auto-restart
npm run dev

# Production mode
npm start
```

## API Documentation

### Base URL
```
http://localhost:3000/api/v1
```

### Authentication Endpoints

#### 1. Register User
```http
POST /api/v1/auth/register
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe",
  "role": "user"
}
```

**Response:**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "user": {
      "id": "user_1234567890",
      "email": "user@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "role": "user"
    },
    "tokens": {
      "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    }
  }
}
```

#### 2. Login User
```http
POST /api/v1/auth/login
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

#### 3. Generate Access Token
```http
POST /api/v1/auth/generate-token
```

**Request Body:**
```json
{
  "userId": "user_123",
  "email": "user@example.com",
  "role": "user"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Tokens generated successfully",
  "data": {
    "tokens": {
      "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    }
  }
}
```

#### 4. Refresh Token
```http
POST /api/v1/auth/refresh-token
```

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### 5. Get User Profile (Protected)
```http
GET /api/v1/auth/profile
Authorization: Bearer <access_token>
```

### Utility Endpoints

#### Health Check
```http
GET /health
```

#### API Information
```http
GET /
```

## Authentication

All protected endpoints require a valid JWT token in the Authorization header:

```http
Authorization: Bearer <your_access_token>
```

## Development

### Available Scripts

```bash
# Start development server with auto-restart
npm run dev

# Start production server
npm start

# Run tests
npm test

# Run tests in watch mode
npm run test:watch

# Lint code
npm run lint

# Fix linting issues
npm run lint:fix
```

### Testing

Run the test suite:
```bash
npm test
```

The project includes comprehensive tests for:
- Authentication endpoints
- JWT token generation and verification
- Password hashing utilities
- API error handling

### Project Structure

```
src/
├── config/          # Configuration files
├── controllers/     # Route controllers
├── middleware/      # Custom middleware
├── routes/         # API routes
├── utils/          # Utility functions
├── app.js          # Express app setup
└── index.js        # Server entry point

tests/              # Test files
```

## Security Features

- **Password Hashing**: Uses bcrypt with salt rounds
- **JWT Security**: Signed tokens with configurable expiration
- **CORS Protection**: Cross-origin request handling
- **Helmet**: Security headers
- **Input Validation**: Request body validation
- **Environment Variables**: Secure configuration management

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `PORT` | Server port | `3000` |
| `NODE_ENV` | Environment mode | `development` |
| `JWT_SECRET` | JWT signing secret | Required |
| `JWT_EXPIRE` | Token expiration time | `24h` |
| `API_VERSION` | API version | `v1` |

## Error Handling

The API returns consistent error responses:

```json
{
  "success": false,
  "message": "Error description",
  "error": "Detailed error message (development only)"
}
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Run the test suite
6. Submit a pull request

## License

ISC License