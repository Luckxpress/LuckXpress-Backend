# LuckXpress Kubernetes Production Setup Guide

This guide provides comprehensive instructions for deploying and managing the LuckXpress platform on Kubernetes in production.

## 📋 Prerequisites

### Infrastructure Requirements
- **Kubernetes cluster** 1.25+ with at least 3 worker nodes
- **CPU**: Minimum 8 cores per node (24 cores total)
- **Memory**: Minimum 16GB RAM per node (48GB total)
- **Storage**: 500GB+ SSD storage with dynamic provisioning
- **Network**: LoadBalancer support (AWS ALB, GCP LB, etc.)

### Required Add-ons
- **Ingress Controller** (nginx-ingress recommended)
- **Cert-Manager** for SSL certificate management
- **Metrics Server** for resource monitoring
- **Container Registry** access (GitHub Container Registry)

### Tools Required
- `kubectl` CLI tool configured for your cluster
- `helm` (optional, for add-on installations)
- PowerShell 7+ for deployment scripts
- Docker (for building images)

## 🏗️ Architecture Overview

### Production Topology
```
┌─────────────────────────────────────────────────────────────┐
│                    Load Balancer (ALB/NLB)                 │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                 Ingress Controller                          │
│           (nginx-ingress with SSL termination)             │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                 Namespace: luckxpress-prod                  │
│                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │ LuckXpress  │  │ LuckXpress  │  │ LuckXpress  │        │
│  │   Pod 1     │  │   Pod 2     │  │   Pod 3     │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
│                                                             │
│  ┌──────────┐   ┌──────────┐   ┌─────────────┐            │
│  │PostgreSQL│   │  Redis   │   │   Kafka     │            │
│  │StatefulSet│   │StatefulSet│   │StatefulSet │            │
│  └──────────┘   └──────────┘   └─────────────┘            │
└─────────────────────────────────────────────────────────────┘
```

### Resource Allocation
| Component | CPU Request | CPU Limit | Memory Request | Memory Limit | Replicas |
|-----------|-------------|-----------|----------------|--------------|----------|
| LuckXpress App | 500m | 2000m | 2Gi | 4Gi | 3 |
| PostgreSQL | 500m | 1000m | 1Gi | 2Gi | 1 |
| Redis | 250m | 500m | 512Mi | 1Gi | 1 |
| Kafka | 500m | 1000m | 1Gi | 2Gi | 3 |

## 🚀 Quick Deployment

### 1. Prepare Environment
```powershell
# Clone the repository
git clone https://github.com/luckxpress/luckxpress-backend.git
cd luckxpress-backend

# Set kubectl context
kubectl config use-context your-production-cluster

# Verify cluster access
kubectl cluster-info
```

### 2. Configure Secrets
⚠️ **CRITICAL**: Update all secrets before deployment!

```powershell
# Edit secrets file (use external secret management in production)
code k8s/production/secrets.yaml

# Required secrets to update:
# - Database passwords
# - JWT secrets
# - API keys (Stripe, Sentry, etc.)
# - SSL certificates
```

### 3. Deploy Infrastructure
```powershell
# Deploy infrastructure components first
.\k8s\scripts\deploy.ps1 -Infrastructure -All

# Wait for infrastructure to be ready
kubectl wait --for=condition=ready pod -l app=postgres -n luckxpress-prod --timeout=300s
kubectl wait --for=condition=ready pod -l app=redis -n luckxpress-prod --timeout=300s
```

### 4. Deploy Application
```powershell
# Deploy application with specific version
.\k8s\scripts\deploy.ps1 -Application -All -Version v1.2.3

# Monitor deployment
kubectl rollout status deployment/luckxpress-backend -n luckxpress-prod
```

### 5. Verify Deployment
```powershell
# Check deployment status
.\k8s\scripts\status.ps1 -Detailed

# Test application health
kubectl port-forward deployment/luckxpress-backend 8080:8080 -n luckxpress-prod
curl http://localhost:8080/actuator/health
```

## 📁 Configuration Files

### Core Kubernetes Resources
```
k8s/production/
├── namespace.yaml          # Namespace and resource quotas
├── rbac.yaml              # Service accounts and RBAC
├── secrets.yaml           # Application secrets (template)
├── configmap.yaml         # Application configuration
├── deployment.yaml        # Main application deployment
├── service.yaml           # Service definitions
├── ingress.yaml           # External access configuration
├── infrastructure.yaml    # PostgreSQL, Redis, Kafka
├── network-policy.yaml    # Network security policies
├── pdb.yaml              # Pod Disruption Budget
└── kustomization.yaml    # Kustomization configuration
```

### Management Scripts
```
k8s/scripts/
├── deploy.ps1            # Deployment automation
├── status.ps1            # Status monitoring
└── rollback.ps1          # Rollback operations
```

## 🔧 Configuration Management

### Environment-Specific Settings
The application uses a layered configuration approach:

1. **Base Configuration**: `application.yml` (in JAR)
2. **Kubernetes Configuration**: `application-k8s.yml` (ConfigMap)
3. **Environment Variables**: Secrets and environment-specific overrides

### Key Configuration Areas

#### Database Configuration
```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 30000
      max-lifetime: 600000
```

#### Security Configuration
```yaml
luckxpress:
  security:
    jwt:
      expiration: 86400  # 24 hours
    cors:
      allowed-origins:
        - "https://luckxpress.com"
```

#### Compliance Configuration
```yaml
luckxpress:
  compliance:
    state-restrictions:
      wa:
        sweeps-allowed: false
        max-deposit: 1000.00
      id:
        sweeps-allowed: false
        max-deposit: 1000.00
```

## 🛠️ Management Operations

### Deployment Management

#### Deploy New Version
```powershell
# Build and push new image
docker build -t ghcr.io/luckxpress/backend:v1.3.0 .
docker push ghcr.io/luckxpress/backend:v1.3.0

# Deploy new version
.\k8s\scripts\deploy.ps1 -Application -Version v1.3.0
```

#### Rolling Updates
```powershell
# Update deployment image
kubectl set image deployment/luckxpress-backend luckxpress=ghcr.io/luckxpress/backend:v1.3.0 -n luckxpress-prod

# Monitor rollout
kubectl rollout status deployment/luckxpress-backend -n luckxpress-prod
```

#### Rollback Operations
```powershell
# View rollout history
.\k8s\scripts\rollback.ps1 -History

# Rollback to previous version
.\k8s\scripts\rollback.ps1

# Rollback to specific revision
.\k8s\scripts\rollback.ps1 -Revision 3
```

### Scaling Operations

#### Manual Scaling
```powershell
# Scale application pods
kubectl scale deployment luckxpress-backend --replicas=5 -n luckxpress-prod

# Scale infrastructure
kubectl scale statefulset postgres --replicas=2 -n luckxpress-prod  # Only if configured for HA
```

#### Auto-scaling
The HPA (Horizontal Pod Autoscaler) automatically scales based on:
- CPU utilization (target: 70%)
- Memory utilization (target: 80%)
- Custom metrics (if configured)

### Monitoring and Troubleshooting

#### Check Application Status
```powershell
# Comprehensive status check
.\k8s\scripts\status.ps1 -Detailed

# View application logs
kubectl logs -f deployment/luckxpress-backend -n luckxpress-prod

# Get pod details
kubectl describe pod <pod-name> -n luckxpress-prod
```

#### Performance Monitoring
```powershell
# View resource usage
kubectl top pods -n luckxpress-prod
kubectl top nodes

# Check metrics
kubectl get hpa -n luckxpress-prod
```

#### Database Operations
```powershell
# Connect to PostgreSQL
kubectl exec -it postgres-0 -n luckxpress-prod -- psql -U luckxpress

# Create database backup
kubectl exec postgres-0 -n luckxpress-prod -- pg_dump -U luckxpress luckxpress > backup.sql

# Monitor database performance
kubectl exec postgres-0 -n luckxpress-prod -- psql -U luckxpress -c "SELECT * FROM pg_stat_activity;"
```

## 🔒 Security Configuration

### Network Security
- **Network Policies**: Restrict inter-pod communication
- **Ingress Security**: SSL termination, rate limiting, IP whitelisting
- **Service Mesh**: Consider Istio for advanced security (optional)

### Pod Security
```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  fsGroup: 1000
  seccompProfile:
    type: RuntimeDefault
```

### Secret Management
⚠️ **IMPORTANT**: In production, use external secret management:

#### AWS Secrets Manager Integration
```powershell
# Install External Secrets Operator
helm repo add external-secrets https://charts.external-secrets.io
helm install external-secrets external-secrets/external-secrets -n external-secrets-system --create-namespace
```

#### Example SecretStore Configuration
```yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: aws-secrets-manager
  namespace: luckxpress-prod
spec:
  provider:
    aws:
      service: SecretsManager
      region: us-west-2
```

## 📊 Monitoring and Alerting

### Prometheus Metrics
The application exposes metrics at `/actuator/prometheus`:

#### Key Metrics to Monitor
- `http_server_requests_seconds` - Request latency
- `jvm_memory_used_bytes` - Memory usage
- `luckxpress_kyc_queue_size` - Compliance queue
- `luckxpress_transaction_amount_total` - Financial metrics

### Grafana Dashboards
Pre-configured dashboards available:
- Application Overview
- JVM Metrics
- Database Performance
- Compliance Monitoring

### Alert Rules
Critical alerts configured for:
- High error rates (>1%)
- High response times (>500ms)
- KYC queue backlog (>100 items)
- Suspicious activity spikes
- Pod crashes and restarts

## 🚨 Disaster Recovery

### Backup Strategy

#### Database Backups
```powershell
# Automated backup CronJob
kubectl apply -f - <<EOF
apiVersion: batch/v1
kind: CronJob
metadata:
  name: postgres-backup
  namespace: luckxpress-prod
spec:
  schedule: "0 2 * * *"  # Daily at 2 AM
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: postgres-backup
            image: postgres:15-alpine
            command:
            - /bin/bash
            - -c
            - |
              pg_dump -h postgres-service -U luckxpress luckxpress | gzip | aws s3 cp - s3://luckxpress-backups/$(date +%Y-%m-%d-%H-%M-%S).sql.gz
            env:
            - name: PGPASSWORD
              valueFrom:
                secretKeyRef:
                  name: luckxpress-db-secret
                  key: password
          restartPolicy: OnFailure
EOF
```

#### Application State Backup
```powershell
# Backup Kubernetes configurations
kubectl get all -n luckxpress-prod -o yaml > luckxpress-backup-$(Get-Date -Format "yyyy-MM-dd").yaml

# Backup secrets (encrypted)
kubectl get secrets -n luckxpress-prod -o yaml > secrets-backup-$(Get-Date -Format "yyyy-MM-dd").yaml
```

### Recovery Procedures

#### Complete Cluster Recovery
```powershell
# 1. Restore infrastructure
.\k8s\scripts\deploy.ps1 -Infrastructure -All

# 2. Restore database from backup
kubectl exec postgres-0 -n luckxpress-prod -- psql -U luckxpress -c "DROP DATABASE IF EXISTS luckxpress;"
kubectl exec postgres-0 -n luckxpress-prod -- psql -U luckxpress -c "CREATE DATABASE luckxpress;"
kubectl exec -i postgres-0 -n luckxpress-prod -- psql -U luckxpress luckxpress < backup.sql

# 3. Deploy application
.\k8s\scripts\deploy.ps1 -Application -All
```

## 🔄 CI/CD Integration

### GitHub Actions Workflow
```yaml
name: Deploy to Production
on:
  push:
    tags:
      - 'v*'

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Configure kubectl
      uses: azure/k8s-set-context@v1
      with:
        method: kubeconfig
        kubeconfig: ${{ secrets.KUBE_CONFIG }}
    
    - name: Deploy to Kubernetes
      run: |
        ./k8s/scripts/deploy.ps1 -Application -Version ${{ github.ref_name }}
```

### Deployment Gates
- **Health Checks**: Application must pass health checks
- **Integration Tests**: API tests must pass
- **Security Scans**: Container security validation
- **Performance Tests**: Load testing validation

## 📈 Performance Tuning

### JVM Optimization
```yaml
env:
- name: JAVA_OPTS
  value: >-
    -XX:MaxRAMPercentage=75
    -XX:InitialRAMPercentage=50
    -XX:+UseG1GC
    -XX:MaxGCPauseMillis=200
    -XX:+UseStringDeduplication
    -XX:+OptimizeStringConcat
```

### Database Optimization
```yaml
# PostgreSQL configuration
env:
- name: POSTGRES_SHARED_BUFFERS
  value: "256MB"
- name: POSTGRES_EFFECTIVE_CACHE_SIZE
  value: "1GB"
- name: POSTGRES_WORK_MEM
  value: "4MB"
```

### Resource Optimization
- **Node Affinity**: Spread pods across nodes
- **Pod Anti-Affinity**: Avoid single points of failure
- **Resource Requests**: Ensure proper scheduling
- **Resource Limits**: Prevent resource starvation

## 🆘 Troubleshooting Guide

### Common Issues

#### Pod Stuck in Pending
```powershell
# Check node resources
kubectl describe nodes

# Check pod events
kubectl describe pod <pod-name> -n luckxpress-prod

# Check resource quotas
kubectl describe quota -n luckxpress-prod
```

#### Application Not Starting
```powershell
# Check application logs
kubectl logs -f deployment/luckxpress-backend -n luckxpress-prod

# Check configuration
kubectl get configmap luckxpress-config -n luckxpress-prod -o yaml

# Check secrets
kubectl get secret luckxpress-db-secret -n luckxpress-prod -o yaml
```

#### Database Connection Issues
```powershell
# Test database connectivity
kubectl exec -it luckxpress-backend-xxx -n luckxpress-prod -- nc -zv postgres-service 5432

# Check database logs
kubectl logs postgres-0 -n luckxpress-prod

# Verify database credentials
kubectl exec postgres-0 -n luckxpress-prod -- psql -U luckxpress -c "SELECT version();"
```

### Performance Issues
```powershell
# Check resource usage
kubectl top pods -n luckxpress-prod
kubectl top nodes

# Check HPA status
kubectl get hpa -n luckxpress-prod

# View slow queries
kubectl exec postgres-0 -n luckxpress-prod -- psql -U luckxpress -c "SELECT query, mean_time, calls FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;"
```

## 📚 Additional Resources

### Documentation Links
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Spring Boot on Kubernetes](https://spring.io/guides/gs/spring-boot-kubernetes/)
- [PostgreSQL on Kubernetes](https://postgres-operator.readthedocs.io/)
- [Kafka on Kubernetes](https://strimzi.io/)

### Best Practices
- [12-Factor App Methodology](https://12factor.net/)
- [Container Security Best Practices](https://kubernetes.io/docs/concepts/security/)
- [Production Readiness Checklist](https://kubernetes.io/docs/setup/best-practices/)

### Support Contacts
- **DevOps Team**: devops@luckxpress.com
- **Security Team**: security@luckxpress.com
- **On-Call**: +1-555-LUCKXPR (555-582-5977)

---

**⚠️ Production Checklist**
- [ ] All secrets updated with production values
- [ ] SSL certificates configured
- [ ] Monitoring and alerting active
- [ ] Backup procedures tested
- [ ] Disaster recovery plan documented
- [ ] Security review completed
- [ ] Performance testing passed
- [ ] Compliance requirements verified

**For emergency support**: Contact the on-call engineer immediately if you encounter critical issues in production.
