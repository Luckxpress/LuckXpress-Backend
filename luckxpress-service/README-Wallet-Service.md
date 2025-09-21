# LuckXpress Wallet Service

Comprehensive wallet management system for LuckXpress Sweepstakes Casino.

## 🏗️ Architecture Overview

The wallet service implements a secure, compliant, and scalable financial management system with dual-currency support (Gold Coins and Sweeps Coins).

### 🔑 Key Components

#### Core Services
- **`WalletService`** - Main wallet operations (credit, debit, balance management)
- **`WithdrawalService`** - Sweeps withdrawal processing with KYC validation
- **`WalletOperationsService`** - Advanced operations (fund locking, game wins)
- **`LedgerService`** - Immutable financial audit trail
- **`ComplianceService`** - Regulatory compliance validation

#### Data Models
- **`WalletTransaction`** - Transaction result with full audit trail
- **`WalletBalance`** - Balance information with available/locked funds
- **`WithdrawalResult`** - Withdrawal processing result
- **`ComplianceResult`** - Compliance validation result

#### Infrastructure
- **`WalletConfig`** - Redis/Redisson configuration for distributed locking
- **Repositories** - Optimized data access with proper locking

## 🛡️ Security Features

### Distributed Locking
```java
// All wallet operations use distributed Redis locks
RLock lock = redissonClient.getLock("wallet:lock:" + userId);
lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
```

### Transaction Isolation
- **SERIALIZABLE** isolation level for all financial transactions
- Optimistic locking with version control on wallet entities
- Idempotent operations with TTL-based deduplication

### Audit Trail
- Every financial operation recorded in immutable ledger
- Sentry integration for real-time monitoring
- Comprehensive compliance logging

## 💰 Currency System

### Gold Coins
- **Source**: Purchased with real money
- **Usage**: Entertainment gameplay only
- **Withdrawal**: **NEVER** withdrawable
- **Compliance**: No special restrictions

### Sweeps Coins
- **Source**: Promotional bonuses, AMOE requests
- **Usage**: Prize-eligible gameplay
- **Withdrawal**: Available after KYC verification
- **Compliance**: State restrictions apply (WA/ID prohibited)

## 🎮 Transaction Flow

### 1. Deposit (Gold Purchase)
```java
WalletTransaction result = walletService.creditGold(
    userId, amount, paymentReference, idempotencyKey
);
```

### 2. Promotional Credit (Sweeps)
```java
WalletTransaction result = walletService.creditSweeps(
    userId, amount, promotionReference, reason
);
```

### 3. Game Bet
```java
// Lock funds for gameplay
walletOperationsService.lockFunds(userId, currency, betAmount, gameId);

// Debit for bet
WalletTransaction bet = walletService.debit(
    userId, currency, betAmount, gameId, "Game bet"
);
```

### 4. Game Win
```java
WalletTransaction win = walletOperationsService.processWin(
    userId, currency, betAmount, winAmount, gameId
);
```

### 5. Withdrawal (Sweeps Only)
```java
WithdrawalResult result = withdrawalService.processSweepsWithdrawal(
    userId, amount, paymentMethod, bankAccountId
);
```

## 📊 Compliance Features

### KYC Validation
- Required for withdrawals ≥ $50
- Enhanced KYC for amounts ≥ $2,000
- W2G reporting for winnings ≥ $600

### Transaction Limits
```java
// Daily limits
DAILY_DEPOSIT_LIMIT = $10,000
DAILY_WITHDRAWAL_LIMIT = $5,000

// Per-transaction limits  
MIN_DEPOSIT_AMOUNT = $5
MAX_WITHDRAWAL_AMOUNT = $5,000
```

### Dual Approval
- Transactions ≥ $500 require dual approval
- Transactions ≥ $10,000 require triple approval
- Automatic compliance violation detection

### State Restrictions
- Washington State: Sweeps operations prohibited
- Idaho State: Sweeps operations prohibited
- Automatic validation on all Sweeps transactions

## 🔍 Monitoring & Observability

### Metrics (Micrometer)
- `wallet.credit.gold` - Gold credit operation timing
- `wallet.credit.sweeps` - Sweeps credit operation timing
- `wallet.debit` - Debit operation timing
- `withdrawal.process` - Withdrawal processing timing

### Error Tracking (Sentry)
- Real-time error monitoring
- Compliance violation alerts
- Performance degradation detection
- Financial discrepancy alerts

### Audit Logging
- All operations tagged with `@RequiresAudit`
- Immutable ledger entries with full context
- Correlation ID tracking across services

## 🚀 Performance Features

### Caching
```java
@Cacheable(value = "wallets", key = "#userId")
public WalletBalance getBalance(String userId)
```

### Connection Pooling
- Redis connection pool (64 connections)
- Database connection optimization
- Efficient batch operations

### Distributed Architecture
- Horizontally scalable design
- Stateless service operations
- Redis-based coordination

## 🧪 Testing Strategy

### Unit Tests
- Service-level transaction testing
- Compliance validation testing
- Error condition handling

### Integration Tests
- End-to-end transaction flows
- Distributed locking behavior
- Database consistency validation

### Load Testing
- Concurrent transaction handling
- Lock contention scenarios
- High-volume compliance processing

## 🔧 Configuration

### Redis Configuration
```properties
# Redis settings for distributed locking
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=

# Redisson settings
redisson.connection-pool-size=64
redisson.idle-connection-timeout=10000
```

### Compliance Settings
```properties
# Compliance thresholds (defined in ComplianceConstants)
compliance.kyc.withdrawal.threshold=50.00
compliance.dual.approval.threshold=500.00
compliance.w2g.reporting.threshold=600.00
```

## 🚨 Critical Operations

### Emergency Procedures

#### Wallet Reconciliation
```java
// Check ledger integrity
boolean isValid = ledgerEntry.validateIntegrity();
```

#### Fund Recovery
```java
// Unlock stuck funds
walletOperationsService.unlockFunds(userId, currency, amount, "RECOVERY");
```

#### Compliance Violation Response
```java
ComplianceResult result = complianceService.validateDailyWithdrawalLimit(userId, amount);
if (result.hasViolation()) {
    // Automatic transaction blocking
    // Compliance team notification
    // Regulatory reporting trigger
}
```

## 📋 Operational Checklist

### Daily Operations
- [ ] Monitor wallet balance reconciliation
- [ ] Review compliance violation reports
- [ ] Check withdrawal processing status
- [ ] Validate ledger integrity

### Weekly Operations  
- [ ] Audit high-value transactions
- [ ] Review dual approval queues
- [ ] Analyze suspicious activity patterns
- [ ] Compliance threshold validation

### Monthly Operations
- [ ] W2G form generation and filing
- [ ] AML compliance reporting
- [ ] Performance metrics review
- [ ] Disaster recovery testing

## 🔗 Dependencies

### Required Services
- **Redis/Redisson** - Distributed locking and caching
- **PostgreSQL** - Primary data storage
- **Sentry** - Error monitoring and alerting
- **Micrometer** - Metrics collection

### Integration Points
- **Payment Processors** - For Gold coin purchases
- **KYC Providers** - For identity verification
- **Banking APIs** - For Sweeps withdrawals
- **Game Engine** - For bet/win processing

This wallet service provides enterprise-grade financial management with full regulatory compliance for sweepstakes casino operations.
