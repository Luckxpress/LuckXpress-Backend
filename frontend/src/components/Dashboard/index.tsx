import React from 'react';
import {
  Grid,
  Typography,
  Box,
  Avatar,
  LinearProgress,
  Card,
  CardContent,
  CardHeader,
} from '@mui/material';
import {
  TrendingUp,
  TrendingDown,
  People,
  AttachMoney,
  Receipt,
  Security,
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

const Dashboard: React.FC = () => {
  return (
    <Box sx={{ flexGrow: 1, p: 3 }}>
      {/* Page Header */}
      <Box mb={3}>
        <Typography variant="h4" gutterBottom sx={{ fontWeight: 'bold' }}>
          LuckXpress Dashboard
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
            color="#4CAF50"
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
      <Grid container spacing={3}>
        <Grid item xs={12} md={8}>
          <Card>
            <CardHeader title="Revenue & User Growth" />
            <CardContent>
              <ResponsiveContainer width="100%" height={300}>
                <AreaChart data={revenueData}>
                  <defs>
                    <linearGradient id="colorRevenue" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#4CAF50" stopOpacity={0.8}/>
                      <stop offset="95%" stopColor="#4CAF50" stopOpacity={0.1}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" />
                  <YAxis />
                  <Tooltip />
                  <Area
                    type="monotone"
                    dataKey="revenue"
                    stroke="#4CAF50"
                    fillOpacity={1}
                    fill="url(#colorRevenue)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={4}>
          <Card>
            <CardHeader title="System Status" />
            <CardContent>
              <Box mb={2}>
                <Typography variant="body2" gutterBottom>
                  Server Uptime
                </Typography>
                <LinearProgress
                  variant="determinate"
                  value={99.9}
                  sx={{ height: 8, borderRadius: 4 }}
                />
                <Typography variant="caption" color="textSecondary">
                  99.9%
                </Typography>
              </Box>
              
              <Box mb={2}>
                <Typography variant="body2" gutterBottom>
                  Database Performance
                </Typography>
                <LinearProgress
                  variant="determinate"
                  value={95}
                  color="warning"
                  sx={{ height: 8, borderRadius: 4 }}
                />
                <Typography variant="caption" color="textSecondary">
                  95%
                </Typography>
              </Box>
              
              <Box>
                <Typography variant="body2" gutterBottom>
                  API Response Time
                </Typography>
                <LinearProgress
                  variant="determinate"
                  value={87}
                  color="success"
                  sx={{ height: 8, borderRadius: 4 }}
                />
                <Typography variant="caption" color="textSecondary">
                  87ms avg
                </Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default Dashboard;
