import React from 'react';
import {
  Box,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Typography,
  Divider,
  Collapse,
} from '@mui/material';
import {
  Dashboard as DashboardIcon,
  People as PeopleIcon,
  AccountBalance as WalletIcon,
  Receipt as TransactionIcon,
  Security as ComplianceIcon,
  Analytics as AnalyticsIcon,
  Settings as SettingsIcon,
  Support as SupportIcon,
  Gavel as AuditIcon,
  Warning as AlertIcon,
  ExpandLess,
  ExpandMore,
  PersonAdd,
  VerifiedUser,
  Block,
  AttachMoney,
  TrendingUp,
  Assessment,
  Notifications,
  AdminPanelSettings,
} from '@mui/icons-material';
import { useNavigate, useLocation } from 'react-router-dom';

interface SidebarItem {
  text: string;
  icon: React.ReactElement;
  path?: string;
  children?: SidebarItem[];
}

const sidebarItems: SidebarItem[] = [
  {
    text: 'Dashboard',
    icon: <DashboardIcon />,
    path: '/dashboard',
  },
  {
    text: 'User Management',
    icon: <PeopleIcon />,
    children: [
      { text: 'All Users', icon: <PeopleIcon />, path: '/users' },
      { text: 'User Registration', icon: <PersonAdd />, path: '/users/registration' },
      { text: 'KYC Verification', icon: <VerifiedUser />, path: '/users/kyc' },
      { text: 'Blocked Users', icon: <Block />, path: '/users/blocked' },
    ],
  },
  {
    text: 'Wallet Management',
    icon: <WalletIcon />,
    children: [
      { text: 'All Wallets', icon: <WalletIcon />, path: '/wallets' },
      { text: 'Deposits', icon: <AttachMoney />, path: '/wallets/deposits' },
      { text: 'Withdrawals', icon: <TrendingUp />, path: '/wallets/withdrawals' },
      { text: 'Balance Adjustments', icon: <Assessment />, path: '/wallets/adjustments' },
    ],
  },
  {
    text: 'Transactions',
    icon: <TransactionIcon />,
    children: [
      { text: 'All Transactions', icon: <TransactionIcon />, path: '/transactions' },
      { text: 'Pending Transactions', icon: <Notifications />, path: '/transactions/pending' },
      { text: 'Failed Transactions', icon: <AlertIcon />, path: '/transactions/failed' },
      { text: 'Transaction Reports', icon: <Assessment />, path: '/transactions/reports' },
    ],
  },
  {
    text: 'Compliance & KYC',
    icon: <ComplianceIcon />,
    children: [
      { text: 'KYC Dashboard', icon: <VerifiedUser />, path: '/compliance/kyc' },
      { text: 'State Restrictions', icon: <Block />, path: '/compliance/restrictions' },
      { text: 'Audit Logs', icon: <AuditIcon />, path: '/compliance/audit' },
      { text: 'Compliance Reports', icon: <Assessment />, path: '/compliance/reports' },
    ],
  },
  {
    text: 'Analytics & Reports',
    icon: <AnalyticsIcon />,
    children: [
      { text: 'Revenue Analytics', icon: <TrendingUp />, path: '/analytics/revenue' },
      { text: 'User Analytics', icon: <PeopleIcon />, path: '/analytics/users' },
      { text: 'Transaction Analytics', icon: <TransactionIcon />, path: '/analytics/transactions' },
      { text: 'Custom Reports', icon: <Assessment />, path: '/analytics/custom' },
    ],
  },
  {
    text: 'Monitoring & Alerts',
    icon: <AlertIcon />,
    children: [
      { text: 'System Health', icon: <Assessment />, path: '/monitoring/health' },
      { text: 'Alert Management', icon: <Notifications />, path: '/monitoring/alerts' },
      { text: 'Performance Metrics', icon: <TrendingUp />, path: '/monitoring/performance' },
    ],
  },
  {
    text: 'Administration',
    icon: <AdminPanelSettings />,
    children: [
      { text: 'Admin Users', icon: <AdminPanelSettings />, path: '/admin/users' },
      { text: 'System Settings', icon: <SettingsIcon />, path: '/admin/settings' },
      { text: 'Support Tickets', icon: <SupportIcon />, path: '/admin/support' },
    ],
  },
];

export const Sidebar: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [openItems, setOpenItems] = React.useState<string[]>(['User Management']);

  const handleItemClick = (item: SidebarItem) => {
    if (item.children) {
      setOpenItems(prev =>
        prev.includes(item.text)
          ? prev.filter(text => text !== item.text)
          : [...prev, item.text]
      );
    } else if (item.path) {
      navigate(item.path);
    }
  };

  const isActive = (path?: string) => {
    return path ? location.pathname === path : false;
  };

  const renderSidebarItem = (item: SidebarItem, level = 0) => {
    const hasChildren = item.children && item.children.length > 0;
    const isOpen = openItems.includes(item.text);
    const active = isActive(item.path);

    return (
      <React.Fragment key={item.text}>
        <ListItem disablePadding>
          <ListItemButton
            onClick={() => handleItemClick(item)}
            sx={{
              pl: 2 + level * 2,
              backgroundColor: active ? 'rgba(255, 255, 255, 0.1)' : 'transparent',
              '&:hover': {
                backgroundColor: 'rgba(255, 255, 255, 0.05)',
              },
              borderRadius: level === 0 ? '0 25px 25px 0' : '0',
              mr: level === 0 ? 2 : 0,
            }}
          >
            <ListItemIcon sx={{ color: 'white', minWidth: 40 }}>
              {item.icon}
            </ListItemIcon>
            <ListItemText
              primary={item.text}
              primaryTypographyProps={{
                fontSize: level === 0 ? '0.9rem' : '0.85rem',
                fontWeight: active ? 600 : 400,
              }}
            />
            {hasChildren && (
              isOpen ? <ExpandLess /> : <ExpandMore />
            )}
          </ListItemButton>
        </ListItem>
        {hasChildren && (
          <Collapse in={isOpen} timeout="auto" unmountOnExit>
            <List component="div" disablePadding>
              {item.children!.map(child => renderSidebarItem(child, level + 1))}
            </List>
          </Collapse>
        )}
      </React.Fragment>
    );
  };

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* Logo Section */}
      <Box sx={{ p: 3, textAlign: 'center' }}>
        <Typography variant="h5" sx={{ fontWeight: 'bold', color: '#00d4ff' }}>
          LuckXpress
        </Typography>
        <Typography variant="caption" sx={{ color: 'rgba(255, 255, 255, 0.7)' }}>
          Admin Dashboard
        </Typography>
      </Box>
      
      <Divider sx={{ borderColor: 'rgba(255, 255, 255, 0.1)' }} />

      {/* Navigation Items */}
      <Box sx={{ flexGrow: 1, overflowY: 'auto', mt: 1 }}>
        <List>
          {sidebarItems.map(item => renderSidebarItem(item))}
        </List>
      </Box>

      {/* Footer */}
      <Box sx={{ p: 2, textAlign: 'center', borderTop: '1px solid rgba(255, 255, 255, 0.1)' }}>
        <Typography variant="caption" sx={{ color: 'rgba(255, 255, 255, 0.5)' }}>
          v1.0.0 - Admin Panel
        </Typography>
      </Box>
    </Box>
  );
};
