# LuckXpress Backend

A modern, multi-module Spring Boot application with JWT authentication, RESTful APIs, and microservices architecture.

## 🚀 Features

- **Multi-Module Architecture**: Clean separation of concerns with distinct modules
- **JWT Authentication**: Secure token-based authentication system
- **RESTful APIs**: Well-designed REST endpoints with OpenAPI documentation
- **Database Migration**: Liquibase for version-controlled database changes
- **External API Integration**: Feign clients for third-party service integration
- **Docker Support**: Containerized deployment with Docker Compose
- **Comprehensive Error Handling**: Global exception handling with detailed error responses
- **API Documentation**: Swagger UI for interactive API exploration

## 📁 Project Structure

```
luckxpress-backend/
├── luckxpress-core/        # Security, JWT, and common utilities
├── luckxpress-data/        # JPA entities, repositories, and database migrations
├── luckxpress-service/     # Business logic and service layer
├── luckxpress-remote/      # External API clients and integrations
└── luckxpress-web/         # REST controllers and main application
```

## 🛠️ Technology Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Security** with JWT
- **Spring Data JPA** with Hibernate
- **Liquibase** for database migrations
- **PostgreSQL** / H2 Database
- **OpenFeign** for REST clients
- **Swagger/OpenAPI 3.0**
- **Docker & Docker Compose**
- **Maven** for dependency management

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker & Docker Compose (optional)
- PostgreSQL (optional, H2 for development)

## 🚀 Quick Start

### Using Maven

1. **Clone the repository**
```bash
git clone <repository-url>
cd luckxpress-backend
```

2. **Build the project**
```bash
mvn clean install
```

3. **Run the application**
```bash
mvn spring-boot:run -pl luckxpress-web
```

The application will start on `http://localhost:8080`

### Using Docker Compose

1. **Build and run with Docker Compose**
```bash
docker-compose up --build
```

This will start:
- PostgreSQL database on port 5432
- LuckXpress Backend on port 8080
- Adminer (database UI) on port 8081

## 📝 API Documentation

Once the application is running, access the API documentation at:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## 🔐 Authentication

The application uses JWT for authentication. Default admin credentials:
- Username: `admin`
- Email: `admin@luckxpress.com`
- Password: `admin123`

### Authentication Flow

1. **Login**: POST `/api/v1/auth/login`
```json
{
  "usernameOrEmail": "admin",
  "password": "admin123"
}
```

2. **Use the token**: Include in Authorization header
```
Authorization: Bearer <your-jwt-token>
```

## 🗄️ Database Configuration

### H2 (Development)
The application uses H2 in-memory database by default. Access H2 console at:
`http://localhost:8080/h2-console`

### PostgreSQL (Production)
Configure PostgreSQL in `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/luckxpress
    username: your-username
    password: your-password
```

## 🧪 Testing

Run tests with Maven:
```bash
mvn test
```

Run integration tests:
```bash
mvn verify
```

## 📊 Health & Monitoring

Health check endpoints:
- Basic health: `GET /api/v1/health`
- Detailed health: `GET /api/v1/health/detailed`
- Actuator health: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`

## 🔧 Configuration

Key configuration files:
- `application.yml`: Main configuration
- `application-dev.yml`: Development profile
- `application-prod.yml`: Production profile

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_URL` | Database URL | H2 in-memory |
| `DB_USERNAME` | Database username | sa |
| `DB_PASSWORD` | Database password | (empty) |
| `JWT_SECRET` | JWT signing key | (generated) |
| `SERVER_PORT` | Server port | 8080 |

## 🚢 Deployment

### Docker Build
```bash
docker build -t luckxpress-backend .
```

### Docker Run
```bash
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host:5432/db \
  -e DB_USERNAME=user \
  -e DB_PASSWORD=pass \
  luckxpress-backend
```

## 📚 Module Details

### luckxpress-core
- Security configuration
- JWT token service
- Authentication filters
- Common utilities

### luckxpress-data
- JPA entities (User, Role)
- Spring Data repositories
- Liquibase migrations
- Database configuration

### luckxpress-service
- Business logic implementation
- User management service
- DTO mappings
- Transaction management

### luckxpress-remote
- Feign client configuration
- External API integrations
- Circuit breaker patterns
- REST client templates

### luckxpress-web
- REST controllers
- Exception handlers
- OpenAPI configuration
- Main Spring Boot application

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

## 📄 License

This project is licensed under the Apache License 2.0

## 📞 Support

For support and questions, please contact: support@luckxpress.com

## 🔄 Version History

- **1.0.0** - Initial release with core functionality

---

Built with ❤️ by the LuckXpress Team
