import React from 'react';
import { AppBar, AppBarProps } from 'react-admin';
import {
  Box,
  IconButton,
  Badge,
  Avatar,
  Typography,
} from '@mui/material';
import {
  Notifications as NotificationsIcon,
  AccountCircle,
  Settings as SettingsIcon,
} from '@mui/icons-material';

const CustomAppBar = (props: AppBarProps) => (
  <AppBar
    {...props}
    sx={{
      backgroundColor: '#FFFFFF',
      color: '#2C3E50',
      boxShadow: '0 1px 3px rgba(0,0,0,0.12)',
      '& .RaAppBar-toolbar': {
        paddingRight: 2,
      },
    }}
    userMenu={
      <Box display="flex" alignItems="center" gap={1}>
        <IconButton color="inherit">
          <Badge badgeContent={4} color="error">
            <NotificationsIcon />
          </Badge>
        </IconButton>
        
        <IconButton color="inherit">
          <SettingsIcon />
        </IconButton>
        
        <Box display="flex" alignItems="center" gap={1} ml={1}>
          <Avatar 
            sx={{ 
              width: 32, 
              height: 32,
              bgcolor: '#4CAF50',
              fontSize: '0.875rem'
            }}
          >
            A
          </Avatar>
          <Box display="flex" flexDirection="column" alignItems="flex-start">
            <Typography variant="body2" fontWeight="bold">
              Admin User
            </Typography>
            <Typography variant="caption" color="textSecondary">
              Super Admin
            </Typography>
          </Box>
        </Box>
      </Box>
    }
  >
    <Box display="flex" alignItems="center" width="100%">
      <Typography variant="h6" sx={{ flexGrow: 1, fontWeight: 'bold' }}>
        LuckXpress Admin Dashboard
      </Typography>
    </Box>
  </AppBar>
);

export default CustomAppBar;
