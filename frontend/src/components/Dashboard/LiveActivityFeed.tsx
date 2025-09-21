import React, { useState, useEffect } from 'react';
import { 
  Box, 
  Typography, 
  List, 
  ListItem, 
  ListItemIcon, 
  ListItemText, 
  Avatar,
  Chip,
  Divider,
  IconButton,
  Tooltip,
  Badge,
  Fade
} from '@mui/material';
import {
  PersonAdd,
  Payment,
  Security,
  Warning,
  CheckCircle,
  TrendingUp,
  MoreVert,
  NotificationsActive,
  Casino,
  Block,
  AccountBalanceWallet
} from '@mui/icons-material';

interface ActivityItem {
  id: number;
  type: string;
  message: string;
  time: string;
  icon: React.ReactNode;
  color: string;
  status: string;
  user?: string;
  amount?: string;
  location?: string;
  isNew?: boolean;
}

const activityData: ActivityItem[] = [
  {
    id: 1,
    type: 'user_registration',
    message: 'New user registered: john_doe_123',
    time: '2 minutes ago',
    icon: <PersonAdd />,
    color: '#4CAF50',
    status: 'success',
    location: 'United States',
    isNew: true
  },
  {
    id: 2,
    type: 'large_deposit',
    message: 'Large deposit: $2,500 by premium_player',
    time: '5 minutes ago',
    icon: <Payment />,
    color: '#2196F3',
    status: 'info',
    user: 'premium_player',
    amount: '$2,500',
    isNew: true
  },
  {
    id: 3,
    type: 'kyc_uploaded',
    message: 'KYC document uploaded by user_456',
    time: '8 minutes ago',
    icon: <Security />,
    color: '#FF9800',
    status: 'warning',
    user: 'user_456'
  },
  {
    id: 4,
    type: 'withdrawal_processed',
    message: 'Withdrawal completed: $500 to user_789',
    time: '12 minutes ago',
    icon: <CheckCircle />,
    color: '#4CAF50',
    status: 'success',
    user: 'user_789',
    amount: '$500'
  },
  {
    id: 5,
    type: 'high_activity',
    message: 'Unusual betting pattern detected',
    time: '15 minutes ago',
    icon: <Warning />,
    color: '#F44336',
    status: 'error',
    user: 'high_roller_22',
    isNew: true
  },
  {
    id: 6,
    type: 'revenue_milestone',
    message: 'Daily revenue target reached!',
    time: '20 minutes ago',
    icon: <TrendingUp />,
    color: '#9C27B0',
    status: 'success'
  },
  {
    id: 7,
    type: 'game_session',
    message: 'Long gaming session: 4+ hours by user_101',
    time: '25 minutes ago',
    icon: <Casino />,
    color: '#FF9800',
    status: 'warning',
    user: 'user_101'
  },
  {
    id: 8,
    type: 'account_blocked',
    message: 'Account blocked due to suspicious activity',
    time: '30 minutes ago',
    icon: <Block />,
    color: '#F44336',
    status: 'error',
    user: 'suspicious_user_42'
  },
  {
    id: 9,
    type: 'large_withdrawal',
    message: 'Large withdrawal request: $5,000 by vip_player',
    time: '35 minutes ago',
    icon: <AccountBalanceWallet />,
    color: '#FF9800',
    status: 'warning',
    user: 'vip_player',
    amount: '$5,000'
  }
];

const LiveActivityFeed: React.FC = () => {
  const [activities, setActivities] = useState<ActivityItem[]>(activityData);
  const [newActivityCount, setNewActivityCount] = useState<number>(3);
  
  // Simulate new activities coming in
  useEffect(() => {
    const interval = setInterval(() => {
      // Simulate a new activity
      if (Math.random() > 0.7) {
        const newActivity: ActivityItem = {
          id: Date.now(),
          type: 'user_registration',
          message: `New user registered: player_${Math.floor(Math.random() * 1000)}`,
          time: 'Just now',
          icon: <PersonAdd />,
          color: '#4CAF50',
          status: 'success',
          isNew: true
        };
        
        setActivities(prev => [newActivity, ...prev.slice(0, 8)]);
        setNewActivityCount(prev => prev + 1);
      }
    }, 30000); // Every 30 seconds
    
    return () => clearInterval(interval);
  }, []);

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'success': return 'success';
      case 'warning': return 'warning';
      case 'error': return 'error';
      case 'info': return 'info';
      default: return 'default';
    }
  };

  const handleClearNew = () => {
    setActivities(prev => 
      prev.map(activity => ({
        ...activity,
        isNew: false
      }))
    );
    setNewActivityCount(0);
  };

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box 
        sx={{ 
          display: 'flex', 
          justifyContent: 'space-between', 
          alignItems: 'center',
          mb: 1,
          px: 2
        }}
      >
        <Badge 
          badgeContent={newActivityCount} 
          color="error"
          sx={{ '& .MuiBadge-badge': { fontSize: '0.6rem' } }}
        >
          <NotificationsActive 
            color="action" 
            sx={{ fontSize: 20, mr: 1 }} 
          />
        </Badge>
        
        {newActivityCount > 0 && (
          <Tooltip title="Mark all as read">
            <Typography 
              variant="caption" 
              sx={{ 
                color: '#4CAF50', 
                cursor: 'pointer',
                fontWeight: 600,
                '&:hover': { textDecoration: 'underline' }
              }}
              onClick={handleClearNew}
            >
              Mark all as read
            </Typography>
          </Tooltip>
        )}
      </Box>
      
      <Box sx={{ flexGrow: 1, overflowY: 'auto', px: 2 }}>
        <List sx={{ padding: 0 }}>
          {activities.map((activity, index) => (
            <React.Fragment key={activity.id}>
              <ListItem 
                sx={{ 
                  px: 0, 
                  py: 1.5,
                  backgroundColor: activity.isNew ? 'rgba(76, 175, 80, 0.04)' : 'transparent',
                  borderRadius: 1,
                  transition: 'background-color 0.3s ease',
                  position: 'relative',
                  overflow: 'hidden'
                }}
              >
                {activity.isNew && (
                  <Box 
                    sx={{ 
                      position: 'absolute',
                      left: 0,
                      top: 0,
                      bottom: 0,
                      width: 3,
                      backgroundColor: '#4CAF50'
                    }} 
                  />
                )}
                
                <ListItemIcon sx={{ minWidth: 44 }}>
                  <Fade in={true} timeout={500}>
                    <Avatar 
                      sx={{ 
                        bgcolor: activity.color,
                        width: 36,
                        height: 36,
                        '& .MuiSvgIcon-root': {
                          fontSize: 18
                        },
                        boxShadow: `0 2px 8px ${activity.color}40`
                      }}
                    >
                      {activity.icon}
                    </Avatar>
                  </Fade>
                </ListItemIcon>
                
                <ListItemText
                  primary={
                    <Box display="flex" alignItems="center" justifyContent="space-between">
                      <Typography 
                        variant="body2" 
                        sx={{ 
                          flex: 1, 
                          mr: 1, 
                          fontWeight: activity.isNew ? 600 : 400,
                          color: activity.isNew ? '#2C3E50' : 'inherit'
                        }}
                      >
                        {activity.message}
                      </Typography>
                      <Chip 
                        size="small" 
                        label={activity.status}
                        color={getStatusColor(activity.status) as any}
                        variant="outlined"
                        sx={{ 
                          height: 20, 
                          '& .MuiChip-label': { 
                            px: 1, 
                            fontSize: '0.6rem',
                            fontWeight: 600,
                            textTransform: 'uppercase'
                          } 
                        }}
                      />
                    </Box>
                  }
                  secondary={
                    <Box>
                      <Typography 
                        variant="caption" 
                        color="textSecondary"
                        sx={{ display: 'block', mt: 0.5 }}
                      >
                        {activity.time}
                      </Typography>
                      
                      {/* Additional details based on activity type */}
                      {(activity.user || activity.amount || activity.location) && (
                        <Box 
                          sx={{ 
                            mt: 0.5, 
                            p: 0.75, 
                            backgroundColor: 'rgba(0,0,0,0.02)', 
                            borderRadius: 1,
                            fontSize: '0.7rem',
                            color: 'text.secondary'
                          }}
                        >
                          {activity.user && (
                            <Box component="span" sx={{ mr: 1 }}>
                              👤 {activity.user}
                            </Box>
                          )}
                          {activity.amount && (
                            <Box component="span" sx={{ mr: 1 }}>
                              💰 {activity.amount}
                            </Box>
                          )}
                          {activity.location && (
                            <Box component="span">
                              📍 {activity.location}
                            </Box>
                          )}
                        </Box>
                      )}
                    </Box>
                  }
                />
                
                <Tooltip title="More actions">
                  <IconButton size="small">
                    <MoreVert fontSize="small" />
                  </IconButton>
                </Tooltip>
              </ListItem>
              
              {index < activities.length - 1 && (
                <Divider variant="inset" component="li" sx={{ ml: 5 }} />
              )}
            </React.Fragment>
          ))}
        </List>
      </Box>
    </Box>
  );
};

export default LiveActivityFeed;
