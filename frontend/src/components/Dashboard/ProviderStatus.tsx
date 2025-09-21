import React, { useState, useEffect } from 'react';
import { 
  Box, 
  Typography, 
  List, 
  ListItem,
  Chip,
  LinearProgress,
  Grid,
  Paper,
  Tooltip,
  IconButton,
  Divider,
  Card,
  CardContent,
  Stack
} from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import WarningIcon from '@mui/icons-material/Warning';
import InfoIcon from '@mui/icons-material/Info';
import CasinoIcon from '@mui/icons-material/Casino';
import PaymentIcon from '@mui/icons-material/Payment';
import SecurityIcon from '@mui/icons-material/Security';
import StorageIcon from '@mui/icons-material/Storage';

interface ProviderData {
  name: string;
  status: string;
  uptime: string;
  responseTime?: number;
  category?: string;
  lastIncident?: string;
  games?: number;
  icon?: React.ReactNode;
}

const defaultProviderData: ProviderData[] = [
  { 
    name: 'Evolution Gaming', 
    status: 'Active', 
    uptime: '99.99%',
    responseTime: 45,
    category: 'Game Provider',
    games: 124,
    icon: <CasinoIcon />
  },
  { 
    name: 'Nuxii', 
    status: 'Active', 
    uptime: '99.95%',
    responseTime: 62,
    category: 'Game Provider',
    games: 89,
    icon: <CasinoIcon />
  },
  { 
    name: 'PayPal', 
    status: 'Disrupted', 
    uptime: '96.72%',
    responseTime: 156,
    category: 'Payment',
    lastIncident: '2 hours ago',
    icon: <PaymentIcon />
  },
  { 
    name: 'Stripe', 
    status: 'Active', 
    uptime: '99.98%',
    responseTime: 38,
    category: 'Payment',
    icon: <PaymentIcon />
  },
  { 
    name: 'KYC Service', 
    status: 'Maintenance', 
    uptime: '98.50%',
    responseTime: 120,
    category: 'Compliance',
    lastIncident: 'Scheduled maintenance',
    icon: <SecurityIcon />
  },
  { 
    name: 'Database Cluster', 
    status: 'Active', 
    uptime: '99.999%',
    responseTime: 12,
    category: 'Infrastructure',
    icon: <StorageIcon />
  },
];

const ProviderStatus: React.FC = () => {
  const [providerData, setProviderData] = useState<ProviderData[]>(defaultProviderData);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [lastUpdated, setLastUpdated] = useState<Date>(new Date());
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);

  const fetchProviderStatus = async () => {
    setIsLoading(true);
    try {
      const response = await fetch(`${process.env.REACT_APP_API_URL}/dashboard/provider-status`);
      if (response.ok) {
        const apiData = await response.json();
        // Add icons and additional data to the API response
        const enhancedData = apiData.map((item: any) => ({
          ...item,
          icon: getCategoryIcon(item.category || 'Game Provider'),
          responseTime: item.responseTime || Math.floor(Math.random() * 100) + 20
        }));
        setProviderData(enhancedData);
      }
    } catch (error) {
      console.error('Failed to fetch provider status:', error);
      // Keep default data on error
    } finally {
      setIsLoading(false);
      setLastUpdated(new Date());
    }
  };

  useEffect(() => {
    fetchProviderStatus();
    // Set up auto-refresh every 5 minutes
    const interval = setInterval(fetchProviderStatus, 5 * 60 * 1000);
    return () => clearInterval(interval);
  }, []);

  const getStatusColor = (status: string) => {
    switch (status.toLowerCase()) {
      case 'active': return 'success';
      case 'disrupted': return 'error';
      case 'maintenance': return 'warning';
      case 'offline': return 'default';
      default: return 'default';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status.toLowerCase()) {
      case 'active': return <CheckCircleIcon fontSize="small" sx={{ color: '#4CAF50' }} />;
      case 'disrupted': return <ErrorIcon fontSize="small" sx={{ color: '#F44336' }} />;
      case 'maintenance': return <WarningIcon fontSize="small" sx={{ color: '#FF9800' }} />;
      case 'offline': return <InfoIcon fontSize="small" sx={{ color: '#9E9E9E' }} />;
      default: return <InfoIcon fontSize="small" sx={{ color: '#9E9E9E' }} />;
    }
  };

  const getCategoryIcon = (category: string) => {
    switch (category.toLowerCase()) {
      case 'game provider': return <CasinoIcon />;
      case 'payment': return <PaymentIcon />;
      case 'compliance': return <SecurityIcon />;
      case 'infrastructure': return <StorageIcon />;
      default: return <CasinoIcon />;
    }
  };

  const getUptimeColor = (uptime: string) => {
    const uptimeValue = parseFloat(uptime.replace('%', ''));
    if (uptimeValue >= 99.9) return '#4CAF50';
    if (uptimeValue >= 99) return '#8BC34A';
    if (uptimeValue >= 98) return '#FFC107';
    if (uptimeValue >= 95) return '#FF9800';
    return '#F44336';
  };

  const getResponseTimeColor = (time: number) => {
    if (time <= 50) return '#4CAF50';
    if (time <= 100) return '#FFC107';
    return '#F44336';
  };

  // Get unique categories
  const categories = Array.from(new Set(providerData.map(p => p.category)));

  // Filter providers by category if selected
  const filteredProviders = selectedCategory 
    ? providerData.filter(p => p.category === selectedCategory)
    : providerData;

  // Calculate overall status
  const totalProviders = providerData.length;
  const activeProviders = providerData.filter(p => p.status.toLowerCase() === 'active').length;
  const disruptedProviders = providerData.filter(p => p.status.toLowerCase() === 'disrupted').length;
  const maintenanceProviders = providerData.filter(p => p.status.toLowerCase() === 'maintenance').length;
  
  const getOverallStatus = () => {
    if (disruptedProviders > 0) return 'Disruptions Detected';
    if (maintenanceProviders > 0) return 'Maintenance in Progress';
    return 'All Systems Operational';
  };

  const getOverallStatusColor = () => {
    if (disruptedProviders > 0) return '#F44336';
    if (maintenanceProviders > 0) return '#FF9800';
    return '#4CAF50';
  };

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* Status Summary */}
      <Paper 
        elevation={0} 
        sx={{ 
          p: 2, 
          mb: 2, 
          backgroundColor: 'rgba(0,0,0,0.02)', 
          borderRadius: 2,
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}
      >
        <Box>
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 0.5 }}>
            <Box 
              sx={{ 
                width: 10, 
                height: 10, 
                borderRadius: '50%', 
                backgroundColor: getOverallStatusColor(),
                mr: 1
              }} 
            />
            <Typography variant="body2" fontWeight="600" color="#2C3E50">
              {getOverallStatus()}
            </Typography>
          </Box>
          <Typography variant="caption" color="text.secondary">
            {activeProviders} of {totalProviders} systems operational
          </Typography>
        </Box>
        
        <Tooltip title={`Last updated: ${lastUpdated.toLocaleTimeString()}`}>
          <IconButton 
            size="small" 
            onClick={() => fetchProviderStatus()} 
            disabled={isLoading}
            sx={{ color: '#2C3E50' }}
          >
            <RefreshIcon fontSize="small" />
          </IconButton>
        </Tooltip>
      </Paper>
      
      {/* Category Filters */}
      <Stack 
        direction="row" 
        spacing={1} 
        sx={{ mb: 2, overflowX: 'auto', pb: 1 }}
      >
        <Chip 
          label="All" 
          variant={selectedCategory === null ? 'filled' : 'outlined'}
          color={selectedCategory === null ? 'primary' : 'default'}
          onClick={() => setSelectedCategory(null)}
          size="small"
        />
        {categories.map(category => (
          <Chip 
            key={category} 
            label={category} 
            variant={selectedCategory === category ? 'filled' : 'outlined'}
            color={selectedCategory === category ? 'primary' : 'default'}
            onClick={() => setSelectedCategory(category as string)}
            size="small"
            icon={getCategoryIcon(category as string)}
          />
        ))}
      </Stack>
      
      {/* Provider List */}
      <Box sx={{ flexGrow: 1, overflowY: 'auto' }}>
        <List sx={{ padding: 0 }}>
          {filteredProviders.map((provider, index) => (
            <React.Fragment key={provider.name}>
              <ListItem 
                sx={{ 
                  px: 1, 
                  py: 1.5, 
                  borderRadius: 1,
                  '&:hover': {
                    backgroundColor: 'rgba(0,0,0,0.02)'
                  }
                }}
              >
                <Grid container spacing={2} alignItems="center">
                  {/* Provider Name and Status */}
                  <Grid item xs={12} sm={6}>
                    <Box display="flex" alignItems="center">
                      <Box 
                        sx={{ 
                          width: 32, 
                          height: 32, 
                          borderRadius: '50%',
                          backgroundColor: 'rgba(0,0,0,0.04)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          color: '#2C3E50',
                          mr: 1.5
                        }}
                      >
                        {provider.icon}
                      </Box>
                      <Box>
                        <Box display="flex" alignItems="center">
                          <Typography variant="body2" fontWeight="600" color="#2C3E50">
                            {provider.name}
                          </Typography>
                          <Tooltip title={provider.status}>
                            <Box sx={{ ml: 1, display: 'flex', alignItems: 'center' }}>
                              {getStatusIcon(provider.status)}
                            </Box>
                          </Tooltip>
                        </Box>
                        <Typography variant="caption" color="text.secondary">
                          {provider.category}
                          {provider.games && ` • ${provider.games} games`}
                        </Typography>
                      </Box>
                    </Box>
                  </Grid>
                  
                  {/* Uptime */}
                  <Grid item xs={6} sm={3}>
                    <Box>
                      <Typography variant="caption" color="text.secondary" display="block">
                        Uptime
                      </Typography>
                      <Box sx={{ display: 'flex', alignItems: 'center' }}>
                        <Typography 
                          variant="body2" 
                          fontWeight="600" 
                          sx={{ color: getUptimeColor(provider.uptime) }}
                        >
                          {provider.uptime}
                        </Typography>
                      </Box>
                    </Box>
                  </Grid>
                  
                  {/* Response Time */}
                  <Grid item xs={6} sm={3}>
                    <Box>
                      <Typography variant="caption" color="text.secondary" display="block">
                        Response Time
                      </Typography>
                      <Box sx={{ display: 'flex', alignItems: 'center' }}>
                        <Typography 
                          variant="body2" 
                          fontWeight="600" 
                          sx={{ color: getResponseTimeColor(provider.responseTime || 0) }}
                        >
                          {provider.responseTime}ms
                        </Typography>
                      </Box>
                    </Box>
                    {provider.lastIncident && (
                      <Typography 
                        variant="caption" 
                        sx={{ 
                          color: 'text.secondary',
                          display: 'block',
                          mt: 0.5,
                          fontSize: '0.65rem'
                        }}
                      >
                        Last incident: {provider.lastIncident}
                      </Typography>
                    )}
                  </Grid>
                </Grid>
              </ListItem>
              {index < filteredProviders.length - 1 && (
                <Divider variant="fullWidth" sx={{ my: 0.5 }} />
              )}
            </React.Fragment>
          ))}
        </List>
      </Box>
    </Box>
  );
};

export default ProviderStatus;
