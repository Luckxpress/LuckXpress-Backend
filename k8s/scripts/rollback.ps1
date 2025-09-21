# LuckXpress Kubernetes Rollback Script
# This script performs rollback operations for the LuckXpress deployment

param(
    [string]$Namespace = "luckxpress-prod",
    [string]$Deployment = "luckxpress-backend",
    [int]$Revision = 0,
    [switch]$DryRun = $false,
    [switch]$History = $false
)

Write-Host "LuckXpress Kubernetes Rollback Tool" -ForegroundColor Green
Write-Host "===================================" -ForegroundColor Green

# Function to show rollout history
function Show-RolloutHistory {
    param([string]$DeploymentName, [string]$Namespace)
    
    Write-Host "`n📜 Rollout History for $DeploymentName:" -ForegroundColor Cyan
    Write-Host "$(('-' * 50))" -ForegroundColor Gray
    
    kubectl rollout history deployment/$DeploymentName -n $Namespace
}

# Function to get deployment status
function Get-DeploymentStatus {
    param([string]$DeploymentName, [string]$Namespace)
    
    Write-Host "`n📊 Current Deployment Status:" -ForegroundColor Cyan
    Write-Host "$(('-' * 30))" -ForegroundColor Gray
    
    $deployment = kubectl get deployment $DeploymentName -n $Namespace -o json | ConvertFrom-Json
    
    $replicas = $deployment.spec.replicas
    $readyReplicas = if ($deployment.status.readyReplicas) { $deployment.status.readyReplicas } else { 0 }
    $updatedReplicas = if ($deployment.status.updatedReplicas) { $deployment.status.updatedReplicas } else { 0 }
    
    Write-Host "Desired Replicas: $replicas" -ForegroundColor White
    Write-Host "Ready Replicas: $readyReplicas" -ForegroundColor White
    Write-Host "Updated Replicas: $updatedReplicas" -ForegroundColor White
    
    # Get current image version
    $currentImage = $deployment.spec.template.spec.containers[0].image
    Write-Host "Current Image: $currentImage" -ForegroundColor Yellow
    
    if ($readyReplicas -eq $replicas) {
        Write-Host "✅ Deployment is healthy" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Deployment has issues" -ForegroundColor Yellow
    }
}

# Function to perform rollback
function Start-Rollback {
    param([string]$DeploymentName, [string]$Namespace, [int]$ToRevision, [bool]$DryRun)
    
    if ($ToRevision -eq 0) {
        Write-Host "`n🔄 Rolling back to previous revision..." -ForegroundColor Cyan
        
        if ($DryRun) {
            Write-Host "DRY RUN: Would rollback deployment $DeploymentName to previous revision" -ForegroundColor Yellow
        } else {
            kubectl rollout undo deployment/$DeploymentName -n $Namespace
        }
    } else {
        Write-Host "`n🔄 Rolling back to revision $ToRevision..." -ForegroundColor Cyan
        
        if ($DryRun) {
            Write-Host "DRY RUN: Would rollback deployment $DeploymentName to revision $ToRevision" -ForegroundColor Yellow
        } else {
            kubectl rollout undo deployment/$DeploymentName --to-revision=$ToRevision -n $Namespace
        }
    }
    
    if (-not $DryRun) {
        Write-Host "Waiting for rollback to complete..." -ForegroundColor Yellow
        kubectl rollout status deployment/$DeploymentName -n $Namespace --timeout=600s
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Rollback completed successfully!" -ForegroundColor Green
        } else {
            Write-Host "❌ Rollback failed!" -ForegroundColor Red
            return $false
        }
    }
    
    return $true
}

# Function to verify rollback
function Test-RollbackSuccess {
    param([string]$DeploymentName, [string]$Namespace)
    
    Write-Host "`n🔍 Verifying rollback success..." -ForegroundColor Cyan
    
    # Wait a bit for pods to stabilize
    Start-Sleep -Seconds 10
    
    # Check pod health
    $pods = kubectl get pods -n $Namespace -l app=luckxpress --no-headers
    $healthyPods = ($pods | Where-Object { $_ -match "Running.*1/1" }).Count
    $totalPods = $pods.Count
    
    Write-Host "Pod Status: $healthyPods/$totalPods healthy" -ForegroundColor $(
        if ($healthyPods -eq $totalPods) { "Green" } else { "Red" }
    )
    
    # Test application health endpoint
    try {
        Write-Host "Testing application health endpoint..." -ForegroundColor Cyan
        
        # Port forward to test health
        $portForwardJob = Start-Job -ScriptBlock {
            kubectl port-forward deployment/luckxpress-backend 8081:8081 -n $using:Namespace
        }
        
        Start-Sleep -Seconds 5
        
        $healthResponse = Invoke-WebRequest -Uri "http://localhost:8081/actuator/health" -TimeoutSec 10 -UseBasicParsing -ErrorAction SilentlyContinue
        
        Stop-Job $portForwardJob -Force
        Remove-Job $portForwardJob -Force
        
        if ($healthResponse -and $healthResponse.StatusCode -eq 200) {
            Write-Host "✅ Health endpoint is responding" -ForegroundColor Green
            return $true
        } else {
            Write-Host "❌ Health endpoint is not responding" -ForegroundColor Red
            return $false
        }
    } catch {
        Write-Host "⚠️  Could not test health endpoint: $($_.Exception.Message)" -ForegroundColor Yellow
        return $false
    }
}

# Main script execution
Write-Host "Target: $Deployment in namespace $Namespace" -ForegroundColor Yellow

# Check if deployment exists
$deploymentExists = kubectl get deployment $Deployment -n $Namespace 2>$null
if (-not $deploymentExists) {
    Write-Host "❌ Deployment '$Deployment' not found in namespace '$Namespace'!" -ForegroundColor Red
    exit 1
}

# Show current status
Get-DeploymentStatus $Deployment $Namespace

# Show history if requested
if ($History) {
    Show-RolloutHistory $Deployment $Namespace
    exit 0
}

# Confirm rollback operation
if (-not $DryRun) {
    Write-Host "`n⚠️  WARNING: This will rollback the production deployment!" -ForegroundColor Red
    Write-Host "This action cannot be easily undone." -ForegroundColor Yellow
    
    $confirmation = Read-Host "`nAre you sure you want to proceed? (yes/no)"
    if ($confirmation -ne "yes") {
        Write-Host "Rollback cancelled." -ForegroundColor Yellow
        exit 0
    }
}

# Perform rollback
$rollbackSuccess = Start-Rollback $Deployment $Namespace $Revision $DryRun

if ($rollbackSuccess -and -not $DryRun) {
    # Show updated status
    Get-DeploymentStatus $Deployment $Namespace
    
    # Verify rollback success
    $verifySuccess = Test-RollbackSuccess $Deployment $Namespace
    
    if ($verifySuccess) {
        Write-Host "`n🎉 Rollback completed and verified successfully!" -ForegroundColor Green
    } else {
        Write-Host "`n⚠️  Rollback completed but verification failed. Please check manually." -ForegroundColor Yellow
    }
    
    Write-Host "`n📝 Post-rollback checklist:" -ForegroundColor Cyan
    Write-Host "  1. Check application logs: kubectl logs -f deployment/$Deployment -n $Namespace" -ForegroundColor White
    Write-Host "  2. Monitor application metrics and alerts" -ForegroundColor White
    Write-Host "  3. Test critical application functions" -ForegroundColor White
    Write-Host "  4. Update incident documentation if applicable" -ForegroundColor White
    
} elseif (-not $DryRun) {
    Write-Host "`n❌ Rollback failed! Please check the deployment status and logs." -ForegroundColor Red
    Write-Host "Troubleshooting commands:" -ForegroundColor Yellow
    Write-Host "  kubectl describe deployment $Deployment -n $Namespace" -ForegroundColor White
    Write-Host "  kubectl get events -n $Namespace --sort-by='.lastTimestamp'" -ForegroundColor White
    exit 1
}

Write-Host "`nRollback operation completed." -ForegroundColor Green
