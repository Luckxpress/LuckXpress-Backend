@echo off
echo 🧪 LuckXpress Dashboard End-to-End Testing
echo ==========================================
echo.
echo 1. Testing Backend API Endpoints
echo -----------------------------------
echo Testing /api/v1/dashboard/metrics...
curl -s http://localhost:8080/api/v1/dashboard/metrics >nul 2>&1
if %errorlevel%==0 (
    echo ✓ PASSED
    curl -s http://localhost:8080/api/v1/dashboard/metrics
) else (
    echo ✗ FAILED
)
echo.
echo Testing /api/v1/dashboard/revenue-trend...
curl -s http://localhost:8080/api/v1/dashboard/revenue-trend >nul 2>&1
if %errorlevel%==0 (
    echo ✓ PASSED
) else (
    echo ✗ FAILED
)
echo.
echo Testing /api/v1/dashboard/conversion-funnel...
curl -s http://localhost:8080/api/v1/dashboard/conversion-funnel >nul 2>&1
if %errorlevel%==0 (
    echo ✓ PASSED
) else (
    echo ✗ FAILED
)
echo.
echo Testing /api/v1/dashboard/provider-status...
curl -s http://localhost:8080/api/v1/dashboard/provider-status >nul 2>&1
if %errorlevel%==0 (
    echo ✓ PASSED
) else (
    echo ✗ FAILED
)
echo.
echo 2. Testing Frontend Dashboard
echo -----------------------------------
echo Testing Frontend availability...
curl -s http://localhost:3000 >nul 2>&1
if %errorlevel%==0 (
    echo ✓ PASSED
) else (
    echo ✗ FAILED
)
echo.
echo 3. Component Testing Checklist
echo -----------------------------------
echo Please manually verify in browser (http://localhost:3000):
echo □ Dashboard loads without errors
echo □ Metric cards display values
echo □ Revenue trend chart renders
echo □ Conversion funnel shows data
echo □ Activity feed updates
echo □ Provider status displays correctly
echo □ Sidebar navigation works
echo □ Top bar buttons are clickable
echo.
echo Testing Complete!
