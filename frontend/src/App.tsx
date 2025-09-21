import React from 'react';
import { Admin, Resource } from 'react-admin';
import { createTheme } from '@mui/material/styles';
import CustomLayout from './components/Layout/CustomLayout';
import Dashboard from './pages/Dashboard';
import { authProvider } from './authProvider';
import { dataProvider } from './dataProvider';

// Import page components
import { UserList, UserEdit, UserCreate } from './pages/Users';
import { PaymentList, PaymentEdit, PaymentCreate } from './pages/Payments';
import { KYCList, KYCEdit, KYCCreate } from './pages/KYC';
import { WithdrawalList } from './pages/Withdrawals';
import { LeadList, LeadEdit, LeadCreate } from './pages/Leads';

// Icons
import DashboardIcon from '@mui/icons-material/Dashboard';
import PeopleIcon from '@mui/icons-material/People';
import PaymentIcon from '@mui/icons-material/Payment';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import ContactsIcon from '@mui/icons-material/Contacts';

const theme = createTheme({
  palette: {
    primary: {
      main: '#4CAF50',
    },
    secondary: {
      main: '#2C3E50',
    },
    background: {
      default: '#F5F6FA',
      paper: '#FFFFFF',
    },
    error: {
      main: '#F44336',
    },
    warning: {
      main: '#FF9800',
    },
    success: {
      main: '#4CAF50',
    },
  },
  typography: {
    fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
    h4: {
      fontWeight: 600,
    },
  },
  components: {
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          borderRadius: 6,
        },
      },
    },
  },
});

const App = () => {
  return (
    <Admin
      title="LuckXpress Admin"
      dashboard={Dashboard}
      layout={CustomLayout}
      theme={theme}
      authProvider={authProvider}
      dataProvider={dataProvider}
      requireAuth
    >
      <Resource 
        name="users" 
        list={UserList} 
        edit={UserEdit} 
        create={UserCreate}
        icon={PeopleIcon}
      />
      <Resource 
        name="payments" 
        list={PaymentList}
        edit={PaymentEdit}
        create={PaymentCreate}
        icon={PaymentIcon}
      />
      <Resource 
        name="kyc" 
        list={KYCList} 
        edit={KYCEdit} 
        create={KYCCreate}
        icon={VerifiedUserIcon}
      />
      <Resource 
        name="withdrawals" 
        list={WithdrawalList}
        icon={AccountBalanceWalletIcon}
      />
      <Resource 
        name="leads" 
        list={LeadList}
        edit={LeadEdit}
        create={LeadCreate}
        icon={ContactsIcon}
      />
    </Admin>
  );
};

export default App;
