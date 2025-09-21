// Base interfaces for the LuckXpress admin dashboard

export interface User {
  id: string;
  email: string;
  username: string;
  fullName: string;
  status: 'active' | 'suspended' | 'pending' | 'blocked';
  kycStatus: 'verified' | 'pending' | 'rejected' | 'not_submitted';
  goldCoins: number;
  sweepCoins: number;
  createdAt: Date;
  lastLogin: Date;
  state: string;
  country: string;
  phoneNumber?: string;
  dateOfBirth?: Date;
  address?: Address;
  bankAccount?: BankAccount;
  riskScore: number;
  totalDeposits: number;
  totalWithdrawals: number;
  totalBets: number;
  totalWins: number;
  isVip: boolean;
  referralCode?: string;
  referredBy?: string;
  notes?: string;
}

export interface Address {
  street: string;
  city: string;
  state: string;
  zipCode: string;
  country: string;
}

export interface BankAccount {
  accountNumber: string;
  routingNumber: string;
  bankName: string;
  accountType: 'checking' | 'savings';
  isVerified: boolean;
}

export interface Transaction {
  id: string;
  userId: string;
  username?: string;
  type: 'deposit' | 'withdrawal' | 'bet' | 'win' | 'bonus' | 'adjustment' | 'refund';
  amount: number;
  currency: 'GC' | 'SC' | 'USD';
  status: 'pending' | 'completed' | 'failed' | 'cancelled' | 'processing';
  timestamp: Date;
  paymentMethod: string;
  description: string;
  reference?: string;
  fees?: number;
  exchangeRate?: number;
  processingTime?: number;
  failureReason?: string;
  approvedBy?: string;
  flagged: boolean;
  riskScore: number;
  ipAddress?: string;
  deviceInfo?: string;
}

export interface KYCDocument {
  id: string;
  userId: string;
  type: 'passport' | 'drivers_license' | 'national_id' | 'utility_bill' | 'bank_statement';
  status: 'pending' | 'approved' | 'rejected';
  uploadedAt: Date;
  reviewedAt?: Date;
  reviewedBy?: string;
  rejectionReason?: string;
  fileUrl: string;
  fileName: string;
  fileSize: number;
  expiryDate?: Date;
}

export interface Wallet {
  id: string;
  userId: string;
  goldCoins: number;
  sweepCoins: number;
  lastUpdated: Date;
  isLocked: boolean;
  lockReason?: string;
  maxDailyDeposit: number;
  maxDailyWithdrawal: number;
  pendingDeposits: number;
  pendingWithdrawals: number;
}

export interface DashboardMetrics {
  revenue: number;
  activeUsers: number;
  deposits24h: number;
  withdrawals24h: number;
  pendingWithdrawals: number;
  newRegistrations24h: number;
  pendingKyc: number;
  flaggedTransactions: number;
  systemUptime: number;
  revenueChange: number;
  usersChange: number;
  depositsChange: number;
  withdrawalsChange: number;
}

export interface GameProvider {
  id: string;
  name: string;
  status: 'active' | 'inactive' | 'maintenance';
  games: Game[];
  apiEndpoint: string;
  lastSync: Date;
  totalRevenue: number;
  playerCount: number;
  rtp: number; // Return to Player percentage
}

export interface Game {
  id: string;
  providerId: string;
  name: string;
  category: 'slots' | 'table' | 'live' | 'instant' | 'lottery';
  status: 'active' | 'inactive';
  minBet: number;
  maxBet: number;
  rtp: number;
  popularity: number;
  totalPlays: number;
  totalRevenue: number;
  imageUrl?: string;
}

export interface Promotion {
  id: string;
  name: string;
  type: 'welcome_bonus' | 'deposit_bonus' | 'free_coins' | 'cashback' | 'tournament';
  status: 'active' | 'inactive' | 'scheduled' | 'expired';
  startDate: Date;
  endDate: Date;
  goldCoinsAmount?: number;
  sweepCoinsAmount?: number;
  percentageBonus?: number;
  minDeposit?: number;
  maxBonus?: number;
  wagering?: number;
  eligibleUsers: string[];
  usedCount: number;
  maxUsage?: number;
  createdBy: string;
  createdAt: Date;
}

export interface ComplianceAlert {
  id: string;
  type: 'kyc_required' | 'large_transaction' | 'suspicious_activity' | 'state_restriction' | 'age_verification' | 'velocity_check';
  severity: 'low' | 'medium' | 'high' | 'critical';
  userId: string;
  username?: string;
  description: string;
  status: 'open' | 'investigating' | 'resolved' | 'false_positive';
  createdAt: Date;
  assignedTo?: string;
  resolvedAt?: Date;
  resolution?: string;
  relatedTransactionId?: string;
  automatedAction?: 'account_suspend' | 'transaction_block' | 'kyc_request' | 'manual_review';
}

export interface AuditLog {
  id: string;
  userId: string;
  adminId?: string;
  action: string;
  resource: string;
  resourceId?: string;
  oldValue?: any;
  newValue?: any;
  timestamp: Date;
  ipAddress: string;
  userAgent: string;
  result: 'success' | 'failure';
  reason?: string;
}

export interface SystemHealth {
  id: string;
  service: string;
  status: 'healthy' | 'warning' | 'critical' | 'down';
  uptime: number;
  lastCheck: Date;
  responseTime: number;
  errorRate: number;
  throughput: number;
  memoryUsage: number;
  cpuUsage: number;
  diskUsage: number;
}

export interface AdminUser {
  id: string;
  email: string;
  username: string;
  role: 'super_admin' | 'admin' | 'compliance' | 'support' | 'finance' | 'readonly';
  permissions: Permission[];
  isActive: boolean;
  lastLogin: Date;
  createdAt: Date;
  createdBy: string;
  twoFactorEnabled: boolean;
}

export interface Permission {
  resource: string;
  actions: ('create' | 'read' | 'update' | 'delete')[];
}

export interface SupportTicket {
  id: string;
  userId: string;
  username?: string;
  category: 'technical' | 'account' | 'payment' | 'kyc' | 'complaint' | 'general';
  priority: 'low' | 'medium' | 'high' | 'urgent';
  status: 'open' | 'in_progress' | 'waiting_customer' | 'resolved' | 'closed';
  subject: string;
  description: string;
  assignedTo?: string;
  createdAt: Date;
  updatedAt: Date;
  resolvedAt?: Date;
  messages: TicketMessage[];
  attachments: TicketAttachment[];
}

export interface TicketMessage {
  id: string;
  ticketId: string;
  senderId: string;
  senderType: 'user' | 'admin';
  message: string;
  timestamp: Date;
  isInternal: boolean;
}

export interface TicketAttachment {
  id: string;
  ticketId: string;
  fileName: string;
  fileSize: number;
  fileUrl: string;
  uploadedAt: Date;
  uploadedBy: string;
}

export interface ReportConfig {
  id: string;
  name: string;
  type: 'user_activity' | 'financial' | 'compliance' | 'games' | 'marketing';
  parameters: Record<string, any>;
  schedule?: 'daily' | 'weekly' | 'monthly';
  recipients: string[];
  format: 'pdf' | 'csv' | 'excel';
  isActive: boolean;
  lastRun?: Date;
  nextRun?: Date;
  createdBy: string;
  createdAt: Date;
}

export interface ABTest {
  id: string;
  name: string;
  description: string;
  status: 'draft' | 'running' | 'paused' | 'completed';
  startDate: Date;
  endDate: Date;
  variants: ABTestVariant[];
  targetAudience: {
    userSegments: string[];
    percentage: number;
  };
  metrics: ABTestMetric[];
  results?: ABTestResults;
  createdBy: string;
  createdAt: Date;
}

export interface ABTestVariant {
  id: string;
  name: string;
  description: string;
  percentage: number;
  config: Record<string, any>;
}

export interface ABTestMetric {
  id: string;
  name: string;
  type: 'conversion' | 'engagement' | 'revenue' | 'retention';
  goalValue?: number;
}

export interface ABTestResults {
  participants: number;
  conversionRate: number;
  confidenceLevel: number;
  statisticalSignificance: boolean;
  variantResults: {
    variantId: string;
    participants: number;
    conversions: number;
    conversionRate: number;
    revenue: number;
  }[];
}

// API Response types
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
  message?: string;
  meta?: {
    page?: number;
    limit?: number;
    total?: number;
    totalPages?: number;
  };
}

export interface PaginatedResponse<T> {
  items: T[];
  page: number;
  limit: number;
  total: number;
  totalPages: number;
}

// Filter and search types
export interface UserFilters {
  status?: User['status'][];
  kycStatus?: User['kycStatus'][];
  states?: string[];
  dateRange?: {
    start: Date;
    end: Date;
  };
  riskScoreRange?: {
    min: number;
    max: number;
  };
  search?: string;
}

export interface TransactionFilters {
  types?: Transaction['type'][];
  statuses?: Transaction['status'][];
  currencies?: Transaction['currency'][];
  amountRange?: {
    min: number;
    max: number;
  };
  dateRange?: {
    start: Date;
    end: Date;
  };
  flaggedOnly?: boolean;
  search?: string;
}

// Chart data types
export interface ChartDataPoint {
  x: string | number | Date;
  y: number;
  label?: string;
}

export interface TimeSeriesData {
  timestamp: Date;
  value: number;
  category?: string;
}

export interface PieChartData {
  name: string;
  value: number;
  color?: string;
}

// WebSocket message types
export interface WebSocketMessage {
  type: string;
  data: any;
  timestamp: Date;
}

export interface RealTimeUpdate {
  type: 'user_registered' | 'transaction_completed' | 'kyc_submitted' | 'compliance_alert' | 'system_alert';
  data: any;
  timestamp: Date;
}

// Error types
export interface AppError {
  code: string;
  message: string;
  details?: any;
  timestamp: Date;
}
