import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Chip,
  Button,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  SelectChangeEvent,
  Alert,
  LinearProgress,
} from '@mui/material';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import {
  TrendingUp,
  TrendingDown,
  Warning,
  CheckCircle,
  Schedule,
  Error,
  Search,
  FilterList,
  Refresh,
} from '@mui/icons-material';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  BarChart,
  Bar,
} from 'recharts';

interface Transaction {
  id: string;
  userId: number;
  username: string;
  type: 'deposit' | 'withdrawal' | 'transfer' | 'bet' | 'win';
  amount: number;
  status: 'completed' | 'pending' | 'failed' | 'cancelled';
  timestamp: string;
  paymentMethod: string;
  description: string;
  flagged: boolean;
  riskScore: number;
}

// Mock transaction data
const mockTransactions: Transaction[] = [
  {
    id: 'TXN-001',
    userId: 1,
    username: 'john_doe',
    type: 'deposit',
    amount: 500,
    status: 'completed',
    timestamp: '2024-01-21T10:30:00Z',
    paymentMethod: 'Credit Card',
    description: 'Deposit via Visa ending 1234',
    flagged: false,
    riskScore: 2,
  },
  {
    id: 'TXN-002',
    userId: 2,
    username: 'jane_smith',
    type: 'withdrawal',
    amount: 1200,
    status: 'pending',
    timestamp: '2024-01-21T11:15:00Z',
    paymentMethod: 'Bank Transfer',
    description: 'Withdrawal to Bank Account',
    flagged: true,
    riskScore: 8,
  },
  {
    id: 'TXN-003',
    userId: 3,
    username: 'blocked_user',
    type: 'deposit',
    amount: 10000,
    status: 'failed',
    timestamp: '2024-01-21T09:45:00Z',
    paymentMethod: 'Wire Transfer',
    description: 'Large deposit attempt',
    flagged: true,
    riskScore: 9,
  },
  {
    id: 'TXN-004',
    userId: 1,
    username: 'john_doe',
    type: 'bet',
    amount: 100,
    status: 'completed',
    timestamp: '2024-01-21T12:00:00Z',
    paymentMethod: 'Wallet',
    description: 'Bet on Game #12345',
    flagged: false,
    riskScore: 1,
  },
  {
    id: 'TXN-005',
    userId: 2,
    username: 'jane_smith',
    type: 'win',
    amount: 250,
    status: 'completed',
    timestamp: '2024-01-21T12:30:00Z',
    paymentMethod: 'Wallet',
    description: 'Win from Game #12346',
    flagged: false,
    riskScore: 1,
  },
];

// Mock chart data
const transactionVolumeData = [
  { time: '00:00', deposits: 1200, withdrawals: 800, volume: 2000 },
  { time: '04:00', deposits: 800, withdrawals: 600, volume: 1400 },
  { time: '08:00', deposits: 2500, withdrawals: 1200, volume: 3700 },
  { time: '12:00', deposits: 3200, withdrawals: 2100, volume: 5300 },
  { time: '16:00', deposits: 2800, withdrawals: 1800, volume: 4600 },
  { time: '20:00', deposits: 3500, withdrawals: 2200, volume: 5700 },
];

const riskDistributionData = [
  { risk: 'Low (1-3)', count: 145, color: '#4caf50' },
  { risk: 'Medium (4-6)', count: 67, color: '#ff9800' },
  { risk: 'High (7-10)', count: 23, color: '#f44336' },
];

export const TransactionMonitoring: React.FC = () => {
  const [transactions, setTransactions] = useState<Transaction[]>(mockTransactions);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [typeFilter, setTypeFilter] = useState<string>('all');
  const [flaggedOnly, setFlaggedOnly] = useState(false);

  const handleStatusChange = (event: SelectChangeEvent) => {
    setStatusFilter(event.target.value);
  };

  const handleTypeChange = (event: SelectChangeEvent) => {
    setTypeFilter(event.target.value);
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'completed':
        return 'success';
      case 'pending':
        return 'warning';
      case 'failed':
        return 'error';
      case 'cancelled':
        return 'default';
      default:
        return 'default';
    }
  };

  const getTypeColor = (type: string) => {
    switch (type) {
      case 'deposit':
        return '#4caf50';
      case 'withdrawal':
        return '#f44336';
      case 'transfer':
        return '#2196f3';
      case 'bet':
        return '#ff9800';
      case 'win':
        return '#9c27b0';
      default:
        return '#666';
    }
  };

  const getRiskColor = (score: number) => {
    if (score <= 3) return '#4caf50';
    if (score <= 6) return '#ff9800';
    return '#f44336';
  };

  const filteredTransactions = transactions.filter(transaction => {
    const matchesSearch = transaction.id.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         transaction.username.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = statusFilter === 'all' || transaction.status === statusFilter;
    const matchesType = typeFilter === 'all' || transaction.type === typeFilter;
    const matchesFlagged = !flaggedOnly || transaction.flagged;
    
    return matchesSearch && matchesStatus && matchesType && matchesFlagged;
  });

  const columns: GridColDef[] = [
    {
      field: 'id',
      headerName: 'Transaction ID',
      width: 130,
      renderCell: (params) => (
        <Box>
          <Typography variant="body2" fontWeight="bold">
            {params.value}
          </Typography>
          {params.row.flagged && (
            <Warning sx={{ color: '#f44336', fontSize: 16 }} />
          )}
        </Box>
      ),
    },
    {
      field: 'username',
      headerName: 'User',
      width: 120,
    },
    {
      field: 'type',
      headerName: 'Type',
      width: 100,
      renderCell: (params) => (
        <Chip
          label={params.value}
          size="small"
          sx={{
            backgroundColor: getTypeColor(params.value),
            color: 'white',
            fontWeight: 'bold',
          }}
        />
      ),
    },
    {
      field: 'amount',
      headerName: 'Amount',
      width: 120,
      renderCell: (params) => (
        <Box display="flex" alignItems="center">
          {params.row.type === 'withdrawal' || params.row.type === 'bet' ? (
            <TrendingDown sx={{ color: '#f44336', mr: 0.5, fontSize: 16 }} />
          ) : (
            <TrendingUp sx={{ color: '#4caf50', mr: 0.5, fontSize: 16 }} />
          )}
          <Typography variant="body2" fontWeight="bold">
            ${params.value.toLocaleString()}
          </Typography>
        </Box>
      ),
    },
    {
      field: 'status',
      headerName: 'Status',
      width: 120,
      renderCell: (params) => (
        <Chip
          label={params.value}
          color={getStatusColor(params.value) as any}
          size="small"
          variant="outlined"
        />
      ),
    },
    {
      field: 'riskScore',
      headerName: 'Risk Score',
      width: 100,
      renderCell: (params) => (
        <Box display="flex" alignItems="center">
          <Box
            sx={{
              width: 20,
              height: 20,
              borderRadius: '50%',
              backgroundColor: getRiskColor(params.value),
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              mr: 1,
            }}
          >
            <Typography variant="caption" sx={{ color: 'white', fontWeight: 'bold' }}>
              {params.value}
            </Typography>
          </Box>
        </Box>
      ),
    },
    {
      field: 'paymentMethod',
      headerName: 'Payment Method',
      width: 140,
    },
    {
      field: 'timestamp',
      headerName: 'Time',
      width: 120,
      renderCell: (params) => new Date(params.value).toLocaleString(),
    },
  ];

  return (
    <Box sx={{ flexGrow: 1 }}>
      {/* Page Header */}
      <Box mb={3}>
        <Typography variant="h4" gutterBottom sx={{ fontWeight: 'bold' }}>
          Transaction Monitoring
        </Typography>
        <Typography variant="body1" color="textSecondary">
          Monitor all financial transactions and detect suspicious activities.
        </Typography>
      </Box>

      {/* Alert for High Risk Transactions */}
      <Alert severity="warning" sx={{ mb: 3 }}>
        <Typography variant="body2">
          <strong>2 high-risk transactions</strong> detected in the last hour. Review flagged transactions immediately.
        </Typography>
      </Alert>

      {/* KPI Cards */}
      <Grid container spacing={3} mb={4}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Total Volume (24h)
              </Typography>
              <Typography variant="h4" component="div" sx={{ fontWeight: 'bold' }}>
                $45,230
              </Typography>
              <Box display="flex" alignItems="center" mt={1}>
                <TrendingUp sx={{ color: '#4caf50', mr: 0.5 }} />
                <Typography variant="body2" sx={{ color: '#4caf50' }}>
                  +12.5%
                </Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Pending Transactions
              </Typography>
              <Typography variant="h4" component="div" sx={{ fontWeight: 'bold', color: '#ff9800' }}>
                {transactions.filter(t => t.status === 'pending').length}
              </Typography>
              <Box display="flex" alignItems="center" mt={1}>
                <Schedule sx={{ color: '#ff9800', mr: 0.5 }} />
                <Typography variant="body2" color="textSecondary">
                  Requires review
                </Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Flagged Transactions
              </Typography>
              <Typography variant="h4" component="div" sx={{ fontWeight: 'bold', color: '#f44336' }}>
                {transactions.filter(t => t.flagged).length}
              </Typography>
              <Box display="flex" alignItems="center" mt={1}>
                <Warning sx={{ color: '#f44336', mr: 0.5 }} />
                <Typography variant="body2" sx={{ color: '#f44336' }}>
                  High priority
                </Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Success Rate
              </Typography>
              <Typography variant="h4" component="div" sx={{ fontWeight: 'bold', color: '#4caf50' }}>
                94.2%
              </Typography>
              <LinearProgress
                variant="determinate"
                value={94.2}
                sx={{ mt: 1, height: 6, borderRadius: 3 }}
              />
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Charts */}
      <Grid container spacing={3} mb={4}>
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Transaction Volume (24h)
              </Typography>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={transactionVolumeData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="time" />
                  <YAxis />
                  <Tooltip />
                  <Line type="monotone" dataKey="deposits" stroke="#4caf50" strokeWidth={2} />
                  <Line type="monotone" dataKey="withdrawals" stroke="#f44336" strokeWidth={2} />
                </LineChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Risk Distribution
              </Typography>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={riskDistributionData} layout="horizontal">
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis type="number" />
                  <YAxis dataKey="risk" type="category" width={80} />
                  <Tooltip />
                  <Bar dataKey="count" fill="#00d4ff" />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Filters */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} md={3}>
              <TextField
                fullWidth
                placeholder="Search transactions..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                InputProps={{
                  startAdornment: <Search sx={{ mr: 1, color: 'text.secondary' }} />,
                }}
              />
            </Grid>
            <Grid item xs={12} md={2}>
              <FormControl fullWidth>
                <InputLabel>Status</InputLabel>
                <Select value={statusFilter} onChange={handleStatusChange} label="Status">
                  <MenuItem value="all">All Status</MenuItem>
                  <MenuItem value="completed">Completed</MenuItem>
                  <MenuItem value="pending">Pending</MenuItem>
                  <MenuItem value="failed">Failed</MenuItem>
                  <MenuItem value="cancelled">Cancelled</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={2}>
              <FormControl fullWidth>
                <InputLabel>Type</InputLabel>
                <Select value={typeFilter} onChange={handleTypeChange} label="Type">
                  <MenuItem value="all">All Types</MenuItem>
                  <MenuItem value="deposit">Deposit</MenuItem>
                  <MenuItem value="withdrawal">Withdrawal</MenuItem>
                  <MenuItem value="transfer">Transfer</MenuItem>
                  <MenuItem value="bet">Bet</MenuItem>
                  <MenuItem value="win">Win</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={5}>
              <Box display="flex" gap={1}>
                <Button
                  variant={flaggedOnly ? "contained" : "outlined"}
                  color="error"
                  onClick={() => setFlaggedOnly(!flaggedOnly)}
                  startIcon={<Warning />}
                >
                  Flagged Only
                </Button>
                <Button
                  variant="outlined"
                  startIcon={<FilterList />}
                >
                  Advanced Filters
                </Button>
                <Button
                  variant="outlined"
                  startIcon={<Refresh />}
                >
                  Refresh
                </Button>
              </Box>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Transaction Table */}
      <Card>
        <CardContent>
          <DataGrid
            rows={filteredTransactions}
            columns={columns}
            initialState={{
              pagination: {
                paginationModel: { page: 0, pageSize: 10 },
              },
            }}
            pageSizeOptions={[10, 25, 50]}
            checkboxSelection
            disableRowSelectionOnClick
            sx={{
              border: 'none',
              '& .MuiDataGrid-cell': {
                borderColor: '#f0f0f0',
              },
              '& .MuiDataGrid-columnHeaders': {
                backgroundColor: '#f8f9fa',
                borderColor: '#f0f0f0',
              },
              '& .MuiDataGrid-row': {
                '&:hover': {
                  backgroundColor: '#f5f5f5',
                },
              },
            }}
          />
        </CardContent>
      </Card>
    </Box>
  );
};
