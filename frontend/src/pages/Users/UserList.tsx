import React from 'react';
import {
  List,
  Datagrid,
  TextField,
  EmailField,
  DateField,
  ChipField,
  EditButton,
  DeleteButton,
  TopToolbar,
  CreateButton,
  ExportButton,
  FilterButton,
  SearchInput,
} from 'react-admin';
import { Chip } from '@mui/material';

const UserFilters = [
  <SearchInput source="q" alwaysOn />,
];

const UserActions = () => (
  <TopToolbar>
    <FilterButton />
    <CreateButton />
    <ExportButton />
  </TopToolbar>
);

const StatusField = ({ record }: any) => {
  const getColor = (status: string) => {
    switch (status) {
      case 'active': return 'success';
      case 'suspended': return 'error';
      case 'pending': return 'warning';
      default: return 'default';
    }
  };

  return (
    <Chip 
      label={record.status} 
      color={getColor(record.status)} 
      size="small" 
      variant="outlined"
    />
  );
};

const KYCStatusField = ({ record }: any) => {
  const getColor = (status: string) => {
    switch (status) {
      case 'verified': return 'success';
      case 'pending': return 'warning';
      case 'rejected': return 'error';
      default: return 'default';
    }
  };

  return (
    <Chip 
      label={record.kycStatus} 
      color={getColor(record.kycStatus)} 
      size="small" 
      variant="outlined"
    />
  );
};

export const UserList = () => (
  <List
    filters={UserFilters}
    actions={<UserActions />}
    title="User Management"
  >
    <Datagrid rowClick="edit">
      <TextField source="id" />
      <TextField source="username" />
      <TextField source="fullName" />
      <EmailField source="email" />
      <StatusField source="status" />
      <KYCStatusField source="kycStatus" />
      <TextField source="goldCoins" />
      <TextField source="sweepCoins" />
      <TextField source="state" />
      <DateField source="createdAt" />
      <EditButton />
      <DeleteButton />
    </Datagrid>
  </List>
);
