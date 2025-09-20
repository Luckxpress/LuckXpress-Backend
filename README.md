# LuckXpress Backend

A comprehensive sweepstakes gaming platform backend with strict compliance validation and dual-currency support.

## 🎯 Overview

LuckXpress Backend is a production-ready Spring Boot application that provides:

- **Dual Currency System**: Gold Coins (GC) for social gaming, Sweeps Coins (SC) for prizes
- **Compliance First**: State restrictions, KYC verification, and regulatory compliance
- **Security**: JWT authentication, role-based access control, comprehensive audit trails
- **Financial Controls**: Dual/triple approval workflows, balance integrity, immutable ledger system

## 🏗️ Architecture

```
luckxpress-backend/
├── luckxpress-common/     # Foundation utilities and exceptions
├── luckxpress-core/       # Security and context management
├── luckxpress-data/       # Data layer with JPA entities
├── luckxpress-service/    # Business logic and services
├── luckxpress-web/        # REST controllers and DTOs
└── luckxpress-app/        # Main Spring Boot application
```

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- PostgreSQL 13+ (for production)
- Redis (optional, for caching)

### Local Development

```bash
# Clone the repository
git clone https://github.com/your-org/luckxpress-backend.git
cd luckxpress-backend

# Build the project
mvn clean install

# Run with local H2 database
mvn spring-boot:run -pl luckxpress-app -Dspring-boot.run.profiles=local

# Access the application
# API: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
# H2 Console: http://localhost:8080/h2-console
```

### Environment Configuration

```bash
# Development
export SPRING_PROFILES_ACTIVE=dev
export DATABASE_URL=jdbc:postgresql://localhost:5432/luckxpress_dev
export DATABASE_USERNAME=luckxpress_dev
export DATABASE_PASSWORD=dev_password

# Production
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_URL=jdbc:postgresql://prod-db:5432/luckxpress
export DATABASE_USERNAME=luckxpress_prod
export DATABASE_PASSWORD=secure_password
export JWT_SECRET=your-production-jwt-secret
```

## 🔐 Compliance & Security

### Financial Compliance
- ✅ State restrictions (ID, WA, MT, NV blocked)
- ✅ KYC verification required for withdrawals
- ✅ Age verification (21+ only)
- ✅ Dual/triple approval workflows for large transactions
- ✅ Daily/monthly withdrawal limits
- ✅ Comprehensive audit trails

### Security Features
- ✅ JWT authentication with role-based access
- ✅ Request tracing and correlation IDs
- ✅ Idempotency protection
- ✅ Input validation and sanitization
- ✅ Global exception handling

## 💰 Currency System

### Gold Coins (GOLD)
- **Purpose**: Social gaming currency
- **Withdrawable**: No
- **Use Cases**: Game play, social features
- **Acquisition**: Purchase, bonuses, promotions

### Sweeps Coins (SWEEPS)
- **Purpose**: Prize currency
- **Withdrawable**: Yes (with KYC verification)
- **Use Cases**: Prize games, cash redemption
- **Acquisition**: Promotional giveaways, bonus with Gold Coin purchases

## 📊 API Documentation

### Swagger/OpenAPI
- **Local**: http://localhost:8080/swagger-ui.html
- **API Groups**:
  - Public API: Registration, public information
  - User API: Account management, transactions
  - Admin API: Administrative operations

### Key Endpoints

```
POST /api/user/register          # User registration
GET  /api/account                # Get user accounts
POST /api/transaction/deposit    # Process deposit
POST /api/withdrawal             # Process withdrawal
POST /api/kyc/submit            # Submit KYC verification
GET  /api/admin/health          # System health check
```

## 🗄️ Database Schema

### Core Entities
- **Users**: User accounts with compliance tracking
- **Accounts**: Currency-specific account balances
- **Transactions**: All financial transactions with audit trail
- **LedgerEntries**: Immutable financial ledger
- **KycVerifications**: KYC compliance tracking
- **ApprovalWorkflows**: Dual/triple approval processes
- **ComplianceAudits**: Regulatory compliance logging

## 🔧 Development

### Code Style
- Java 21 features encouraged
- Spring Boot best practices
- Comprehensive JavaDoc documentation
- Unit tests required for all services

### Branch Strategy
- `main`: Production-ready code
- `develop`: Integration branch
- `feature/*`: Feature development
- `release/*`: Release preparation
- `hotfix/*`: Production fixes

### Testing
```bash
# Run all tests
mvn test

# Run integration tests
mvn verify -P integration-tests

# Generate test coverage report
mvn jacoco:report
```

## 🚀 Deployment

### Docker Support
```bash
# Build Docker image
docker build -t luckxpress-backend:latest .

# Run with Docker Compose
docker-compose up -d
```

### Environment Variables
| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `local` |
| `DATABASE_URL` | Database connection URL | H2 in-memory |
| `JWT_SECRET` | JWT signing secret | Development key |
| `REDIS_HOST` | Redis server host | `localhost` |

## 📈 Monitoring

### Health Checks
- **Endpoint**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Prometheus**: `/actuator/prometheus`

### Logging
- **Format**: Structured JSON logging
- **Correlation**: Request tracing with correlation IDs
- **Audit**: Comprehensive compliance audit logs

## 🤝 Contributing

### Development Workflow
1. Create feature branch from `develop`
2. Implement changes with tests
3. Submit pull request to `develop`
4. Code review and approval required
5. Merge to `develop` after CI passes

### Code Review Requirements
- Minimum 2 approvals required
- All CI checks must pass
- Security review for sensitive changes
- Compliance review for financial logic

## 📄 License

This project is proprietary software owned by LuckXpress. All rights reserved.

## 📞 Support

- **Technical Issues**: Create GitHub issue
- **Security Concerns**: security@luckxpress.com
- **Compliance Questions**: compliance@luckxpress.com

## 🔄 Changelog

See [CHANGELOG.md](CHANGELOG.md) for detailed release notes.

---

**Built with ❤️ by the LuckXpress Engineering Team**
