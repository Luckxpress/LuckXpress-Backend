# LuckXpress Docker Health Check Script
# This script checks the health of all Docker services

Write-Host "LuckXpress Docker Health Check" -ForegroundColor Green
Write-Host "==============================" -ForegroundColor Green

# Function to check service health
function Check-ServiceHealth {
    param([string]$ServiceName)
    
    $container = docker-compose ps -q $ServiceName
    if (-not $container) {
        Write-Host "❌ $ServiceName : Container not found" -ForegroundColor Red
        return $false
    }
    
    $health = docker inspect --format='{{.State.Health.Status}}' $container 2>$null
    $status = docker inspect --format='{{.State.Status}}' $container 2>$null
    
    if ($status -eq "running") {
        if ($health -eq "healthy" -or $health -eq "") {
            Write-Host "✅ $ServiceName : Healthy" -ForegroundColor Green
            return $true
        } elseif ($health -eq "starting") {
            Write-Host "🟡 $ServiceName : Starting" -ForegroundColor Yellow
            return $false
        } else {
            Write-Host "❌ $ServiceName : Unhealthy ($health)" -ForegroundColor Red
            return $false
        }
    } else {
        Write-Host "❌ $ServiceName : Not running ($status)" -ForegroundColor Red
        return $false
    }
}

# Function to check port accessibility
function Check-Port {
    param([string]$Host, [int]$Port, [string]$ServiceName)
    
    try {
        $connection = Test-NetConnection -ComputerName $Host -Port $Port -WarningAction SilentlyContinue
        if ($connection.TcpTestSucceeded) {
            Write-Host "✅ $ServiceName Port $Port : Accessible" -ForegroundColor Green
            return $true
        } else {
            Write-Host "❌ $ServiceName Port $Port : Not accessible" -ForegroundColor Red
            return $false
        }
    } catch {
        Write-Host "❌ $ServiceName Port $Port : Error checking" -ForegroundColor Red
        return $false
    }
}

# Function to check HTTP endpoint
function Check-HttpEndpoint {
    param([string]$Url, [string]$ServiceName)
    
    try {
        $response = Invoke-WebRequest -Uri $Url -TimeoutSec 10 -UseBasicParsing
        if ($response.StatusCode -eq 200) {
            Write-Host "✅ $ServiceName HTTP : Responding (Status: $($response.StatusCode))" -ForegroundColor Green
            return $true
        } else {
            Write-Host "❌ $ServiceName HTTP : Bad response (Status: $($response.StatusCode))" -ForegroundColor Red
            return $false
        }
    } catch {
        Write-Host "❌ $ServiceName HTTP : Not responding" -ForegroundColor Red
        return $false
    }
}

# Check Docker Compose services
Write-Host "`nChecking Docker Services:" -ForegroundColor Cyan
Write-Host "-------------------------"

$services = @("postgres", "redis", "kafka", "zookeeper", "app", "prometheus", "grafana")
$healthyServices = 0

foreach ($service in $services) {
    if (Check-ServiceHealth $service) {
        $healthyServices++
    }
}

# Check port accessibility
Write-Host "`nChecking Port Accessibility:" -ForegroundColor Cyan
Write-Host "----------------------------"

$ports = @(
    @{Host="localhost"; Port=5432; Service="PostgreSQL"},
    @{Host="localhost"; Port=6379; Service="Redis"},
    @{Host="localhost"; Port=9092; Service="Kafka"},
    @{Host="localhost"; Port=8080; Service="LuckXpress App"},
    @{Host="localhost"; Port=8081; Service="LuckXpress Management"},
    @{Host="localhost"; Port=9090; Service="Prometheus"},
    @{Host="localhost"; Port=3000; Service="Grafana"}
)

$accessiblePorts = 0
foreach ($port in $ports) {
    if (Check-Port $port.Host $port.Port $port.Service) {
        $accessiblePorts++
    }
}

# Check HTTP endpoints
Write-Host "`nChecking HTTP Endpoints:" -ForegroundColor Cyan
Write-Host "------------------------"

$endpoints = @(
    @{Url="http://localhost:8081/actuator/health"; Service="LuckXpress Health"},
    @{Url="http://localhost:8081/actuator/info"; Service="LuckXpress Info"},
    @{Url="http://localhost:9090/-/healthy"; Service="Prometheus Health"},
    @{Url="http://localhost:3000/api/health"; Service="Grafana Health"}
)

$healthyEndpoints = 0
foreach ($endpoint in $endpoints) {
    if (Check-HttpEndpoint $endpoint.Url $endpoint.Service) {
        $healthyEndpoints++
    }
}

# Summary
Write-Host "`nHealth Check Summary:" -ForegroundColor Cyan
Write-Host "====================="
Write-Host "Services: $healthyServices/$($services.Count) healthy" -ForegroundColor $(if($healthyServices -eq $services.Count) {"Green"} else {"Red"})
Write-Host "Ports: $accessiblePorts/$($ports.Count) accessible" -ForegroundColor $(if($accessiblePorts -eq $ports.Count) {"Green"} else {"Red"})
Write-Host "Endpoints: $healthyEndpoints/$($endpoints.Count) responding" -ForegroundColor $(if($healthyEndpoints -eq $endpoints.Count) {"Green"} else {"Red"})

$overallHealth = ($healthyServices -eq $services.Count) -and ($accessiblePorts -eq $ports.Count) -and ($healthyEndpoints -gt 0)

if ($overallHealth) {
    Write-Host "`n🎉 Overall Status: HEALTHY" -ForegroundColor Green
    exit 0
} else {
    Write-Host "`n⚠️  Overall Status: NEEDS ATTENTION" -ForegroundColor Red
    Write-Host "Check the failed services above for more details." -ForegroundColor Yellow
    exit 1
}
