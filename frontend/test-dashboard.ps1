# LuckXpress Dashboard End-to-End Testing
Write-Host "🧪 LuckXpress Dashboard End-to-End Testing" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# Base URLs
$BACKEND_URL = "http://localhost:8080"
$FRONTEND_URL = "http://localhost:3000"

# Test Backend APIs
Write-Host ""
Write-Host "1. Testing Backend API Endpoints" -ForegroundColor Yellow
Write-Host "-----------------------------------"

# Test metrics endpoint
Write-Host -NoNewline "Testing /api/v1/dashboard/metrics... "
try {
    $response = Invoke-WebRequest -Uri "$BACKEND_URL/api/v1/dashboard/metrics" -Method GET -TimeoutSec 10
    if ($response.StatusCode -eq 200) {
        Write-Host "✓ PASSED" -ForegroundColor Green
        $response.Content | ConvertFrom-Json | ConvertTo-Json
    } else {
        Write-Host "✗ FAILED (HTTP $($response.StatusCode))" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ FAILED ($($_.Exception.Message))" -ForegroundColor Red
}

# Test revenue trend endpoint
Write-Host -NoNewline "Testing /api/v1/dashboard/revenue-trend... "
try {
    $response = Invoke-WebRequest -Uri "$BACKEND_URL/api/v1/dashboard/revenue-trend" -Method GET -TimeoutSec 10
    if ($response.StatusCode -eq 200) {
        Write-Host "✓ PASSED" -ForegroundColor Green
    } else {
        Write-Host "✗ FAILED (HTTP $($response.StatusCode))" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ FAILED ($($_.Exception.Message))" -ForegroundColor Red
}

# Test conversion funnel endpoint
Write-Host -NoNewline "Testing /api/v1/dashboard/conversion-funnel... "
try {
    $response = Invoke-WebRequest -Uri "$BACKEND_URL/api/v1/dashboard/conversion-funnel" -Method GET -TimeoutSec 10
    if ($response.StatusCode -eq 200) {
        Write-Host "✓ PASSED" -ForegroundColor Green
    } else {
        Write-Host "✗ FAILED (HTTP $($response.StatusCode))" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ FAILED ($($_.Exception.Message))" -ForegroundColor Red
}

# Test provider status endpoint
Write-Host -NoNewline "Testing /api/v1/dashboard/provider-status... "
try {
    $response = Invoke-WebRequest -Uri "$BACKEND_URL/api/v1/dashboard/provider-status" -Method GET -TimeoutSec 10
    if ($response.StatusCode -eq 200) {
        Write-Host "✓ PASSED" -ForegroundColor Green
    } else {
        Write-Host "✗ FAILED (HTTP $($response.StatusCode))" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ FAILED ($($_.Exception.Message))" -ForegroundColor Red
}

# Test Frontend
Write-Host ""
Write-Host "2. Testing Frontend Dashboard" -ForegroundColor Yellow
Write-Host "-----------------------------------"

Write-Host -NoNewline "Testing Frontend availability... "
try {
    $response = Invoke-WebRequest -Uri $FRONTEND_URL -Method GET -TimeoutSec 10
    if ($response.StatusCode -eq 200) {
        Write-Host "✓ PASSED" -ForegroundColor Green
    } else {
        Write-Host "✗ FAILED (HTTP $($response.StatusCode))" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ FAILED ($($_.Exception.Message))" -ForegroundColor Red
}

Write-Host ""
Write-Host "3. Component Testing Checklist" -ForegroundColor Yellow
Write-Host "-----------------------------------"
Write-Host "Please manually verify in browser ($FRONTEND_URL):"
Write-Host "□ Dashboard loads without errors"
Write-Host "□ Metric cards display values"
Write-Host "□ Revenue trend chart renders"
Write-Host "□ Conversion funnel shows data"
Write-Host "□ Activity feed updates"
Write-Host "□ Provider status displays correctly"
Write-Host "□ Sidebar navigation works"
Write-Host "□ Top bar buttons are clickable"

Write-Host ""
Write-Host "Testing Complete!" -ForegroundColor Green
