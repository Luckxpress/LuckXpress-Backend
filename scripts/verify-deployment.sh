#!/bin/bash

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "🎰 LuckXpress Backend Deployment Verification Script"
echo "===================================================="
echo ""

# Function to check command success
check_status() {
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $1"
        return 0
    else
        echo -e "${RED}✗${NC} $1"
        return 1
    fi
}

# Function to check environment variable
check_env() {
    if [ -z "${!1}" ]; then
        echo -e "${RED}✗${NC} Environment variable $1 is not set"
        return 1
    else
        echo -e "${GREEN}✓${NC} Environment variable $1 is set"
        return 0
    fi
}

# 1. Check Java Version
echo "1. Checking Java Version..."
java_version=$(java -version 2>&1 | head -n 1 | cut -d '"' -f 2 | cut -d '.' -f 1)
if [ "$java_version" == "21" ]; then
    check_status "Java 21 is installed"
else
    echo -e "${RED}✗${NC} Java 21 is required, found version $java_version"
fi

# 2. Check Maven Version
echo ""
echo "2. Checking Maven Version..."
maven_version=$(mvn -version | head -n 1 | cut -d ' ' -f 3 | cut -d '.' -f 1-2)
if [ "$maven_version" == "3.9" ]; then
    check_status "Maven 3.9.x is installed"
else
    echo -e "${YELLOW}⚠${NC} Maven 3.9.9 recommended, found $maven_version"
fi

# 3. Build Project
echo ""
echo "3. Building Project..."
mvn clean compile -q
check_status "Project compiles successfully"

# 4. Run Unit Tests
echo ""
echo "4. Running Unit Tests..."
mvn test -Dspring.profiles.active=test -q
check_status "Unit tests pass"

# 5. Check Compliance Tests
echo ""
echo "5. Running Compliance Tests..."
mvn test -Dtest=ComplianceIntegrationTest -q
check_status "Compliance tests pass"

# 6. Check Required Environment Variables
echo ""
echo "6. Checking Environment Variables..."
required_vars=(
    "SENTRY_DSN"
    "DB_PASSWORD"
    "JWT_SECRET"
    "REDIS_PASSWORD"
    "OAUTH2_CLIENT_SECRET"
)

for var in "${required_vars[@]}"; do
    check_env "$var"
done

# 7. Check Docker
echo ""
echo "7. Checking Docker..."
docker --version > /dev/null 2>&1
check_status "Docker is installed"

docker compose version > /dev/null 2>&1
check_status "Docker Compose is installed"

# 8. Check Database Migration
echo ""
echo "8. Checking Database Migrations..."
mvn flyway:info -q
check_status "Database migrations are valid"

# 9. Check API Documentation
echo ""
echo "9. Checking API Documentation..."
curl -s http://localhost:8080/v3/api-docs > /dev/null 2>&1
if [ $? -eq 0 ]; then
    check_status "Swagger API documentation is accessible"
else
    echo -e "${YELLOW}⚠${NC} API documentation not accessible (app may not be running)"
fi

# 10. Security Check
echo ""
echo "10. Running Security Checks..."
mvn org.owasp:dependency-check-maven:check -q
check_status "No critical security vulnerabilities"

# 11. Check Postman Collection
echo ""
echo "11. Checking Postman Collection..."
if [ -f "postman/LuckXpress-API-Collection.json" ]; then
    check_status "Postman collection exists"
else
    echo -e "${RED}✗${NC} Postman collection not found"
fi

# 12. Check GitHub Actions
echo ""
echo "12. Checking GitHub Actions..."
if [ -f ".github/workflows/ci-cd-pipeline.yml" ]; then
    check_status "GitHub Actions workflow configured"
else
    echo -e "${RED}✗${NC} GitHub Actions workflow not found"
fi

# 13. Verify Sentry Connection
echo ""
echo "13. Verifying Sentry Connection..."
if [ ! -z "$SENTRY_DSN" ]; then
    curl -X POST "$SENTRY_DSN/store/" \
         -H 'Content-Type: application/json' \
         -d '{"message":"Test connection"}' > /dev/null 2>&1
    check_status "Sentry connection verified"
else
    echo -e "${YELLOW}⚠${NC} Sentry DSN not configured"
fi

# 14. Check Compliance Requirements
echo ""
echo "14. Verifying Compliance Requirements..."

# Check for BigDecimal usage
grep -r "float\|Float\|double\|Double" --include="*.java" luckxpress-service/src/main/java | grep -v BigDecimal > /dev/null
if [ $? -ne 0 ]; then
    check_status "No float/double used for money (BigDecimal only)"
else
    echo -e "${RED}✗${NC} Found float/double usage for money - CRITICAL COMPLIANCE ISSUE"
fi

# Check for state restrictions
grep -r "WA\|ID" --include="*.java" luckxpress-common/src/main/java/**/StateRestriction.java > /dev/null
check_status "State restrictions (WA/ID) configured"

# Check for KYC requirements
grep -r "@RequiresKYC" --include="*.java" luckxpress-service/src/main/java > /dev/null
check_status "KYC requirements implemented"

# 15. Generate Report
echo ""
echo "===================================================="
echo "Verification Complete!"
echo ""
echo "Next Steps:"
echo "1. Review any failed checks above"
echo "2. Configure missing environment variables"
echo "3. Run 'docker compose up' to start services"
echo "4. Import Postman collection for API testing"
echo "5. Configure Sentry alerts in production"
echo ""
echo "📚 Documentation: https://github.com/your-org/luckxpress-backend/wiki"
echo "🐛 Sentry Dashboard: https://sentry.io/organizations/luckxpress"
echo "📊 Grafana Dashboard: http://localhost:3000 (admin/admin)"
echo ""
echo "Happy coding! 🎰"
