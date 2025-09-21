# LuckXpress Docker Setup Guide

This guide provides comprehensive instructions for setting up and running the LuckXpress platform using Docker and Docker Compose.

## 📋 Prerequisites

- **Docker Desktop** 4.20+ with Windows containers support
- **Docker Compose** v2.20+
- **PowerShell** 5.1+ or PowerShell Core 7+
- **At least 8GB RAM** available for containers
- **At least 20GB disk space** for images and volumes

## 🚀 Quick Start

### 1. Initial Setup

```powershell
# Clone the repository (if not already done)
git clone https://github.com/your-org/luckxpress-backend.git
cd luckxpress-backend

# Copy and configure environment variables
cp .env.template .env
# Edit .env file with your configuration
```

### 2. Start the Application

#### Production Environment
```powershell
# Using the utility script
.\scripts\docker-start.ps1 -Environment prod -Build

# Or manually
docker-compose up -d --build
```

#### Development Environment
```powershell
# Using the utility script  
.\scripts\docker-start.ps1 -Environment dev -Build

# Or manually
docker-compose -f docker-compose.yml -f docker-dev.yml up -d --build
```

### 3. Verify Installation

```powershell
# Check all services are healthy
.\scripts\docker-health.ps1

# View application logs
.\scripts\docker-logs.ps1 -Service app -Follow
```

## 🏗️ Architecture Overview

The Docker setup includes the following services:

| Service | Port | Description |
|---------|------|-------------|
| **luckxpress-app** | 8080, 8081 | Main Spring Boot application |
| **postgres** | 5432 | PostgreSQL database |
| **redis** | 6379 | Redis cache |
| **kafka** | 9092 | Message broker |
| **zookeeper** | 2181 | Kafka coordination |
| **prometheus** | 9090 | Metrics collection |
| **grafana** | 3000 | Monitoring dashboards |

### Development-Only Services

| Service | Port | Description |
|---------|------|-------------|
| **pgadmin** | 8082 | Database management |
| **redis-commander** | 8083 | Redis management |

## 📁 Directory Structure

```
luckxpress-backend/
├── Dockerfile                  # Multi-stage application build
├── docker-compose.yml         # Production services
├── docker-dev.yml             # Development overrides
├── .env.template              # Environment configuration template
├── scripts/
│   ├── init-db.sql           # Database initialization
│   ├── docker-start.ps1      # Start script
│   ├── docker-stop.ps1       # Stop script
│   ├── docker-logs.ps1       # Log viewer
│   └── docker-health.ps1     # Health checker
└── monitoring/
    ├── prometheus.yml         # Prometheus configuration
    └── grafana/
        ├── datasources/       # Grafana data sources
        └── dashboards/        # Pre-built dashboards
```

## ⚙️ Configuration

### Environment Variables

The `.env` file contains all configuration. Key sections:

#### Database Configuration
```env
DB_NAME=luckxpress
DB_USER=luckxpress
DB_PASSWORD=your_secure_password
```

#### Security Configuration
```env
JWT_SECRET=your_jwt_secret_minimum_64_characters
OAUTH2_CLIENT_ID=your_oauth2_client_id
OAUTH2_CLIENT_SECRET=your_oauth2_client_secret
```

#### External Services
```env
SENTRY_DSN=https://your-sentry-dsn@sentry.io/project-id
STRIPE_SECRET_KEY=sk_live_your_stripe_secret_key
KYC_PROVIDER_API_KEY=your_kyc_provider_api_key
```

### Spring Profiles

| Profile | Description | Use Case |
|---------|-------------|----------|
| `prod` | Production configuration | Live deployment |
| `dev` | Development with debug | Local development |
| `test` | Testing configuration | CI/CD pipelines |

## 🛠️ Management Scripts

### Start Services

```powershell
# Production with clean build
.\scripts\docker-start.ps1 -Environment prod -Build -Clean

# Development with logs
.\scripts\docker-start.ps1 -Environment dev -Logs

# Production with specific version
$env:BUILD_VERSION="1.2.0"
.\scripts\docker-start.ps1 -Environment prod -Build
```

### Stop Services

```powershell
# Stop all services
.\scripts\docker-stop.ps1

# Stop and remove volumes (⚠️ Data loss!)
.\scripts\docker-stop.ps1 -RemoveVolumes

# Stop with database backup
.\scripts\docker-stop.ps1 -Backup -CleanImages
```

### View Logs

```powershell
# Application logs (last 100 lines, follow)
.\scripts\docker-logs.ps1 -Service app -Lines 100 -Follow

# All services logs
.\scripts\docker-logs.ps1 -All -Follow

# Database logs
.\scripts\docker-logs.ps1 -Service postgres -Lines 50
```

### Health Monitoring

```powershell
# Complete health check
.\scripts\docker-health.ps1

# Docker Compose status
docker-compose ps

# Individual service health
docker-compose exec app curl -f http://localhost:8081/actuator/health
```

## 🔍 Monitoring and Observability

### Application Metrics
- **URL**: http://localhost:8081/actuator
- **Endpoints**: `/health`, `/metrics`, `/info`, `/prometheus`

### Grafana Dashboards
- **URL**: http://localhost:3000
- **Login**: admin / admin (change in .env)
- **Dashboards**: 
  - LuckXpress Overview
  - JVM Metrics
  - Database Performance
  - Compliance Monitoring

### Prometheus Metrics
- **URL**: http://localhost:9090
- **Targets**: Application, Database, Redis, Kafka
- **Alerts**: Custom rules for compliance violations

## 🗄️ Database Management

### Database Access

```powershell
# Connect to PostgreSQL
docker-compose exec postgres psql -U luckxpress -d luckxpress

# PgAdmin (Development)
# URL: http://localhost:8082
# Email: admin@luckxpress.com / Password: admin
```

### Backup and Restore

```powershell
# Create backup
docker-compose exec postgres pg_dump -U luckxpress luckxpress > backup.sql

# Restore backup
docker-compose exec -T postgres psql -U luckxpress -d luckxpress < backup.sql

# Automated backup (using stop script)
.\scripts\docker-stop.ps1 -Backup
```

### Database Schema

The `scripts/init-db.sql` includes:
- **Extensions**: UUID, pgcrypto
- **Audit Schema**: Change tracking
- **Compliance Tables**: State restrictions, KYC types
- **Security**: Row-level security, readonly user

## 🧪 Development Environment

### Debug Configuration

The development environment includes:
- **Debug Port**: 5005 (Java Remote Debug)
- **Hot Reload**: Volume mounts for target directory
- **Mock Services**: Payment and KYC mocking enabled
- **Extended Logging**: DEBUG level logs

### Development Tools

| Tool | URL | Credentials |
|------|-----|-------------|
| PgAdmin | http://localhost:8082 | admin@luckxpress.com / admin |
| Redis Commander | http://localhost:8083 | N/A |
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | N/A |

### IntelliJ IDEA Setup

1. Create Remote Debug Configuration:
   - Host: localhost
   - Port: 5005
   - Module: luckxpress-web

2. Database Connection:
   - URL: jdbc:postgresql://localhost:5432/luckxpress_dev
   - User: dev_user
   - Password: dev_password

## 🚨 Troubleshooting

### Common Issues

#### Port Conflicts
```powershell
# Check port usage
netstat -an | findstr :8080

# Change ports in docker-compose.yml if needed
ports:
  - "8081:8080"  # Host:Container
```

#### Memory Issues
```powershell
# Check Docker memory settings
docker system df
docker stats

# Increase Docker Desktop memory allocation
# Settings > Resources > Memory > 8GB+
```

#### Database Connection Issues
```powershell
# Check database status
docker-compose exec postgres pg_isready -U luckxpress

# View database logs
.\scripts\docker-logs.ps1 -Service postgres -Lines 100

# Reset database (⚠️ Data loss!)
docker-compose down -v
docker-compose up -d postgres
```

#### Build Issues
```powershell
# Clean rebuild
docker-compose down
docker system prune -f
docker-compose build --no-cache
docker-compose up -d
```

### Performance Tuning

#### JVM Settings
```env
# In .env file
JAVA_OPTS=-XX:MaxRAMPercentage=75 -XX:+UseG1GC -XX:+UseStringDeduplication
```

#### Database Tuning
```env
# PostgreSQL settings
POSTGRES_SHARED_BUFFERS=256MB
POSTGRES_EFFECTIVE_CACHE_SIZE=1GB
POSTGRES_RANDOM_PAGE_COST=1.1
```

#### Redis Optimization
```env
# Redis memory settings
REDIS_MAXMEMORY=512mb
REDIS_MAXMEMORY_POLICY=allkeys-lru
```

## 🔒 Security Considerations

### Production Security Checklist

- [ ] Change all default passwords in `.env`
- [ ] Use strong JWT secrets (64+ characters)
- [ ] Configure HTTPS with valid certificates
- [ ] Enable database SSL connections
- [ ] Set up proper firewall rules
- [ ] Configure log retention policies
- [ ] Enable container resource limits
- [ ] Set up automated security updates

### Network Security
```yaml
# Custom network configuration
networks:
  luckxpress-network:
    driver: bridge
    driver_opts:
      com.docker.network.bridge.enable_icc: "false"
```

### Secrets Management
```powershell
# Use Docker secrets for sensitive data
echo "your_secret" | docker secret create jwt_secret -
```

## 📊 Performance Monitoring

### Key Metrics to Monitor

1. **Application Metrics**
   - Response times (95th percentile < 500ms)
   - Error rates (< 0.1%)
   - Throughput (requests/second)

2. **Database Metrics**
   - Connection pool utilization
   - Query execution times
   - Deadlock occurrences

3. **Financial Metrics**
   - Transaction volumes
   - Failed payment rates
   - KYC processing times

4. **Compliance Metrics**
   - Suspicious activity alerts
   - State restriction violations
   - Audit log completeness

### Alerting Rules

Configure alerts for:
- Application down for > 1 minute
- Database connection failures
- High error rates (> 5%)
- KYC queue backlog (> 100 items)
- Failed transactions (> 10/hour)

## 🔄 CI/CD Integration

### GitHub Actions

```yaml
# Example workflow snippet
jobs:
  docker-build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Build and test
        run: |
          docker-compose -f docker-compose.yml -f docker-compose.test.yml up --abort-on-container-exit
      - name: Deploy to staging
        run: |
          docker-compose -f docker-compose.yml up -d
```

### Deployment Strategies

1. **Blue-Green Deployment**
2. **Rolling Updates**
3. **Canary Releases**

## 📚 Additional Resources

- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)
- [PostgreSQL Docker Hub](https://hub.docker.com/_/postgres)
- [Prometheus Configuration](https://prometheus.io/docs/prometheus/latest/configuration/configuration/)
- [Grafana Dashboard Documentation](https://grafana.com/docs/grafana/latest/dashboards/)

## 🆘 Support

For issues and questions:
1. Check the troubleshooting section above
2. Review application logs: `.\scripts\docker-logs.ps1 -Service app`
3. Run health check: `.\scripts\docker-health.ps1`
4. Contact the development team with logs and error details

---

**⚠️ Important**: Always backup your database before major updates or configuration changes!
