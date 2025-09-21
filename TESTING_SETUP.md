# LuckXpress Testing Infrastructure Setup Guide

## 🚀 **Testing Infrastructure Overview**

This guide will help you fully activate the comprehensive testing infrastructure for the LuckXpress platform, including CI/CD pipelines, security scans, compliance tests, and monitoring integration.

## 📋 **Prerequisites**

- Java 21 JDK
- Maven 3.8+
- Docker (for Testcontainers)
- Git repository access
- GitHub repository with Actions enabled

## 🔑 **Required GitHub Secrets**

Configure the following secrets in your GitHub repository settings (`Settings > Secrets and variables > Actions`):

### **Code Quality & Security**
```bash
SONAR_TOKEN=your_sonarqube_token
SONAR_HOST_URL=https://sonarcloud.io  # or your SonarQube instance
CODECOV_TOKEN=your_codecov_token
```

### **Sentry Integration** 
```bash
SENTRY_AUTH_TOKEN=your_sentry_auth_token
SENTRY_DSN=https://your_sentry_dsn@sentry.io/project_id
```

### **Container Registry**
```bash
# GitHub Container Registry uses GITHUB_TOKEN (automatically provided)
# For other registries, add:
DOCKER_USERNAME=your_docker_username
DOCKER_PASSWORD=your_docker_password
```

### **Kubernetes Deployment**
```bash
KUBE_CONFIG_DATA=your_base64_encoded_kubeconfig
KUBE_NAMESPACE=luckxpress-staging
```

## 🛠 **Maven Configuration**

### **Run Different Test Types**

```bash
# Unit tests only
mvn clean test

# Integration tests only  
mvn clean verify -P integration-test -DskipUnitTests

# All tests with coverage
mvn clean verify

# Security scans
mvn clean verify -P security-scan

# Coverage check (enforces 80% threshold)
mvn jacoco:check
```

### **Profiles Available**

- `local` - Default development profile
- `test` - Test environment configuration
- `integration-test` - Runs integration tests with Testcontainers
- `security-scan` - Runs SpotBugs and OWASP dependency checks

## 🐳 **Docker Setup for Tests**

Ensure Docker is running for Testcontainers integration tests:

```bash
# Verify Docker is running
docker --version
docker ps

# Pull required test images (optional - Testcontainers will do this)
docker pull postgres:15
docker pull redis:7-alpine
```

## 🔐 **Security Configuration Files**

Create these optional security configuration files in the project root:

### **SpotBugs Security Filter** (`spotbugs-security.xml`)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<FindBugsFilter>
    <Match>
        <Bug category="SECURITY"/>
    </Match>
    <Match>
        <Bug category="BAD_PRACTICE"/>
    </Match>
    <Match>
        <Bug category="PERFORMANCE"/>
    </Match>
</FindBugsFilter>
```

### **OWASP Suppressions** (`owasp-suppressions.xml`)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
    <!-- Suppress false positives here -->
    <!-- Example:
    <suppress>
        <notes>False positive - this vulnerability doesn't apply to our use case</notes>
        <gav regex="true">^org\.springframework.*$</gav>
        <cve>CVE-2023-XXXXX</cve>
    </suppress>
    -->
</suppressions>
```

## 🎯 **GitHub Environments**

Configure deployment environments in GitHub (`Settings > Environments`):

### **Staging Environment**
- **Name**: `staging`
- **Deployment branches**: `develop` branch only
- **Required reviewers**: 1 reviewer minimum
- **Protection rules**: Enable

### **Production Environment** 
- **Name**: `production` 
- **Deployment branches**: `main` branch only
- **Required reviewers**: 2 reviewers minimum
- **Protection rules**: Enable with 6-hour delay

## 🧪 **Running Tests Locally**

### **Quick Test Run**
```bash
# Full test suite with coverage
mvn clean verify

# Integration tests only
mvn clean verify -P integration-test

# Watch test output
mvn clean test -Dmaven.test.failure.ignore=true | tee test-output.log
```

### **Debugging Integration Tests**
```bash
# Run with debug logging
mvn clean verify -P integration-test -Dlogging.level.com.luckxpress=DEBUG

# Run specific test class
mvn clean test -Dtest=ComplianceIntegrationTest

# Run with Testcontainers debug
mvn clean verify -Dtestcontainers.logger=DEBUG
```

## 📊 **Test Reports & Coverage**

After running tests, view reports:

- **JUnit Reports**: `target/surefire-reports/`
- **Integration Test Reports**: `target/failsafe-reports/`
- **JaCoCo Coverage**: `target/site/jacoco/index.html`
- **JaCoCo Aggregate**: `target/site/jacoco-aggregate/index.html`
- **SpotBugs**: `target/spotbugs-reports/`
- **OWASP**: `target/dependency-check-report.html`

## 🚀 **CI/CD Pipeline Features**

The `ci-cd-pipeline.yml` includes:

### **Automated Stages**
1. **Code Quality** - SpotBugs, OWASP, SonarQube
2. **Testing** - Unit tests, integration tests, coverage
3. **API Testing** - Postman collection via Newman
4. **Docker Build** - Container creation and registry push
5. **Deployment** - Staging environment deployment

### **Security Features**
- Daily security scans at 2 AM
- CVSS 7+ threshold for security failures
- Dependency vulnerability scanning
- Code quality gates with SonarQube

### **Monitoring Integration**
- Sentry release tracking
- Code coverage reporting to Codecov
- Test result artifacts
- Security scan reports

## 🔧 **Troubleshooting**

### **Common Issues**

**Docker not found during tests:**
```bash
# Ensure Docker daemon is running
sudo systemctl start docker  # Linux
# or start Docker Desktop on Windows/Mac
```

**Port conflicts in tests:**
```bash
# Kill processes on common test ports
sudo kill $(sudo lsof -t -i:5432)  # PostgreSQL
sudo kill $(sudo lsof -t -i:6379)  # Redis
```

**Maven dependency issues:**
```bash
# Clear Maven cache and re-download
rm -rf ~/.m2/repository
mvn clean install
```

**Testcontainers issues:**
```bash
# Clear Testcontainers cache
rm -rf ~/.testcontainers
docker system prune -f
```

### **Debug Mode**

Enable debug logging for troubleshooting:

```yaml
# Add to application-test.yml
logging:
  level:
    org.testcontainers: DEBUG
    io.restassured: DEBUG
    com.luckxpress: DEBUG
```

## ✅ **Verification Checklist**

- [ ] All GitHub secrets configured
- [ ] Docker running for Testcontainers
- [ ] Maven builds successfully: `mvn clean compile`
- [ ] Unit tests pass: `mvn clean test`
- [ ] Integration tests pass: `mvn clean verify -P integration-test`
- [ ] Security scans pass: `mvn clean verify -P security-scan`
- [ ] Coverage meets threshold: `mvn jacoco:check`
- [ ] CI pipeline runs successfully on push
- [ ] SonarQube analysis completes
- [ ] Sentry integration working

## 🆘 **Support**

For issues with the testing infrastructure:

1. Check the [GitHub Actions logs](../../actions)
2. Review test output in `target/surefire-reports/`
3. Verify all required secrets are configured
4. Ensure Docker is running for integration tests
5. Check Maven dependencies are up to date

## 📚 **Additional Resources**

- [Testcontainers Documentation](https://www.testcontainers.org/)
- [RestAssured Guide](https://rest-assured.io/)
- [JaCoCo Maven Plugin](https://www.jacoco.org/jacoco/trunk/doc/maven.html)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [SonarQube Integration](https://sonarcloud.io/documentation/)
- [Sentry Integration Guide](https://docs.sentry.io/platforms/java/)
