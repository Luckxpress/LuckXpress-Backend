import React from 'react';
import {
  Grid,
  Card,
  CardContent,
  Typography,
  Box,
  Paper,
  Avatar,
  LinearProgress,
  Chip,
  IconButton,
} from '@mui/material';
import {
  TrendingUp,
  TrendingDown,
  People,
  AttachMoney,
  Receipt,
  Security,
  Warning,
  CheckCircle,
  MoreVert,
} from '@mui/icons-material';
import {
  LineChart,
  Line,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
} from 'recharts';

// Mock data for charts
const revenueData = [
  { name: 'Jan', revenue: 65000, users: 1200 },
  { name: 'Feb', revenue: 78000, users: 1350 },
  { name: 'Mar', revenue: 82000, users: 1420 },
  { name: 'Apr', revenue: 91000, users: 1580 },
  { name: 'May', revenue: 87000, users: 1520 },
  { name: 'Jun', revenue: 95000, users: 1650 },
];

const transactionData = [
  { name: 'Deposits', value: 65, color: '#00d4ff' },
  { name: 'Withdrawals', value: 25, color: '#ff6b6b' },
  { name: 'Transfers', value: 10, color: '#4ecdc4' },
];

const kycStatusData = [
  { name: 'Mon', pending: 45, approved: 120, rejected: 8 },
  { name: 'Tue', pending: 52, approved: 135, rejected: 12 },
  { name: 'Wed', pending: 38, approved: 98, rejected: 6 },
  { name: 'Thu', pending: 61, approved: 142, rejected: 15 },
  { name: 'Fri', pending: 55, approved: 128, rejected: 9 },
  { name: 'Sat', pending: 42, approved: 95, rejected: 7 },
  { name: 'Sun', pending: 35, approved: 88, rejected: 5 },
];

interface KPICardProps {
  title: string;
  value: string;
  change: string;
  changeType: 'positive' | 'negative';
  icon: React.ReactElement;
  color: string;
}

const KPICard: React.FC<KPICardProps> = ({ title, value, change, changeType, icon, color }) => (
  <Card sx={{ height: '100%', position: 'relative', overflow: 'visible' }}>
    <CardContent>
      <Box display="flex" justifyContent="space-between" alignItems="flex-start">
        <Box>
          <Typography color="textSecondary" gutterBottom variant="h6">
            {title}
          </Typography>
          <Typography variant="h4" component="div" sx={{ fontWeight: 'bold', mb: 1 }}>
            {value}
          </Typography>
          <Box display="flex" alignItems="center">
            {changeType === 'positive' ? (
              <TrendingUp sx={{ color: '#4caf50', mr: 0.5 }} />
            ) : (
              <TrendingDown sx={{ color: '#f44336', mr: 0.5 }} />
            )}
            <Typography
              variant="body2"
              sx={{ color: changeType === 'positive' ? '#4caf50' : '#f44336' }}
            >
              {change}
            </Typography>
          </Box>
        </Box>
        <Avatar sx={{ bgcolor: color, width: 56, height: 56 }}>
          {icon}
        </Avatar>
      </Box>
    </CardContent>
  </Card>
);

export const Dashboard: React.FC = () => {
  return (
    <Box sx={{ flexGrow: 1 }}>
      {/* Page Header */}
      <Box mb={3}>
        <Typography variant="h4" gutterBottom sx={{ fontWeight: 'bold' }}>
          Dashboard Overview
        </Typography>
        <Typography variant="body1" color="textSecondary">
          Welcome back! Here's what's happening with your platform today.
        </Typography>
      </Box>

      {/* KPI Cards */}
      <Grid container spacing={3} mb={4}>
        <Grid item xs={12} sm={6} md={3}>
          <KPICard
            title="Total Revenue"
            value="$95,420"
            change="+12.5% from last month"
            changeType="positive"
            icon={<AttachMoney />}
            color="#4caf50"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <KPICard
            title="Active Users"
            value="1,652"
            change="+8.2% from last month"
            changeType="positive"
            icon={<People />}
            color="#2196f3"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <KPICard
            title="Transactions"
            value="8,429"
            change="+15.3% from last month"
            changeType="positive"
            icon={<Receipt />}
            color="#ff9800"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <KPICard
            title="KYC Pending"
            value="127"
            change="-5.1% from last month"
            changeType="negative"
            icon={<Security />}
            color="#9c27b0"
          />
        </Grid>
      </Grid>

      {/* Charts Row */}
      <Grid container spacing={3} mb={4}>
        {/* Revenue Chart */}
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                <Typography variant="h6" gutterBottom>
                  Revenue & User Growth
                </Typography>
                <IconButton>
                  <MoreVert />
                </IconButton>
              </Box>
              <ResponsiveContainer width="100%" height={300}>
                <AreaChart data={revenueData}>
                  <defs>
                    <linearGradient id="colorRevenue" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#00d4ff" stopOpacity={0.8}/>
                      <stop offset="95%" stopColor="#00d4ff" stopOpacity={0.1}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" />
                  <YAxis />
                  <Tooltip />
                  <Area
                    type="monotone"
                    dataKey="revenue"
                    stroke="#00d4ff"
                    fillOpacity={1}
                    fill="url(#colorRevenue)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>

        {/* Transaction Distribution */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Transaction Distribution
              </Typography>
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={transactionData}
                    cx="50%"
                    cy="50%"
                    outerRadius={80}
                    fill="#8884d8"
                    dataKey="value"
                    label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                  >
                    {transactionData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Bottom Row */}
      <Grid container spacing={3}>
        {/* KYC Status Chart */}
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                KYC Processing Status (Last 7 Days)
              </Typography>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={kycStatusData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" />
                  <YAxis />
                  <Tooltip />
                  <Bar dataKey="approved" fill="#4caf50" name="Approved" />
                  <Bar dataKey="pending" fill="#ff9800" name="Pending" />
                  <Bar dataKey="rejected" fill="#f44336" name="Rejected" />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>

        {/* Recent Activity */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Recent Activity
              </Typography>
              <Box>
                {[
                  { text: 'New user registration', time: '2 min ago', status: 'success' },
                  { text: 'Large withdrawal pending', time: '5 min ago', status: 'warning' },
                  { text: 'KYC document uploaded', time: '8 min ago', status: 'info' },
                  { text: 'Compliance alert triggered', time: '12 min ago', status: 'error' },
                  { text: 'System backup completed', time: '15 min ago', status: 'success' },
                ].map((activity, index) => (
                  <Box key={index} display="flex" alignItems="center" mb={2}>
                    <Box mr={2}>
                      {activity.status === 'success' && <CheckCircle sx={{ color: '#4caf50' }} />}
                      {activity.status === 'warning' && <Warning sx={{ color: '#ff9800' }} />}
                      {activity.status === 'info' && <Security sx={{ color: '#2196f3' }} />}
                      {activity.status === 'error' && <Warning sx={{ color: '#f44336' }} />}
                    </Box>
                    <Box flexGrow={1}>
                      <Typography variant="body2">{activity.text}</Typography>
                      <Typography variant="caption" color="textSecondary">
                        {activity.time}
                      </Typography>
                    </Box>
                  </Box>
                ))}
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};
