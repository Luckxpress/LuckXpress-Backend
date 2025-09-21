import React, { useEffect, useState } from 'react';
import { Grid, Card, CardContent, Typography, Box, Button } from '@mui/material';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import TrendingDownIcon from '@mui/icons-material/TrendingDown';
import GroupIcon from '@mui/icons-material/Group';
import AttachMoneyIcon from '@mui/icons-material/AttachMoney';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import PendingIcon from '@mui/icons-material/Pending';
import { useDataProvider } from 'react-admin';

// Import chart components
import RevenueTrendChart from '../components/Dashboard/RevenueTrendChart';
import ConversionFunnel from '../components/Dashboard/ConversionFunnel';
import LiveActivityFeed from '../components/Dashboard/LiveActivityFeed';
import ProviderStatus from '../components/Dashboard/ProviderStatus';

// Metric Card Component
const MetricCard = ({ title, value, change, trend, icon, prefix = '$' }: {
  title: string;
  value: number;
  change: number;
  trend: 'up' | 'down';
  icon: React.ReactElement;
  prefix?: string;
}) => {
  const isPositive = trend === 'up';
  const trendColor = isPositive ? '#4CAF50' : '#F44336';
  const formattedValue = value.toLocaleString();
  
  return (
    <Card 
      sx={{ 
        height: '100%', 
        minHeight: 140, 
        borderRadius: 2,
        boxShadow: '0 4px 12px rgba(0,0,0,0.05)',
        transition: 'transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out',
        '&:hover': {
          transform: 'translateY(-4px)',
          boxShadow: '0 8px 16px rgba(0,0,0,0.1)',
        },
        position: 'relative',
        overflow: 'hidden',
        '&::before': {
          content: '""',
          position: 'absolute',
          top: 0,
          left: 0,
          width: '4px',
          height: '100%',
          backgroundColor: trendColor,
        }
      }}
    >
      <CardContent sx={{ p: 3 }}>
        <Box display="flex" justifyContent="space-between" alignItems="flex-start">
          <Box>
            <Typography 
              color="textSecondary" 
              variant="caption" 
              sx={{ 
                fontSize: '0.75rem', 
                fontWeight: 600, 
                textTransform: 'uppercase',
                letterSpacing: '0.5px',
                mb: 1,
                display: 'block'
              }}
            >
              {title}
            </Typography>
            <Typography 
              variant="h4" 
              component="div" 
              sx={{ 
                fontWeight: 700, 
                color: '#2C3E50',
                fontSize: '1.75rem',
                mb: 1
              }}
            >
              {prefix}{formattedValue}
            </Typography>
            <Box 
              display="flex" 
              alignItems="center" 
              sx={{
                backgroundColor: `${trendColor}10`,
                borderRadius: '4px',
                py: 0.5,
                px: 1,
                display: 'inline-flex',
              }}
            >
              {isPositive ? (
                <TrendingUpIcon sx={{ color: trendColor, fontSize: 16, mr: 0.5 }} />
              ) : (
                <TrendingDownIcon sx={{ color: trendColor, fontSize: 16, mr: 0.5 }} />
              )}
              <Typography
                variant="body2"
                sx={{ 
                  color: trendColor,
                  fontWeight: 600,
                  fontSize: '0.75rem'
                }}
              >
                {isPositive ? '+' : ''}{change}%
              </Typography>
            </Box>
          </Box>
          <Box 
            sx={{ 
              color: 'white',
              backgroundColor: '#4CAF50',
              borderRadius: '50%',
              width: 48,
              height: 48,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 4px 8px rgba(76, 175, 80, 0.2)'
            }}
          >
            {React.cloneElement(icon, { sx: { fontSize: 24 } })}
          </Box>
        </Box>
      </CardContent>
    </Card>
  );
};

// Main Dashboard Component
const Dashboard = () => {
  const dataProvider = useDataProvider();
  const [metrics, setMetrics] = useState({
    revenue: 60,
    activeUsers: 8,
    deposits: 0,
    withdrawals: 0,
    revenueChange: 12.3,
    usersChange: 8.1,
    depositsChange: -2.4,
    withdrawalsStatus: 'pending'
  });

  useEffect(() => {
    // Fetch dashboard metrics from backend API
    const fetchMetrics = async () => {
      try {
        const response = await fetch(`${process.env.REACT_APP_API_URL}/dashboard/metrics`);
        if (response.ok) {
          const data = await response.json();
          setMetrics(data);
          console.log('Dashboard metrics loaded from API:', data);
        } else {
          console.error('Failed to fetch metrics from API');
        }
      } catch (error) {
        console.error('Failed to fetch metrics:', error);
        // Fallback to existing mock data on error
      }
    };

    fetchMetrics();
    const interval = setInterval(fetchMetrics, 30000); // Refresh every 30 seconds
    
    return () => clearInterval(interval);
  }, [dataProvider]);

  return (
    <Box sx={{ p: 3, backgroundColor: '#f9fafc' }}>
      {/* Top Action Bar */}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={4} pb={2} borderBottom="1px solid #eaedf2">
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 700, color: '#2C3E50', mb: 0.5 }}>
            Global Dashboard
          </Typography>
          <Typography variant="body2" color="textSecondary">
            Real-time overview of platform operations • Last updated: {new Date().toLocaleTimeString()}
          </Typography>
        </Box>
        <Box display="flex" gap={2}>
          <Button 
            variant="outlined" 
            color="primary" 
            startIcon={<AttachMoneyIcon />}
            sx={{ 
              borderRadius: '8px',
              textTransform: 'none',
              fontWeight: 500,
              boxShadow: 'none'
            }}
          >
            Export CSV
          </Button>
          <Button 
            variant="contained" 
            color="warning"
            sx={{ 
              borderRadius: '8px',
              textTransform: 'none',
              fontWeight: 500,
              boxShadow: '0 2px 4px rgba(0,0,0,0.05)'
            }}
          >
            🔧 Maintenance Mode
          </Button>
          <Button 
            variant="contained" 
            color="error"
            sx={{ 
              borderRadius: '8px',
              textTransform: 'none',
              fontWeight: 500,
              boxShadow: '0 2px 4px rgba(0,0,0,0.05)'
            }}
          >
            🚨 Emergency Stop
          </Button>
        </Box>
      </Box>

      {/* Metric Cards */}
      <Grid container spacing={3} mb={3}>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Revenue (30d)"
            value={metrics.revenue}
            change={metrics.revenueChange}
            trend="up"
            icon={<AttachMoneyIcon sx={{ fontSize: 30 }} />}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Active Users"
            value={metrics.activeUsers}
            change={metrics.usersChange}
            trend="up"
            icon={<GroupIcon sx={{ fontSize: 30 }} />}
            prefix=""
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Deposits (24h)"
            value={metrics.deposits}
            change={metrics.depositsChange}
            trend="down"
            icon={<AccountBalanceWalletIcon sx={{ fontSize: 30 }} />}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Pending Withdrawals"
            value={metrics.withdrawals}
            change={0}
            trend="up"
            icon={<PendingIcon sx={{ fontSize: 30 }} />}
          />
        </Grid>
      </Grid>

      {/* Charts Row */}
      <Grid container spacing={3} mb={3}>
        <Grid item xs={12} md={8}>
          <Card 
            sx={{ 
              height: 400, 
              borderRadius: 2,
              boxShadow: '0 4px 12px rgba(0,0,0,0.05)',
              overflow: 'hidden'
            }}
          >
            <CardContent sx={{ p: 0 }}>
              <Box sx={{ p: 2, borderBottom: '1px solid #eaedf2', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography 
                  variant="h6" 
                  sx={{ 
                    fontWeight: 600, 
                    color: '#2C3E50',
                    fontSize: '1rem'
                  }}
                >
                  Revenue Trend
                </Typography>
                <Box sx={{ display: 'flex', gap: 1 }}>
                  <Button 
                    size="small" 
                    variant="outlined" 
                    sx={{ 
                      borderRadius: '4px', 
                      textTransform: 'none',
                      fontWeight: 500,
                      fontSize: '0.75rem',
                      py: 0.5,
                      minWidth: 'auto'
                    }}
                  >
                    Week
                  </Button>
                  <Button 
                    size="small" 
                    variant="contained" 
                    sx={{ 
                      borderRadius: '4px', 
                      textTransform: 'none',
                      fontWeight: 500,
                      fontSize: '0.75rem',
                      py: 0.5,
                      minWidth: 'auto',
                      bgcolor: '#4CAF50',
                      '&:hover': {
                        bgcolor: '#3d9140'
                      }
                    }}
                  >
                    Month
                  </Button>
                  <Button 
                    size="small" 
                    variant="outlined" 
                    sx={{ 
                      borderRadius: '4px', 
                      textTransform: 'none',
                      fontWeight: 500,
                      fontSize: '0.75rem',
                      py: 0.5,
                      minWidth: 'auto'
                    }}
                  >
                    Year
                  </Button>
                </Box>
              </Box>
              <Box sx={{ p: 2, height: 'calc(100% - 56px)' }}>
                <RevenueTrendChart />
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={4}>
          <Card 
            sx={{ 
              height: 400, 
              borderRadius: 2,
              boxShadow: '0 4px 12px rgba(0,0,0,0.05)',
              overflow: 'hidden'
            }}
          >
            <CardContent sx={{ p: 0 }}>
              <Box sx={{ p: 2, borderBottom: '1px solid #eaedf2' }}>
                <Typography 
                  variant="h6" 
                  sx={{ 
                    fontWeight: 600, 
                    color: '#2C3E50',
                    fontSize: '1rem'
                  }}
                >
                  Conversion Funnel
                </Typography>
              </Box>
              <Box sx={{ p: 2, height: 'calc(100% - 56px)' }}>
                <ConversionFunnel />
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Activity and Provider Status Row */}
      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Card 
            sx={{ 
              height: 400, 
              borderRadius: 2,
              boxShadow: '0 4px 12px rgba(0,0,0,0.05)',
              overflow: 'hidden'
            }}
          >
            <CardContent sx={{ p: 0 }}>
              <Box sx={{ p: 2, borderBottom: '1px solid #eaedf2', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography 
                  variant="h6" 
                  sx={{ 
                    fontWeight: 600, 
                    color: '#2C3E50',
                    fontSize: '1rem'
                  }}
                >
                  Live Activity Feed
                </Typography>
                <Button 
                  size="small" 
                  variant="text" 
                  color="primary"
                  sx={{ 
                    textTransform: 'none',
                    fontWeight: 500,
                    fontSize: '0.75rem'
                  }}
                >
                  View All
                </Button>
              </Box>
              <Box sx={{ p: 0, height: 'calc(100% - 56px)', overflowY: 'auto' }}>
                <LiveActivityFeed />
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={6}>
          <Card 
            sx={{ 
              height: 400, 
              borderRadius: 2,
              boxShadow: '0 4px 12px rgba(0,0,0,0.05)',
              overflow: 'hidden'
            }}
          >
            <CardContent sx={{ p: 0 }}>
              <Box sx={{ p: 2, borderBottom: '1px solid #eaedf2', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography 
                  variant="h6" 
                  sx={{ 
                    fontWeight: 600, 
                    color: '#2C3E50',
                    fontSize: '1rem'
                  }}
                >
                  Provider Status
                </Typography>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <Box 
                    sx={{ 
                      width: 8, 
                      height: 8, 
                      borderRadius: '50%', 
                      backgroundColor: '#4CAF50', 
                      mr: 1 
                    }} 
                  />
                  <Typography variant="caption" color="textSecondary">
                    All Systems Operational
                  </Typography>
                </Box>
              </Box>
              <Box sx={{ p: 0, height: 'calc(100% - 56px)' }}>
                <ProviderStatus />
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default Dashboard;
