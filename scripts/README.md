# LuckXpress Backend Scripts

This directory contains various scripts to help with development, deployment, and verification of the LuckXpress backend system.

## Available Scripts

### 🔍 Deployment Verification
- **`verify-deployment.sh`** - Comprehensive verification script (Bash)
- **`verify-deployment.ps1`** - Comprehensive verification script (PowerShell)
- **`verify-deployment.bat`** - Windows batch wrapper to choose between versions

### 🐳 Docker Management
- **`docker-start.ps1`** - Start all Docker services
- **`docker-stop.ps1`** - Stop all Docker services  
- **`docker-health.ps1`** - Check health of Docker containers
- **`docker-logs.ps1`** - View logs from Docker containers

### 💾 Database
- **`init-db.sql`** - Database initialization script

## Usage

### Running Verification Script

**Windows (PowerShell):**
```powershell
.\scripts\verify-deployment.ps1
```

**Windows (Batch wrapper):**
```cmd
.\scripts\verify-deployment.bat
```

**Linux/Mac/WSL (Bash):**
```bash
chmod +x scripts/verify-deployment.sh
./scripts/verify-deployment.sh
```

### What the Verification Script Checks

1. **Environment Setup**
   - Java 21 installation
   - Maven 3.9.x installation
   - Docker and Docker Compose

2. **Project Health**
   - Code compilation
   - Unit tests execution
   - Compliance tests
   - Security vulnerability scan

3. **Configuration**
   - Required environment variables
   - Database migrations
   - API documentation accessibility

4. **Compliance Requirements**
   - BigDecimal usage for financial calculations
   - State restrictions implementation
   - KYC requirements
   - Monitoring setup

5. **Infrastructure**
   - Postman collection availability
   - GitHub Actions configuration
   - Sentry integration
   - Monitoring alerts

### Environment Variables Required

The verification script checks for these critical environment variables:

- `SENTRY_DSN` - Sentry error tracking DSN
- `DB_PASSWORD` - Database password
- `JWT_SECRET` - JWT signing secret
- `REDIS_PASSWORD` - Redis authentication password
- `OAUTH2_CLIENT_SECRET` - OAuth2 client secret

### Prerequisites

1. **Java 21** - Required for Spring Boot 3.x
2. **Maven 3.9.x** - For building the project
3. **Docker & Docker Compose** - For running services
4. **Environment Variables** - Set in `.env` file or system environment

### Troubleshooting

#### Common Issues

**Java Version Mismatch:**
```bash
# Check current Java version
java -version

# Set JAVA_HOME if needed (Windows)
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.x.x

# Set JAVA_HOME if needed (Linux/Mac)
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
```

**Maven Not Found:**
```bash
# Windows (Chocolatey)
choco install maven

# Linux (apt)
sudo apt install maven

# Mac (Homebrew)
brew install maven
```

**Docker Issues:**
```bash
# Check Docker status
docker --version
docker compose version

# Start Docker service (Linux)
sudo systemctl start docker
```

#### Script Permissions

**Linux/Mac:**
```bash
chmod +x scripts/*.sh
```

**Windows PowerShell:**
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Integration with CI/CD

The verification script can be integrated into your CI/CD pipeline:

```yaml
# Example GitHub Actions usage
- name: Run Verification Script
  run: |
    chmod +x scripts/verify-deployment.sh
    ./scripts/verify-deployment.sh
```

### Support

For issues with these scripts or deployment verification:

1. Check the logs output from the verification script
2. Ensure all prerequisites are installed
3. Verify environment variables are set correctly
4. Check the project documentation
5. Contact the development team

---

**Note:** These scripts are designed for the LuckXpress gambling platform and include specific compliance checks required for regulated gambling software.
