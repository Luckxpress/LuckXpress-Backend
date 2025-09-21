import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  TextField,
  Grid,
  Chip,
  Avatar,
  IconButton,
  Menu,
  MenuItem,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  InputLabel,
  Select,
  SelectChangeEvent,
} from '@mui/material';
import { DataGrid, GridColDef, GridActionsCellItem } from '@mui/x-data-grid';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Block as BlockIcon,
  CheckCircle as ApproveIcon,
  MoreVert as MoreVertIcon,
  Search as SearchIcon,
  FilterList as FilterIcon,
  Download as ExportIcon,
} from '@mui/icons-material';

interface User {
  id: number;
  username: string;
  email: string;
  fullName: string;
  status: 'active' | 'blocked' | 'pending';
  kycStatus: 'verified' | 'pending' | 'rejected' | 'not_submitted';
  registrationDate: string;
  lastLogin: string;
  totalDeposits: number;
  totalWithdrawals: number;
  state: string;
}

// Mock data
const mockUsers: User[] = [
  {
    id: 1,
    username: 'john_doe',
    email: 'john.doe@email.com',
    fullName: 'John Doe',
    status: 'active',
    kycStatus: 'verified',
    registrationDate: '2024-01-15',
    lastLogin: '2024-01-20',
    totalDeposits: 5000,
    totalWithdrawals: 2500,
    state: 'CA',
  },
  {
    id: 2,
    username: 'jane_smith',
    email: 'jane.smith@email.com',
    fullName: 'Jane Smith',
    status: 'active',
    kycStatus: 'pending',
    registrationDate: '2024-01-18',
    lastLogin: '2024-01-21',
    totalDeposits: 3000,
    totalWithdrawals: 1000,
    state: 'NY',
  },
  {
    id: 3,
    username: 'blocked_user',
    email: 'blocked@email.com',
    fullName: 'Blocked User',
    status: 'blocked',
    kycStatus: 'rejected',
    registrationDate: '2024-01-10',
    lastLogin: '2024-01-12',
    totalDeposits: 1000,
    totalWithdrawals: 0,
    state: 'WA',
  },
];

export const UserManagement: React.FC = () => {
  const [users, setUsers] = useState<User[]>(mockUsers);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [kycFilter, setKycFilter] = useState<string>('all');
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const handleStatusChange = (event: SelectChangeEvent) => {
    setStatusFilter(event.target.value);
  };

  const handleKycChange = (event: SelectChangeEvent) => {
    setKycFilter(event.target.value);
  };

  const handleUserAction = (userId: number, action: string) => {
    setUsers(prevUsers =>
      prevUsers.map(user => {
        if (user.id === userId) {
          switch (action) {
            case 'block':
              return { ...user, status: 'blocked' as const };
            case 'unblock':
              return { ...user, status: 'active' as const };
            case 'approve_kyc':
              return { ...user, kycStatus: 'verified' as const };
            case 'reject_kyc':
              return { ...user, kycStatus: 'rejected' as const };
            default:
              return user;
          }
        }
        return user;
      })
    );
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'active':
        return 'success';
      case 'blocked':
        return 'error';
      case 'pending':
        return 'warning';
      default:
        return 'default';
    }
  };

  const getKycStatusColor = (status: string) => {
    switch (status) {
      case 'verified':
        return 'success';
      case 'pending':
        return 'warning';
      case 'rejected':
        return 'error';
      case 'not_submitted':
        return 'default';
      default:
        return 'default';
    }
  };

  const filteredUsers = users.filter(user => {
    const matchesSearch = user.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         user.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         user.fullName.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = statusFilter === 'all' || user.status === statusFilter;
    const matchesKyc = kycFilter === 'all' || user.kycStatus === kycFilter;
    
    return matchesSearch && matchesStatus && matchesKyc;
  });

  const columns: GridColDef[] = [
    {
      field: 'avatar',
      headerName: '',
      width: 60,
      renderCell: (params) => (
        <Avatar sx={{ width: 32, height: 32 }}>
          {params.row.fullName.charAt(0)}
        </Avatar>
      ),
      sortable: false,
      filterable: false,
    },
    {
      field: 'username',
      headerName: 'Username',
      width: 150,
      renderCell: (params) => (
        <Box>
          <Typography variant="body2" fontWeight="bold">
            {params.value}
          </Typography>
          <Typography variant="caption" color="textSecondary">
            ID: {params.row.id}
          </Typography>
        </Box>
      ),
    },
    {
      field: 'fullName',
      headerName: 'Full Name',
      width: 180,
    },
    {
      field: 'email',
      headerName: 'Email',
      width: 200,
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
      field: 'kycStatus',
      headerName: 'KYC Status',
      width: 130,
      renderCell: (params) => (
        <Chip
          label={params.value.replace('_', ' ')}
          color={getKycStatusColor(params.value) as any}
          size="small"
          variant="outlined"
        />
      ),
    },
    {
      field: 'state',
      headerName: 'State',
      width: 80,
      renderCell: (params) => (
        <Chip
          label={params.value}
          size="small"
          color={params.value === 'WA' || params.value === 'ID' ? 'error' : 'default'}
        />
      ),
    },
    {
      field: 'totalDeposits',
      headerName: 'Total Deposits',
      width: 130,
      renderCell: (params) => `$${params.value.toLocaleString()}`,
    },
    {
      field: 'registrationDate',
      headerName: 'Registration',
      width: 120,
      renderCell: (params) => new Date(params.value).toLocaleDateString(),
    },
    {
      field: 'actions',
      type: 'actions',
      headerName: 'Actions',
      width: 120,
      getActions: (params) => [
        <GridActionsCellItem
          icon={<EditIcon />}
          label="Edit"
          onClick={() => {
            setSelectedUser(params.row);
            setDialogOpen(true);
          }}
        />,
        <GridActionsCellItem
          icon={params.row.status === 'blocked' ? <ApproveIcon /> : <BlockIcon />}
          label={params.row.status === 'blocked' ? 'Unblock' : 'Block'}
          onClick={() => handleUserAction(params.row.id, params.row.status === 'blocked' ? 'unblock' : 'block')}
        />,
        <GridActionsCellItem
          icon={<MoreVertIcon />}
          label="More"
          onClick={(event) => {
            setAnchorEl(event.currentTarget as HTMLElement);
            setSelectedUser(params.row);
          }}
        />,
      ],
    },
  ];

  return (
    <Box sx={{ flexGrow: 1 }}>
      {/* Page Header */}
      <Box mb={3}>
        <Typography variant="h4" gutterBottom sx={{ fontWeight: 'bold' }}>
          User Management
        </Typography>
        <Typography variant="body1" color="textSecondary">
          Manage user accounts, KYC verification, and compliance status.
        </Typography>
      </Box>

      {/* Stats Cards */}
      <Grid container spacing={3} mb={3}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Total Users
              </Typography>
              <Typography variant="h4" component="div" sx={{ fontWeight: 'bold' }}>
                {users.length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Active Users
              </Typography>
              <Typography variant="h4" component="div" sx={{ fontWeight: 'bold', color: '#4caf50' }}>
                {users.filter(u => u.status === 'active').length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                KYC Pending
              </Typography>
              <Typography variant="h4" component="div" sx={{ fontWeight: 'bold', color: '#ff9800' }}>
                {users.filter(u => u.kycStatus === 'pending').length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Blocked Users
              </Typography>
              <Typography variant="h4" component="div" sx={{ fontWeight: 'bold', color: '#f44336' }}>
                {users.filter(u => u.status === 'blocked').length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Filters and Actions */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                placeholder="Search users..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                InputProps={{
                  startAdornment: <SearchIcon sx={{ mr: 1, color: 'text.secondary' }} />,
                }}
              />
            </Grid>
            <Grid item xs={12} md={2}>
              <FormControl fullWidth>
                <InputLabel>Status</InputLabel>
                <Select value={statusFilter} onChange={handleStatusChange} label="Status">
                  <MenuItem value="all">All Status</MenuItem>
                  <MenuItem value="active">Active</MenuItem>
                  <MenuItem value="blocked">Blocked</MenuItem>
                  <MenuItem value="pending">Pending</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={2}>
              <FormControl fullWidth>
                <InputLabel>KYC Status</InputLabel>
                <Select value={kycFilter} onChange={handleKycChange} label="KYC Status">
                  <MenuItem value="all">All KYC</MenuItem>
                  <MenuItem value="verified">Verified</MenuItem>
                  <MenuItem value="pending">Pending</MenuItem>
                  <MenuItem value="rejected">Rejected</MenuItem>
                  <MenuItem value="not_submitted">Not Submitted</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={4}>
              <Box display="flex" gap={1}>
                <Button
                  variant="contained"
                  startIcon={<AddIcon />}
                  onClick={() => setDialogOpen(true)}
                >
                  Add User
                </Button>
                <Button
                  variant="outlined"
                  startIcon={<ExportIcon />}
                >
                  Export
                </Button>
                <Button
                  variant="outlined"
                  startIcon={<FilterIcon />}
                >
                  Advanced Filters
                </Button>
              </Box>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Data Grid */}
      <Card>
        <CardContent>
          <DataGrid
            rows={filteredUsers}
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
            }}
          />
        </CardContent>
      </Card>

      {/* Context Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={() => setAnchorEl(null)}
      >
        <MenuItem onClick={() => {
          if (selectedUser?.kycStatus === 'pending') {
            handleUserAction(selectedUser.id, 'approve_kyc');
          }
          setAnchorEl(null);
        }}>
          <ApproveIcon sx={{ mr: 1 }} />
          Approve KYC
        </MenuItem>
        <MenuItem onClick={() => {
          if (selectedUser?.kycStatus === 'pending') {
            handleUserAction(selectedUser.id, 'reject_kyc');
          }
          setAnchorEl(null);
        }}>
          <BlockIcon sx={{ mr: 1 }} />
          Reject KYC
        </MenuItem>
        <MenuItem onClick={() => setAnchorEl(null)}>
          <EditIcon sx={{ mr: 1 }} />
          View Details
        </MenuItem>
      </Menu>

      {/* User Dialog */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          {selectedUser ? 'Edit User' : 'Add New User'}
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Username"
                defaultValue={selectedUser?.username || ''}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Email"
                defaultValue={selectedUser?.email || ''}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Full Name"
                defaultValue={selectedUser?.fullName || ''}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="State"
                defaultValue={selectedUser?.state || ''}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={() => setDialogOpen(false)}>
            {selectedUser ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};
