# LuckXpress Kubernetes Configuration

This directory contains the complete Kubernetes configuration for deploying LuckXpress in production environments.

## 📁 Directory Structure

```
k8s/
├── production/              # Production environment configuration
│   ├── namespace.yaml       # Namespace and resource quotas
│   ├── rbac.yaml           # Service accounts and RBAC
│   ├── secrets.yaml        # Application secrets (template)
│   ├── configmap.yaml      # Application configuration
│   ├── deployment.yaml     # Main application deployment + HPA
│   ├── service.yaml        # Internal and external services
│   ├── ingress.yaml        # Ingress controllers for external access
│   ├── infrastructure.yaml # PostgreSQL, Redis, Kafka StatefulSets
│   ├── network-policy.yaml # Network security policies
│   ├── pdb.yaml           # Pod Disruption Budget + monitoring alerts
│   └── kustomization.yaml # Kustomize configuration
├── scripts/                # Management and deployment scripts
│   ├── deploy.ps1         # Automated deployment script
│   ├── status.ps1         # Status monitoring script
│   └── rollback.ps1       # Rollback operations script
└── README.md              # This file
```

## 🚀 Quick Start

### Prerequisites
- Kubernetes cluster 1.25+
- `kubectl` configured for your cluster
- PowerShell 7+ for management scripts

### Deploy to Production
```powershell
# 1. Update secrets with production values
code k8s/production/secrets.yaml

# 2. Deploy infrastructure
.\k8s\scripts\deploy.ps1 -Infrastructure -All

# 3. Deploy application
.\k8s\scripts\deploy.ps1 -Application -All -Version v1.2.3

# 4. Check deployment status
.\k8s\scripts\status.ps1 -Detailed
```

## 🏗️ Architecture Components

### Application Tier
- **LuckXpress Backend**: 3 replicas with auto-scaling (3-10 pods)
- **Resource Allocation**: 500m-2000m CPU, 2Gi-4Gi Memory per pod
- **Health Checks**: Liveness and readiness probes configured
- **Security**: Non-root containers, security contexts applied

### Data Tier
- **PostgreSQL**: StatefulSet deployment with persistent storage
- **Redis**: StatefulSet for caching with persistence
- **Kafka**: 3-node cluster for message processing

### Network Tier
- **Services**: Internal ClusterIP and external LoadBalancer
- **Ingress**: HTTPS termination, rate limiting, security headers
- **Network Policies**: Microsegmentation and traffic filtering

### Security & Compliance
- **RBAC**: Least-privilege access controls
- **Secrets Management**: Kubernetes secrets (external secret manager recommended)
- **Network Policies**: Deny-all default with explicit allow rules
- **Pod Security**: Security contexts, non-root users

## 📊 Monitoring & Observability

### Metrics Collection
- **Prometheus**: Application metrics via `/actuator/prometheus`
- **Custom Metrics**: Financial transactions, KYC queue, compliance events
- **Infrastructure Metrics**: CPU, memory, disk, network usage

### Alerting Rules
- High error rates (>1%)
- High response times (>500ms)
- KYC queue backlog (>100 items)
- Suspicious activity spikes
- Resource exhaustion
- Pod crash loops

### Health Checks
- **Liveness Probe**: `/actuator/health/liveness`
- **Readiness Probe**: `/actuator/health/readiness`
- **Startup Probe**: Initial health check with longer timeout

## 🔧 Management Operations

### Deployment Commands
```powershell
# Deploy everything
.\k8s\scripts\deploy.ps1 -All -Version v1.3.0

# Deploy only infrastructure
.\k8s\scripts\deploy.ps1 -Infrastructure

# Deploy only application
.\k8s\scripts\deploy.ps1 -Application -Version v1.3.0

# Dry run deployment
.\k8s\scripts\deploy.ps1 -All -DryRun
```

### Status Monitoring
```powershell
# Quick status check
.\k8s\scripts\status.ps1

# Detailed status with metrics
.\k8s\scripts\status.ps1 -Detailed

# View application logs
.\k8s\scripts\status.ps1 -Logs

# Check specific component
.\k8s\scripts\status.ps1 -Component backend
```

### Rollback Operations
```powershell
# View deployment history
.\k8s\scripts\rollback.ps1 -History

# Rollback to previous version
.\k8s\scripts\rollback.ps1

# Rollback to specific revision
.\k8s\scripts\rollback.ps1 -Revision 5

# Dry run rollback
.\k8s\scripts\rollback.ps1 -DryRun
```

### Manual Operations
```powershell
# Scale application
kubectl scale deployment luckxpress-backend --replicas=5 -n luckxpress-prod

# Port forward for testing
kubectl port-forward deployment/luckxpress-backend 8080:8080 -n luckxpress-prod

# Execute commands in pods
kubectl exec -it luckxpress-backend-xxx -n luckxpress-prod -- bash

# View detailed pod information
kubectl describe pod luckxpress-backend-xxx -n luckxpress-prod
```

## 🔒 Security Configuration

### Network Security
- **Ingress Controller**: HTTPS only, security headers, rate limiting
- **Network Policies**: Microsegmentation with deny-all default
- **Service Mesh**: Ready for Istio integration (optional)

### Application Security
- **Non-root Containers**: All containers run as non-root user (1000)
- **Security Contexts**: Read-only filesystems where possible
- **Resource Limits**: CPU and memory limits to prevent DoS

### Secrets Management
⚠️ **Production Warning**: Replace Kubernetes secrets with external secret management:
- AWS Secrets Manager
- HashiCorp Vault
- Azure Key Vault
- Google Secret Manager

### RBAC Configuration
- **Service Accounts**: Dedicated service account per component
- **Roles**: Minimal required permissions
- **RoleBindings**: Explicit permission assignments

## 📈 Performance & Scaling

### Horizontal Pod Autoscaler (HPA)
- **CPU Target**: 70% utilization
- **Memory Target**: 80% utilization
- **Scale Range**: 3-10 replicas
- **Scale Up**: 100% increase every 60s
- **Scale Down**: 50% decrease every 300s

### Resource Optimization
- **Requests vs Limits**: Proper resource allocation
- **Pod Anti-Affinity**: Distribute pods across nodes
- **Node Affinity**: Optimize placement for performance

### Database Performance
- **Connection Pooling**: HikariCP with optimized settings
- **Persistent Storage**: SSD-backed storage classes
- **Backup Strategy**: Automated daily backups to S3

## 🚨 Disaster Recovery

### Backup Strategy
- **Database**: Daily automated backups to S3
- **Configuration**: GitOps with version control
- **Secrets**: Encrypted backup procedures

### Recovery Procedures
1. **Infrastructure Recovery**: Redeploy infrastructure components
2. **Data Recovery**: Restore database from latest backup
3. **Application Recovery**: Deploy application with known-good version
4. **Verification**: Run health checks and integration tests

## 🔍 Troubleshooting

### Common Issues
| Issue | Symptoms | Resolution |
|-------|----------|------------|
| Pod Stuck Pending | `kubectl get pods` shows Pending | Check node resources, quotas |
| Application Not Starting | Pod restarts frequently | Check logs, configuration, secrets |
| Database Connection | Connection timeouts | Verify database pod, network policies |
| High Memory Usage | OOMKilled events | Adjust resource limits, check for leaks |
| Slow Response Times | High latency metrics | Check database queries, resource usage |

### Debug Commands
```powershell
# Check cluster resources
kubectl describe nodes
kubectl get events --sort-by=.metadata.creationTimestamp

# Application debugging
kubectl logs -f deployment/luckxpress-backend -n luckxpress-prod
kubectl exec -it luckxpress-backend-xxx -n luckxpress-prod -- jstack 1

# Database debugging
kubectl exec -it postgres-0 -n luckxpress-prod -- psql -U luckxpress
kubectl logs postgres-0 -n luckxpress-prod

# Network debugging
kubectl exec -it luckxpress-backend-xxx -n luckxpress-prod -- nslookup postgres-service
kubectl describe networkpolicy -n luckxpress-prod
```

## 📚 Additional Resources

### Documentation
- **Deployment Guide**: [KUBERNETES_SETUP.md](../KUBERNETES_SETUP.md)
- **Docker Guide**: [DOCKER_SETUP.md](../DOCKER_SETUP.md)
- **Testing Guide**: [TESTING_SETUP.md](../TESTING_SETUP.md)

### External Links
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Kustomize Guide](https://kustomize.io/)
- [Spring Boot Kubernetes Guide](https://spring.io/guides/gs/spring-boot-kubernetes/)

### Support
- **DevOps Team**: devops@luckxpress.com
- **Emergency**: +1-555-LUCKXPR
- **Documentation**: https://wiki.luckxpress.com

---

## ⚡ Quick Reference

### Essential Commands
```bash
# Deploy everything
.\k8s\scripts\deploy.ps1 -All -Version latest

# Check status
.\k8s\scripts\status.ps1

# View logs
kubectl logs -f deployment/luckxpress-backend -n luckxpress-prod

# Scale up
kubectl scale deployment luckxpress-backend --replicas=5 -n luckxpress-prod

# Rollback
.\k8s\scripts\rollback.ps1
```

### Important Endpoints
- **Application**: `https://api.luckxpress.com`
- **Admin Panel**: `https://admin-api.luckxpress.com`
- **Health Check**: `https://api.luckxpress.com/actuator/health`
- **Metrics**: `https://admin-api.luckxpress.com/actuator/prometheus`

### Emergency Contacts
- **On-Call Engineer**: +1-555-LUCKXPR
- **DevOps Team**: devops@luckxpress.com
- **Security Team**: security@luckxpress.com
