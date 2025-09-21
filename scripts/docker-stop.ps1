# LuckXpress Docker Stop Script
# This script stops the LuckXpress application and all dependencies

param(
    [switch]$RemoveVolumes = $false,
    [switch]$CleanImages = $false,
    [switch]$Backup = $false
)

Write-Host "Stopping LuckXpress Docker Environment..." -ForegroundColor Yellow

# Backup database if requested
if ($Backup) {
    Write-Host "Creating database backup..." -ForegroundColor Green
    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $backupFile = "backup_$timestamp.sql"
    
    docker-compose exec -T postgres pg_dump -U luckxpress luckxpress > "backups\$backupFile"
    Write-Host "Database backup created: backups\$backupFile" -ForegroundColor Green
}

# Stop all services
Write-Host "Stopping all services..." -ForegroundColor Yellow
if ($RemoveVolumes) {
    docker-compose down -v --remove-orphans
    Write-Host "All services stopped and volumes removed." -ForegroundColor Green
} else {
    docker-compose down --remove-orphans
    Write-Host "All services stopped." -ForegroundColor Green
}

# Clean up images if requested
if ($CleanImages) {
    Write-Host "Cleaning up unused Docker images..." -ForegroundColor Yellow
    docker image prune -f
    docker system prune -f
    Write-Host "Docker cleanup completed." -ForegroundColor Green
}

# Display volume status
if (-not $RemoveVolumes) {
    Write-Host "`nPersistent volumes retained:" -ForegroundColor Cyan
    docker volume ls --filter name=luckxpress
}

Write-Host "`nLuckXpress Docker environment stopped successfully!" -ForegroundColor Green
