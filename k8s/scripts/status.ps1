# LuckXpress Kubernetes Status Check Script
# This script checks the status of the LuckXpress deployment

param(
    [string]$Namespace = "luckxpress-prod",
    [switch]$Detailed = $false,
    [switch]$Logs = $false,
    [string]$Component = "all"
)

Write-Host "LuckXpress Kubernetes Status Check" -ForegroundColor Green
Write-Host "==================================" -ForegroundColor Green

# Function to check pod health
function Get-PodHealth {
    param([string]$Namespace)
    
    Write-Host "`n📊 Pod Health Status:" -ForegroundColor Cyan
    Write-Host "---------------------" -ForegroundColor Cyan
    
    $pods = kubectl get pods -n $Namespace -o json | ConvertFrom-Json
    
    foreach ($pod in $pods.items) {
        $name = $pod.metadata.name
        $status = $pod.status.phase
        $ready = 0
        $total = $pod.spec.containers.Count
        
        # Count ready containers
        if ($pod.status.containerStatuses) {
            $ready = ($pod.status.containerStatuses | Where-Object { $_.ready -eq $true }).Count
        }
        
        # Determine health indicator
        $healthIcon = switch ($status) {
            "Running" { 
                if ($ready -eq $total) { "✅" } else { "🟡" }
            }
            "Pending" { "🟡" }
            "Failed" { "❌" }
            "Succeeded" { "✅" }
            default { "❓" }
        }
        
        $readyStatus = "$ready/$total"
        Write-Host "$healthIcon $name : $status ($readyStatus ready)" -ForegroundColor $(
            if ($status -eq "Running" -and $ready -eq $total) { "Green" }
            elseif ($status -eq "Running") { "Yellow" }
            elseif ($status -eq "Failed") { "Red" }
            else { "Yellow" }
        )
        
        # Show detailed container status if requested
        if ($Detailed -and $pod.status.containerStatuses) {
            foreach ($container in $pod.status.containerStatuses) {
                $containerStatus = if ($container.ready) { "Ready" } else { "Not Ready" }
                $restarts = $container.restartCount
                Write-Host "  └─ $($container.name): $containerStatus (Restarts: $restarts)" -ForegroundColor Gray
            }
        }
    }
}

# Function to check service endpoints
function Get-ServiceStatus {
    param([string]$Namespace)
    
    Write-Host "`n🌐 Service Status:" -ForegroundColor Cyan
    Write-Host "------------------" -ForegroundColor Cyan
    
    $services = kubectl get services -n $Namespace -o json | ConvertFrom-Json
    
    foreach ($service in $services.items) {
        $name = $service.metadata.name
        $type = $service.spec.type
        $clusterIP = $service.spec.clusterIP
        $ports = ($service.spec.ports | ForEach-Object { "$($_.port):$($_.targetPort)" }) -join ", "
        
        Write-Host "🔗 $name ($type)" -ForegroundColor White
        Write-Host "  └─ Cluster IP: $clusterIP" -ForegroundColor Gray
        Write-Host "  └─ Ports: $ports" -ForegroundColor Gray
        
        # Check external IP for LoadBalancer services
        if ($type -eq "LoadBalancer" -and $service.status.loadBalancer.ingress) {
            $externalIP = $service.status.loadBalancer.ingress[0].ip
            if ($externalIP) {
                Write-Host "  └─ External IP: $externalIP" -ForegroundColor Green
            }
        }
    }
}

# Function to check ingress status
function Get-IngressStatus {
    param([string]$Namespace)
    
    Write-Host "`n🚪 Ingress Status:" -ForegroundColor Cyan
    Write-Host "------------------" -ForegroundColor Cyan
    
    try {
        $ingresses = kubectl get ingress -n $Namespace -o json | ConvertFrom-Json
        
        foreach ($ingress in $ingresses.items) {
            $name = $ingress.metadata.name
            $hosts = ($ingress.spec.rules | ForEach-Object { $_.host }) -join ", "
            
            Write-Host "🌍 $name" -ForegroundColor White
            Write-Host "  └─ Hosts: $hosts" -ForegroundColor Gray
            
            if ($ingress.status.loadBalancer.ingress) {
                $ingressIP = $ingress.status.loadBalancer.ingress[0].ip
                if ($ingressIP) {
                    Write-Host "  └─ External IP: $ingressIP" -ForegroundColor Green
                } else {
                    Write-Host "  └─ External IP: Pending..." -ForegroundColor Yellow
                }
            }
        }
    } catch {
        Write-Host "No ingress resources found or ingress controller not available" -ForegroundColor Yellow
    }
}

# Function to check resource usage
function Get-ResourceUsage {
    param([string]$Namespace)
    
    Write-Host "`n📈 Resource Usage:" -ForegroundColor Cyan
    Write-Host "-------------------" -ForegroundColor Cyan
    
    try {
        # Get pod metrics if metrics-server is available
        $metrics = kubectl top pods -n $Namespace --no-headers 2>$null
        
        if ($metrics) {
            Write-Host "CPU and Memory usage:" -ForegroundColor White
            $metrics | ForEach-Object {
                Write-Host "  $($_)" -ForegroundColor Gray
            }
        } else {
            Write-Host "Metrics not available (metrics-server may not be installed)" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "Unable to retrieve resource metrics" -ForegroundColor Yellow
    }
}

# Function to get recent events
function Get-RecentEvents {
    param([string]$Namespace)
    
    Write-Host "`n📅 Recent Events:" -ForegroundColor Cyan
    Write-Host "------------------" -ForegroundColor Cyan
    
    $events = kubectl get events -n $Namespace --sort-by='.lastTimestamp' | Select-Object -Last 10
    
    if ($events) {
        $events | ForEach-Object {
            if ($_ -match "Warning|Error|Failed") {
                Write-Host $_ -ForegroundColor Red
            } elseif ($_ -match "Normal|Successful|Created|Started") {
                Write-Host $_ -ForegroundColor Green
            } else {
                Write-Host $_ -ForegroundColor Gray
            }
        }
    } else {
        Write-Host "No recent events found" -ForegroundColor Yellow
    }
}

# Function to show application logs
function Show-ApplicationLogs {
    param([string]$Namespace, [string]$Component)
    
    Write-Host "`n📋 Application Logs:" -ForegroundColor Cyan
    Write-Host "--------------------" -ForegroundColor Cyan
    
    if ($Component -eq "all") {
        $pods = kubectl get pods -n $Namespace -l app=luckxpress --no-headers | ForEach-Object { ($_ -split '\s+')[0] }
    } else {
        $pods = kubectl get pods -n $Namespace -l component=$Component --no-headers | ForEach-Object { ($_ -split '\s+')[0] }
    }
    
    foreach ($pod in $pods) {
        Write-Host "`nLogs for pod: $pod" -ForegroundColor Yellow
        Write-Host "$(('-' * 50))" -ForegroundColor Gray
        kubectl logs $pod -n $Namespace --tail=20
    }
}

# Main status check
Write-Host "Namespace: $Namespace" -ForegroundColor Yellow

# Check if namespace exists
$namespaceExists = kubectl get namespace $Namespace 2>$null
if (-not $namespaceExists) {
    Write-Host "❌ Namespace '$Namespace' does not exist!" -ForegroundColor Red
    exit 1
}

# Show basic status
Get-PodHealth $Namespace
Get-ServiceStatus $Namespace
Get-IngressStatus $Namespace

# Show detailed information if requested
if ($Detailed) {
    Get-ResourceUsage $Namespace
    Get-RecentEvents $Namespace
}

# Show logs if requested
if ($Logs) {
    Show-ApplicationLogs $Namespace $Component
}

# Overall health summary
Write-Host "`n🏥 Health Summary:" -ForegroundColor Cyan
Write-Host "------------------" -ForegroundColor Cyan

$runningPods = (kubectl get pods -n $Namespace --no-headers | Where-Object { $_ -match "Running" }).Count
$totalPods = (kubectl get pods -n $Namespace --no-headers).Count

if ($runningPods -eq $totalPods -and $totalPods -gt 0) {
    Write-Host "✅ All pods are running ($runningPods/$totalPods)" -ForegroundColor Green
    Write-Host "🎉 LuckXpress deployment is healthy!" -ForegroundColor Green
} elseif ($runningPods -gt 0) {
    Write-Host "⚠️  Some pods are not running ($runningPods/$totalPods)" -ForegroundColor Yellow
    Write-Host "🔧 Check pod status and events for issues" -ForegroundColor Yellow
} else {
    Write-Host "❌ No pods are running" -ForegroundColor Red
    Write-Host "🚨 Deployment requires immediate attention" -ForegroundColor Red
}

Write-Host "`nFor more details, use:" -ForegroundColor Cyan
Write-Host "  kubectl get all -n $Namespace" -ForegroundColor White
Write-Host "  kubectl describe deployment luckxpress-backend -n $Namespace" -ForegroundColor White
Write-Host "  kubectl logs -f deployment/luckxpress-backend -n $Namespace" -ForegroundColor White
