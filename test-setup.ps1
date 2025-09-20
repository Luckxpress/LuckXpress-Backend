# LuckXpress Backend Setup Test Script
Write-Host "=== LuckXpress Backend Setup Test ===" -ForegroundColor Green

# Test 1: Check Git configuration
Write-Host "`n1. Testing Git Configuration..." -ForegroundColor Yellow
git --version
git config user.name
git config user.email
git remote -v

# Test 2: Check Java and Maven
Write-Host "`n2. Testing Java and Maven..." -ForegroundColor Yellow
java -version
mvn -version

# Test 3: Test Maven build (quick validation)
Write-Host "`n3. Testing Maven Build (validation only)..." -ForegroundColor Yellow
mvn validate

# Test 4: Check Docker
Write-Host "`n4. Testing Docker..." -ForegroundColor Yellow
docker --version
docker-compose --version

# Test 5: Validate project structure
Write-Host "`n5. Validating Project Structure..." -ForegroundColor Yellow
$modules = @("luckxpress-common", "luckxpress-core", "luckxpress-data", "luckxpress-service", "luckxpress-web", "luckxpress-app")
foreach ($module in $modules) {
    if (Test-Path $module) {
        Write-Host "✓ $module exists" -ForegroundColor Green
    } else {
        Write-Host "✗ $module missing" -ForegroundColor Red
    }
}

# Test 6: Check GitHub files
Write-Host "`n6. Validating GitHub Configuration..." -ForegroundColor Yellow
$githubFiles = @(".github/workflows/ci.yml", ".github/CODEOWNERS", ".github/pull_request_template.md")
foreach ($file in $githubFiles) {
    if (Test-Path $file) {
        Write-Host "✓ $file exists" -ForegroundColor Green
    } else {
        Write-Host "✗ $file missing" -ForegroundColor Red
    }
}

Write-Host "`n=== Setup Test Complete ===" -ForegroundColor Green
Write-Host "If all items show checkmarks, your setup is ready!" -ForegroundColor Cyan
