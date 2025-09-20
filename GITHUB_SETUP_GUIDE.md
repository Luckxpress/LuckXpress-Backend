# GitHub Enterprise Repository Setup Guide

## 🎯 Complete Step-by-Step Instructions

This guide will walk you through setting up your LuckXpress Backend repository on GitHub Enterprise with proper branch protection and CI/CD.

## 📋 Prerequisites

- [x] Local Git repository initialized
- [x] All branches created (main, develop, feature/initial-setup)
- [x] CI/CD pipeline configured (.github/workflows/ci.yml)
- [x] CODEOWNERS file configured
- [x] Docker setup completed
- [x] All commits made and ready to push

## 🚀 STEP 1: Create GitHub Enterprise Repository

### 1.1 Navigate to your GitHub Enterprise instance
```
https://github.enterprise.com/your-org
```

### 1.2 Create New Repository
- Click "New Repository" or "+" > "New repository"
- **Repository name**: `luckxpress-backend`
- **Description**: `LuckXpress Backend - Sweepstakes gaming platform with compliance validation`
- **Visibility**: `Private`
- **Initialize repository**: `None` (DO NOT check any boxes - we already have files)
- Click "Create repository"

### 1.3 Copy the Repository URL
After creation, copy the HTTPS URL (should look like):
```
https://github.enterprise.com/your-org/luckxpress-backend.git
```

## 🔗 STEP 2: Update Remote Origin

Replace the placeholder URL with your actual repository URL:

```powershell
# Remove current origin (if needed)
git remote remove origin

# Add your actual GitHub Enterprise URL
git remote add origin https://github.enterprise.com/your-org/luckxpress-backend.git

# Verify remote is set correctly
git remote -v
```

## 📤 STEP 3: Push All Branches

Execute these commands in order:

```powershell
# Push main branch (production)
git checkout main
git push -u origin main

# Push develop branch (integration)
git checkout develop
git push -u origin develop

# Push feature branch (current work)
git checkout feature/initial-setup
git push -u origin feature/initial-setup
```

**Note**: You may need to authenticate with your GitHub Enterprise credentials.

## 🛡️ STEP 4: Configure Branch Protection Rules

### 4.1 Navigate to Repository Settings
- Go to your repository on GitHub Enterprise
- Click "Settings" tab
- Click "Branches" in the left sidebar

### 4.2 Protect Main Branch
Click "Add rule" and configure:

**Branch name pattern**: `main`

**Protection Settings**:
- ✅ Require pull request reviews before merging
  - Required approving reviews: `2`
  - ✅ Dismiss stale reviews when new commits are pushed
  - ✅ Require review from code owners
- ✅ Require status checks to pass before merging
  - ✅ Require branches to be up to date before merging
  - Status checks: Select `CI Pipeline` (after first run)
- ✅ Require conversation resolution before merging
- ✅ Include administrators (set to `No` for strict enforcement)
- ✅ Restrict pushes that create files larger than 100MB
- ❌ Allow force pushes
- ❌ Allow deletions

### 4.3 Protect Develop Branch
Click "Add rule" and configure:

**Branch name pattern**: `develop`

**Protection Settings**:
- ✅ Require pull request reviews before merging
  - Required approving reviews: `1`
  - ✅ Dismiss stale reviews when new commits are pushed
  - ✅ Require review from code owners
- ✅ Require status checks to pass before merging
  - ✅ Require branches to be up to date before merging
  - Status checks: Select `CI Pipeline` (after first run)
- ✅ Require conversation resolution before merging
- ❌ Allow force pushes
- ❌ Allow deletions

## ⚡ STEP 5: Verify GitHub Actions

### 5.1 Check Actions Tab
- Navigate to the "Actions" tab in your repository
- You should see workflow runs triggered by the pushes
- The CI Pipeline should start automatically

### 5.2 Expected Initial Behavior
- ✅ **Checkout code**: Should pass
- ✅ **Set up JDK 21**: Should pass
- ✅ **Cache Maven dependencies**: Should pass
- ❌ **Run tests**: May fail due to compilation issues (expected)
- ❌ **Security scan**: May fail due to compilation issues (expected)

**This is normal!** The infrastructure is working, compilation issues will be fixed in Option 2.

### 5.3 Configure Repository Secrets (if needed)
If your repository needs additional secrets:
- Go to Settings > Secrets and variables > Actions
- Add any required secrets for your CI/CD pipeline

## 🎯 STEP 6: Create Your First Pull Request

### 6.1 Create PR from feature branch
- Go to "Pull requests" tab
- Click "New pull request"
- Base: `develop` ← Compare: `feature/initial-setup`
- Title: `Initial project setup with CI/CD pipeline`
- Description: Use the PR template that was created

### 6.2 Verify PR Workflow
This will test:
- ✅ Branch protection rules
- ✅ Required status checks
- ✅ Code owner reviews
- ✅ CI/CD pipeline integration

## 📊 STEP 7: Repository Settings Verification

### 7.1 General Settings
- ✅ Issues enabled
- ✅ Pull requests enabled
- ✅ Discussions (optional)
- ✅ Actions enabled

### 7.2 Security Settings
- Navigate to Settings > Security
- ✅ Enable vulnerability alerts
- ✅ Enable security updates
- ✅ Enable secret scanning (if available)

## 🔧 Troubleshooting

### Authentication Issues
```powershell
# If you get 403 errors, configure Git credentials
git config credential.helper manager-core
```

### SSH Certificate Required
If you see "SSH certificate required":
1. Contact your GitHub Enterprise administrator
2. Request SSH certificate for repository access
3. Or use Personal Access Token for HTTPS authentication

### CI/CD Pipeline Failures
Expected initially due to compilation issues. After Option 2 (compilation fixes), the pipeline should pass.

## ✅ Success Criteria

After completing these steps, you should have:

- ✅ Private GitHub Enterprise repository created
- ✅ All three branches pushed successfully
- ✅ Branch protection rules configured
- ✅ GitHub Actions running (may have compilation failures initially)
- ✅ CODEOWNERS governance active
- ✅ Professional repository structure with documentation

## 🎯 Next Steps

Once GitHub setup is complete, proceed to **Option 2: Fix Compilation Issues** to resolve the remaining Lombok and dependency issues.

---

**📞 Support**: If you encounter issues, check with your GitHub Enterprise administrator for organization-specific authentication requirements.
