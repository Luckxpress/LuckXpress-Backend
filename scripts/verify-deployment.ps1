# LuckXpress Backend Deployment Verification Script (PowerShell)

Write-Host "🎰 LuckXpress Backend Deployment Verification Script" -ForegroundColor Cyan
Write-Host "====================================================" -ForegroundColor Cyan
Write-Host ""

# Function to check command success
function Check-Status {
    param(
        [string]$Message,
        [bool]$Success = $true
    )
    
    if ($Success) {
        Write-Host "✓ $Message" -ForegroundColor Green
        return $true
    } else {
        Write-Host "✗ $Message" -ForegroundColor Red
        return $false
    }
}

# Function to check environment variable
function Check-EnvVar {
    param([string]$VarName)
    
    $value = [Environment]::GetEnvironmentVariable($VarName)
    if ([string]::IsNullOrEmpty($value)) {
        Write-Host "✗ Environment variable $VarName is not set" -ForegroundColor Red
        return $false
    } else {
        Write-Host "✓ Environment variable $VarName is set" -ForegroundColor Green
        return $true
    }
}

# 1. Check Java Version
Write-Host "1. Checking Java Version..."
try {
    $javaVersion = & java -version 2>&1 | Select-String "version" | ForEach-Object { $_.ToString() }
    if ($javaVersion -match '"21\.' -or $javaVersion -match '"21"') {
        Check-Status "Java 21 is installed"
    } else {
        Write-Host "✗ Java 21 is required" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ Java is not installed or not in PATH" -ForegroundColor Red
}

# 2. Check Maven Version
Write-Host ""
Write-Host "2. Checking Maven Version..."
try {
    $mavenVersion = & mvn -version | Select-String "Apache Maven" | ForEach-Object { $_.ToString() }
    if ($mavenVersion -match "3\.9") {
        Check-Status "Maven 3.9.x is installed"
    } else {
        Write-Host "⚠ Maven 3.9.9 recommended" -ForegroundColor Yellow
    }
} catch {
    Write-Host "✗ Maven is not installed or not in PATH" -ForegroundColor Red
}

# 3. Build Project
Write-Host ""
Write-Host "3. Building Project..."
try {
    $buildResult = & mvn clean compile -q 2>&1
    if ($LASTEXITCODE -eq 0) {
        Check-Status "Project compiles successfully"
    } else {
        Check-Status "Project compilation failed" $false
    }
} catch {
    Check-Status "Build command failed" $false
}

# 4. Run Unit Tests
Write-Host ""
Write-Host "4. Running Unit Tests..."
try {
    $testResult = & mvn test -Dspring.profiles.active=test -q 2>&1
    if ($LASTEXITCODE -eq 0) {
        Check-Status "Unit tests pass"
    } else {
        Check-Status "Unit tests failed" $false
    }
} catch {
    Check-Status "Test command failed" $false
}

# 5. Check Required Environment Variables
Write-Host ""
Write-Host "6. Checking Environment Variables..."
$requiredVars = @(
    "SENTRY_DSN",
    "DB_PASSWORD", 
    "JWT_SECRET",
    "REDIS_PASSWORD",
    "OAUTH2_CLIENT_SECRET"
)

foreach ($var in $requiredVars) {
    Check-EnvVar $var
}

# 6. Check Docker
Write-Host ""
Write-Host "7. Checking Docker..."
try {
    & docker --version | Out-Null
    Check-Status "Docker is installed"
    
    & docker compose version | Out-Null
    Check-Status "Docker Compose is installed"
} catch {
    Write-Host "✗ Docker or Docker Compose not available" -ForegroundColor Red
}

# 7. Check Database Migration
Write-Host ""
Write-Host "8. Checking Database Migrations..."
try {
    $flywayResult = & mvn flyway:info -q 2>&1
    if ($LASTEXITCODE -eq 0) {
        Check-Status "Database migrations are valid"
    } else {
        Check-Status "Database migration check failed" $false
    }
} catch {
    Check-Status "Flyway command failed" $false
}

# 8. Check API Documentation
Write-Host ""
Write-Host "9. Checking API Documentation..."
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/v3/api-docs" -UseBasicParsing -TimeoutSec 5 -ErrorAction SilentlyContinue
    if ($response.StatusCode -eq 200) {
        Check-Status "Swagger API documentation is accessible"
    } else {
        Write-Host "⚠ API documentation not accessible (app may not be running)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠ API documentation not accessible (app may not be running)" -ForegroundColor Yellow
}

# 9. Check Postman Collection
Write-Host ""
Write-Host "11. Checking Postman Collection..."
if (Test-Path "postman/LuckXpress-API-Collection.json") {
    Check-Status "Postman collection exists"
} else {
    Check-Status "Postman collection not found" $false
}

# 10. Check GitHub Actions
Write-Host ""
Write-Host "12. Checking GitHub Actions..."
if (Test-Path ".github/workflows/ci-cd-pipeline.yml") {
    Check-Status "GitHub Actions workflow configured"
} else {
    Check-Status "GitHub Actions workflow not found" $false
}

# 11. Check Compliance Requirements
Write-Host ""
Write-Host "14. Verifying Compliance Requirements..."

# Check for BigDecimal usage
try {
    $floatUsage = Get-ChildItem -Path "luckxpress-service/src/main/java" -Include "*.java" -Recurse | 
                  Select-String -Pattern "(float|Float|double|Double)" | 
                  Where-Object { $_.Line -notmatch "BigDecimal" }
    
    if ($floatUsage.Count -eq 0) {
        Check-Status "No float/double used for money (BigDecimal only)"
    } else {
        Write-Host "✗ Found float/double usage for money - CRITICAL COMPLIANCE ISSUE" -ForegroundColor Red
    }
} catch {
    Write-Host "⚠ Could not verify BigDecimal usage" -ForegroundColor Yellow
}

# Check for monitoring files
if (Test-Path "monitoring/prometheus.yml") {
    Check-Status "Prometheus monitoring configured"
} else {
    Check-Status "Prometheus monitoring not configured" $false
}

if (Test-Path "monitoring/alerts/compliance-alerts.yml") {
    Check-Status "Compliance alerts configured"
} else {
    Check-Status "Compliance alerts not configured" $false
}

# 12. Generate Report
Write-Host ""
Write-Host "====================================================" -ForegroundColor Cyan
Write-Host "Verification Complete!" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next Steps:"
Write-Host "1. Review any failed checks above"
Write-Host "2. Configure missing environment variables"
Write-Host "3. Run 'docker compose up' to start services"
Write-Host "4. Import Postman collection for API testing"
Write-Host "5. Configure Sentry alerts in production"
Write-Host ""
Write-Host "📚 Documentation: https://github.com/your-org/luckxpress-backend/wiki"
Write-Host "🐛 Sentry Dashboard: https://sentry.io/organizations/luckxpress"
Write-Host "📊 Grafana Dashboard: http://localhost:3000 (admin/admin)"
Write-Host ""
Write-Host "Happy coding! 🎰" -ForegroundColor Green
