# LuckXpress API Testing with Postman

This directory contains comprehensive Postman collections and environments for testing the LuckXpress Sweepstakes Casino API.

## 📁 Directory Structure

```
postman/
├── LuckXpress-API-Collection.json    # Main API collection
├── environments/
│   ├── local.json                    # Local development environment
│   ├── staging.json                  # Staging environment
│   └── production.json               # Production environment
├── test-scripts/
│   ├── compliance-tests.js           # Compliance validation tests
│   └── security-tests.js             # Security validation tests
└── README.md                         # This file
```

## 🚀 Quick Start

### 1. Import Collection
1. Open Postman
2. Click **Import** button
3. Select `LuckXpress-API-Collection.json`
4. The collection will be imported with all requests and tests

### 2. Import Environments
1. Go to **Environments** tab in Postman
2. Click **Import**
3. Select environment files from `environments/` folder
4. Choose appropriate environment (local/staging/production)

### 3. Configure Variables
Update environment variables:
- `BASE_URL`: API server URL
- `CLIENT_ID`: OAuth client ID
- `CLIENT_SECRET`: OAuth client secret (keep secure!)
- `SENTRY_DSN`: Sentry monitoring URL

## 🧪 Test Categories

### Authentication Tests
- **Player Login**: Tests user authentication flow
- **Token Refresh**: Validates JWT token refresh
- **OAuth2 Flow**: Tests OAuth2 client credentials

### Wallet Operations
- **Deposit Gold Coins**: Tests payment processing
- **Withdraw Sweeps**: Tests payout functionality
- **Balance Inquiry**: Tests wallet balance retrieval
- **Transaction History**: Tests transaction listing

### Compliance Tests (`compliance-tests.js`)
- **State Restrictions**: Validates Washington/Idaho restrictions
- **KYC Requirements**: Tests Know Your Customer validation
- **Dual Approval**: Tests high-value transaction approvals
- **Age Verification**: Tests 21+ age requirements

### Security Tests (`security-tests.js`)
- **JWT Validation**: Tests token security
- **Rate Limiting**: Validates API rate limits
- **SQL Injection**: Tests injection attack prevention
- **XSS Protection**: Tests cross-site scripting prevention
- **CSRF Protection**: Tests cross-site request forgery prevention
- **Data Exposure**: Validates sensitive data protection

## 🔧 Usage Instructions

### Running Individual Tests
1. Select a request from the collection
2. Ensure correct environment is selected
3. Click **Send**
4. Check **Test Results** tab for validation results

### Running Collection Tests
1. Click **Runner** in Postman
2. Select **LuckXpress API Collection**
3. Choose environment
4. Click **Run LuckXpress API Collection**
5. Review test results and compliance status

### Custom Test Scripts
Add custom tests to collection or folder level:

```javascript
// Example: Test response time
pm.test("Response time is less than 200ms", function () {
    pm.expect(pm.response.responseTime).to.be.below(200);
});

// Example: Test compliance header
pm.test("Compliance tracking header present", function () {
    pm.expect(pm.response.headers.get("X-Compliance-Verified")).to.exist;
});
```

## 📊 Environment Configuration

### Local Development
```json
{
  "BASE_URL": "http://localhost:8080",
  "ENVIRONMENT": "local",
  "CLIENT_ID": "luckxpress-client",
  "CLIENT_SECRET": "secret"
}
```

### Staging
```json
{
  "BASE_URL": "https://api-staging.luckxpress.com",
  "ENVIRONMENT": "staging",
  "CLIENT_ID": "luckxpress-staging-client"
}
```

### Production
```json
{
  "BASE_URL": "https://api.luckxpress.com",
  "ENVIRONMENT": "production",
  "CLIENT_ID": "luckxpress-prod-client"
}
```

## 🛂 Compliance Features

### Automatic State Validation
Tests automatically validate:
- Washington state users cannot access Sweeps
- Idaho state restrictions
- Proper error responses for restricted states

### KYC Validation
Tests verify:
- Withdrawal requires verified KYC status
- Deposit limits for non-KYC users
- Proper KYC status responses

### Financial Controls
Tests validate:
- Dual approval for transactions >$500
- Transaction limits per user tier
- Proper audit trail generation

## 🔒 Security Features

### Authentication Security
- JWT token validation
- Token expiration handling
- Refresh token security
- OAuth2 flow validation

### Input Validation
- SQL injection prevention
- XSS attack prevention
- Parameter tampering protection
- Rate limiting enforcement

### Data Protection
- Sensitive data masking in responses
- PII data handling validation
- Secure header verification
- CSRF protection validation

## 📈 Monitoring Integration

### Sentry Integration
- Automatic error tracking for failed tests
- Performance monitoring
- Real-time alert generation
- Compliance violation tracking

### Test Reporting
- Automated test result compilation
- Compliance status reporting
- Security validation reports
- Performance metrics tracking

## 🚨 Troubleshooting

### Common Issues

**Authentication Failures**
- Verify CLIENT_ID and CLIENT_SECRET
- Check token expiration
- Validate environment URL

**Rate Limiting**
- Implement delays between requests
- Use collection runner with delays
- Check API rate limit headers

**Compliance Test Failures**
- Verify user state configuration
- Check KYC status in test data
- Validate transaction amounts

### Debug Tips
1. Enable Postman Console for detailed logs
2. Check environment variable values
3. Verify request headers and body
4. Review API response status and headers
5. Check Sentry for server-side errors

## 📝 Best Practices

1. **Always run compliance tests** before deploying
2. **Use environment variables** for sensitive data
3. **Test with real-world data scenarios**
4. **Monitor test execution results** regularly
5. **Keep test scripts updated** with API changes
6. **Document any custom test modifications**
7. **Run security tests** on all environments

## 🔄 Continuous Integration

For CI/CD integration, use Newman (Postman CLI):

```bash
# Install Newman
npm install -g newman

# Run collection
newman run LuckXpress-API-Collection.json \
  -e environments/staging.json \
  --reporters cli,json \
  --reporter-json-export results.json
```

This comprehensive testing suite ensures your LuckXpress API meets all compliance, security, and functional requirements!
