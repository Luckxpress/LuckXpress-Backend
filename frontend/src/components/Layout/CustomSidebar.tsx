import React from 'react';
import { Sidebar, MenuItemLink } from 'react-admin';
import { Box, Badge } from '@mui/material';
import DashboardIcon from '@mui/icons-material/Dashboard';
import PeopleIcon from '@mui/icons-material/People';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import PaymentIcon from '@mui/icons-material/Payment';
import MoneyOffIcon from '@mui/icons-material/MoneyOff';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import SecurityIcon from '@mui/icons-material/Security';
import CasinoIcon from '@mui/icons-material/Casino';
import LocalOfferIcon from '@mui/icons-material/LocalOffer';
import ScienceIcon from '@mui/icons-material/Science';
import MessageIcon from '@mui/icons-material/Message';
import SupportAgentIcon from '@mui/icons-material/SupportAgent';
import GavelIcon from '@mui/icons-material/Gavel';
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import AssessmentIcon from '@mui/icons-material/Assessment';
import MonitorHeartIcon from '@mui/icons-material/MonitorHeart';
import SettingsIcon from '@mui/icons-material/Settings';

const menuItems = [
  { name: 'Dashboard', to: '/', icon: <DashboardIcon /> },
  { name: 'Users', to: '/users', icon: <PeopleIcon />, badge: 23 },
  { name: 'Leads', to: '/leads', icon: <TrendingUpIcon />, badge: 8 },
  { name: 'Wallets & Ledger', to: '/wallets', icon: <AccountBalanceWalletIcon /> },
  { name: 'Payments', to: '/payments', icon: <PaymentIcon />, badge: 5 },
  { name: 'Withdrawals', to: '/withdrawals', icon: <MoneyOffIcon />, badge: 8 },
  { name: 'KYC / AML', to: '/kyc', icon: <VerifiedUserIcon />, badge: 13 },
  { name: 'Fraud & Risk', to: '/fraud', icon: <SecurityIcon />, badge: 3 },
  { name: 'Game Providers', to: '/providers', icon: <CasinoIcon /> },
  { name: 'Promotions', to: '/promotions', icon: <LocalOfferIcon /> },
  { name: 'A/B Testing', to: '/ab-testing', icon: <ScienceIcon /> },
  { name: 'Messaging', to: '/messaging', icon: <MessageIcon /> },
  { name: 'Support', to: '/support', icon: <SupportAgentIcon /> },
  { name: 'Compliance', to: '/compliance', icon: <GavelIcon /> },
  { name: 'Finance', to: '/finance', icon: <AccountBalanceIcon /> },
  { name: 'Reports', to: '/reports', icon: <AssessmentIcon /> },
  { name: 'System Health', to: '/system', icon: <MonitorHeartIcon /> },
  { name: 'Settings', to: '/settings', icon: <SettingsIcon /> },
];

const CustomSidebar = (props: any) => (
  <Sidebar
    {...props}
    sx={{
      '& .RaSidebar-drawerPaper': {
        backgroundColor: '#2C3E50',
        color: 'white',
        width: 240,
      },
    }}
  >
    <Box sx={{ padding: 2 }}>
      <img src="/logo.png" alt="LuckXpress" style={{ width: '100%' }} />
    </Box>
    {menuItems.map((item) => (
      <MenuItemLink
        key={item.name}
        to={item.to}
        primaryText={
          item.badge ? (
            <Badge badgeContent={item.badge} color="error">
              {item.name}
            </Badge>
          ) : (
            item.name
          )
        }
        leftIcon={item.icon}
        sx={{
          color: 'white',
          '&:hover': {
            backgroundColor: '#34495e',
          },
          '&.RaMenuItemLink-active': {
            backgroundColor: '#4CAF50',
          },
        }}
      />
    ))}
  </Sidebar>
);

export default CustomSidebar;
