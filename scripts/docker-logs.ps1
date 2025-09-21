# LuckXpress Docker Logs Script
# This script provides easy access to Docker container logs

param(
    [string]$Service = "app",
    [int]$Lines = 100,
    [switch]$Follow = $false,
    [switch]$All = $false
)

Write-Host "LuckXpress Docker Logs Viewer" -ForegroundColor Green

if ($All) {
    Write-Host "Showing logs for all services..." -ForegroundColor Yellow
    if ($Follow) {
        docker-compose logs -f --tail=$Lines
    } else {
        docker-compose logs --tail=$Lines
    }
} else {
    Write-Host "Showing logs for service: $Service" -ForegroundColor Yellow
    
    # Validate service name
    $validServices = @("app", "postgres", "redis", "kafka", "zookeeper", "prometheus", "grafana")
    if ($validServices -notcontains $Service) {
        Write-Host "Invalid service name: $Service" -ForegroundColor Red
        Write-Host "Valid services: $($validServices -join ', ')" -ForegroundColor Yellow
        exit 1
    }
    
    if ($Follow) {
        docker-compose logs -f --tail=$Lines $Service
    } else {
        docker-compose logs --tail=$Lines $Service
    }
}

Write-Host "`nLog viewer finished." -ForegroundColor Green
