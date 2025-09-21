# LuckXpress Kubernetes Deployment Script
# This script deploys the LuckXpress application to Kubernetes

param(
    [string]$Environment = "production",
    [string]$Version = "latest",
    [switch]$DryRun = $false,
    [switch]$Infrastructure = $false,
    [switch]$Application = $false,
    [switch]$All = $false
)

Write-Host "LuckXpress Kubernetes Deployment" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Green
Write-Host "Environment: $Environment" -ForegroundColor Yellow
Write-Host "Version: $Version" -ForegroundColor Yellow

# Set kubeconfig context
$contextName = "luckxpress-$Environment"
Write-Host "Setting kubectl context to: $contextName" -ForegroundColor Cyan

try {
    kubectl config use-context $contextName
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error: Failed to set kubectl context. Please check your kubeconfig." -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "Error: kubectl not found or context not available." -ForegroundColor Red
    exit 1
}

# Function to apply Kubernetes manifests
function Apply-Manifests {
    param([string]$Path, [string]$Description)
    
    Write-Host "Deploying $Description..." -ForegroundColor Cyan
    
    if ($DryRun) {
        Write-Host "DRY RUN: Would apply manifests from $Path" -ForegroundColor Yellow
        kubectl apply -f $Path --dry-run=client
    } else {
        kubectl apply -f $Path
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Error: Failed to apply $Description" -ForegroundColor Red
            return $false
        }
    }
    return $true
}

# Function to wait for deployment
function Wait-ForDeployment {
    param([string]$DeploymentName, [string]$Namespace)
    
    Write-Host "Waiting for deployment $DeploymentName to be ready..." -ForegroundColor Cyan
    
    if (-not $DryRun) {
        kubectl rollout status deployment/$DeploymentName -n $Namespace --timeout=600s
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Error: Deployment $DeploymentName failed to become ready" -ForegroundColor Red
            return $false
        }
    }
    return $true
}

# Set deployment directory
$deployDir = Join-Path $PSScriptRoot "..\production"

# Deploy infrastructure components
if ($Infrastructure -or $All) {
    Write-Host "`n=== DEPLOYING INFRASTRUCTURE ===" -ForegroundColor Green
    
    # Create namespace and RBAC
    if (-not (Apply-Manifests "$deployDir\namespace.yaml" "Namespace and Resource Quota")) { exit 1 }
    if (-not (Apply-Manifests "$deployDir\rbac.yaml" "RBAC Configuration")) { exit 1 }
    
    # Deploy secrets (Note: In production, use external secret management)
    if (-not (Apply-Manifests "$deployDir\secrets.yaml" "Secrets")) { exit 1 }
    
    # Deploy infrastructure services
    if (-not (Apply-Manifests "$deployDir\infrastructure.yaml" "Infrastructure Services")) { exit 1 }
    
    # Deploy network policies
    if (-not (Apply-Manifests "$deployDir\network-policy.yaml" "Network Policies")) { exit 1 }
    
    Write-Host "Infrastructure deployment completed!" -ForegroundColor Green
}

# Deploy application components
if ($Application -or $All) {
    Write-Host "`n=== DEPLOYING APPLICATION ===" -ForegroundColor Green
    
    # Update image version
    if ($Version -ne "latest") {
        Write-Host "Updating image version to: $Version" -ForegroundColor Cyan
        
        # Use kustomize to set image version
        $kustomizeDir = $deployDir
        (Get-Content "$kustomizeDir\kustomization.yaml") -replace "newTag: .*", "newTag: $Version" | Set-Content "$kustomizeDir\kustomization.yaml"
    }
    
    # Deploy configuration
    if (-not (Apply-Manifests "$deployDir\configmap.yaml" "Configuration Maps")) { exit 1 }
    
    # Deploy application
    if (-not (Apply-Manifests "$deployDir\deployment.yaml" "Application Deployment")) { exit 1 }
    
    # Deploy services
    if (-not (Apply-Manifests "$deployDir\service.yaml" "Services")) { exit 1 }
    
    # Deploy ingress
    if (-not (Apply-Manifests "$deployDir\ingress.yaml" "Ingress Configuration")) { exit 1 }
    
    # Deploy PDB and monitoring
    if (-not (Apply-Manifests "$deployDir\pdb.yaml" "Pod Disruption Budget and Monitoring")) { exit 1 }
    
    # Wait for deployment to be ready
    if (-not (Wait-ForDeployment "luckxpress-backend" "luckxpress-prod")) { exit 1 }
    
    Write-Host "Application deployment completed!" -ForegroundColor Green
}

# Deploy everything if no specific component specified
if (-not $Infrastructure -and -not $Application -and -not $All) {
    Write-Host "No deployment target specified. Use -Infrastructure, -Application, or -All" -ForegroundColor Yellow
    Write-Host "Available options:" -ForegroundColor Cyan
    Write-Host "  -Infrastructure : Deploy infrastructure components only" -ForegroundColor White
    Write-Host "  -Application    : Deploy application components only" -ForegroundColor White
    Write-Host "  -All           : Deploy everything" -ForegroundColor White
    Write-Host "  -DryRun        : Show what would be deployed without applying" -ForegroundColor White
    exit 1
}

# Display deployment status
if (-not $DryRun) {
    Write-Host "`n=== DEPLOYMENT STATUS ===" -ForegroundColor Green
    
    # Show pods status
    Write-Host "`nPods:" -ForegroundColor Cyan
    kubectl get pods -n luckxpress-prod -o wide
    
    # Show services
    Write-Host "`nServices:" -ForegroundColor Cyan
    kubectl get services -n luckxpress-prod
    
    # Show ingress
    Write-Host "`nIngress:" -ForegroundColor Cyan
    kubectl get ingress -n luckxpress-prod
    
    # Show application endpoints
    Write-Host "`n=== APPLICATION ENDPOINTS ===" -ForegroundColor Green
    $ingressIP = kubectl get ingress luckxpress-ingress -n luckxpress-prod -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>$null
    if ($ingressIP) {
        Write-Host "Application URL: https://api.luckxpress.com" -ForegroundColor Cyan
        Write-Host "Admin URL: https://admin-api.luckxpress.com" -ForegroundColor Cyan
        Write-Host "Ingress IP: $ingressIP" -ForegroundColor Yellow
    } else {
        Write-Host "Ingress IP not yet assigned. Check again in a few minutes." -ForegroundColor Yellow
    }
}

Write-Host "`n🎉 Deployment completed successfully!" -ForegroundColor Green
Write-Host "Use 'kubectl get all -n luckxpress-prod' to check status" -ForegroundColor Yellow
