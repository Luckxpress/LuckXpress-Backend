import React from 'react';
import {
  List,
  Datagrid,
  TextField,
  DateField,
  NumberField,
  ReferenceField,
  ChipField,
} from 'react-admin';
import { Chip } from '@mui/material';

const StatusField = ({ record }: any) => {
  const getColor = (status: string) => {
    switch (status) {
      case 'completed': return 'success';
      case 'pending': return 'warning';
      case 'failed': return 'error';
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

export const PaymentList = () => (
  <List title="Payment Management">
    <Datagrid>
      <TextField source="id" />
      <ReferenceField source="userId" reference="users">
        <TextField source="username" />
      </ReferenceField>
      <NumberField source="amount" options={{ style: 'currency', currency: 'USD' }} />
      <TextField source="type" />
      <StatusField source="status" />
      <TextField source="paymentMethod" />
      <DateField source="timestamp" showTime />
    </Datagrid>
  </List>
);
