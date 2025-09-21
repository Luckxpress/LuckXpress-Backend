# LuckXpress Docker Start Script
# This script starts the LuckXpress application with all dependencies

param(
    [string]$Environment = "prod",
    [switch]$Build = $false,
    [switch]$Clean = $false,
    [switch]$Logs = $false
)

Write-Host "Starting LuckXpress Docker Environment..." -ForegroundColor Green
Write-Host "Environment: $Environment" -ForegroundColor Yellow

# Check if .env file exists
if (-not (Test-Path ".env")) {
    Write-Host "Warning: .env file not found. Creating from template..." -ForegroundColor Yellow
    Copy-Item ".env.template" ".env"
    Write-Host "Please edit the .env file with your configuration before continuing." -ForegroundColor Red
    exit 1
}

# Clean up if requested
if ($Clean) {
    Write-Host "Cleaning up existing containers and volumes..." -ForegroundColor Yellow
    docker-compose down -v --remove-orphans
    docker system prune -f
}

# Build if requested
if ($Build) {
    Write-Host "Building application..." -ForegroundColor Yellow
    docker-compose build --no-cache
}

# Start services based on environment
switch ($Environment.ToLower()) {
    "dev" {
        Write-Host "Starting development environment..." -ForegroundColor Green
        docker-compose -f docker-compose.yml -f docker-dev.yml up -d
        Write-Host "Development services started:" -ForegroundColor Green
        Write-Host "  - Application: http://localhost:8080" -ForegroundColor Cyan
        Write-Host "  - Management: http://localhost:8081" -ForegroundColor Cyan
        Write-Host "  - PgAdmin: http://localhost:8082" -ForegroundColor Cyan
        Write-Host "  - Redis Commander: http://localhost:8083" -ForegroundColor Cyan
        Write-Host "  - Grafana: http://localhost:3000" -ForegroundColor Cyan
        Write-Host "  - Prometheus: http://localhost:9090" -ForegroundColor Cyan
    }
    "prod" {
        Write-Host "Starting production environment..." -ForegroundColor Green
        docker-compose up -d
        Write-Host "Production services started:" -ForegroundColor Green
        Write-Host "  - Application: http://localhost:8080" -ForegroundColor Cyan
        Write-Host "  - Management: http://localhost:8081" -ForegroundColor Cyan
        Write-Host "  - Grafana: http://localhost:3000" -ForegroundColor Cyan
        Write-Host "  - Prometheus: http://localhost:9090" -ForegroundColor Cyan
    }
    default {
        Write-Host "Invalid environment: $Environment. Use 'dev' or 'prod'" -ForegroundColor Red
        exit 1
    }
}

# Show logs if requested
if ($Logs) {
    Write-Host "Following application logs..." -ForegroundColor Yellow
    docker-compose logs -f app
}

Write-Host "LuckXpress Docker environment started successfully!" -ForegroundColor Green
Write-Host "Use 'docker-compose logs -f' to view logs" -ForegroundColor Yellow
Write-Host "Use 'docker-compose down' to stop services" -ForegroundColor Yellow
