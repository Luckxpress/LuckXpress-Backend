import React from 'react';
import {
  List,
  Datagrid,
  TextField,
  DateField,
  ReferenceField,
} from 'react-admin';
import { Chip } from '@mui/material';

const StatusField = ({ record }: any) => {
  const getColor = (status: string) => {
    switch (status) {
      case 'approved': return 'success';
      case 'pending': return 'warning';
      case 'rejected': return 'error';
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

export const KYCList = () => (
  <List title="KYC Management">
    <Datagrid>
      <TextField source="id" />
      <ReferenceField source="userId" reference="users">
        <TextField source="username" />
      </ReferenceField>
      <StatusField source="status" />
      <TextField source="documentType" />
      <DateField source="uploadedAt" showTime />
    </Datagrid>
  </List>
);
