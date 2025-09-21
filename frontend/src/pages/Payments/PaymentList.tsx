import React from 'react';
import {
  List,
  Datagrid,
  TextField,
  DateField,
  NumberField,
  ReferenceField,
  SelectInput,
  TextInput,
  DateInput,
  ReferenceInput,
  EditButton,
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

const paymentFilters = [
  <TextInput source="q" label="Search" alwaysOn />,
  <ReferenceInput source="userId" label="User" reference="users">
    <SelectInput optionText="username" />
  </ReferenceInput>,
  <SelectInput source="type" choices={[
    { id: 'DEPOSIT', name: 'Deposit' },
    { id: 'WITHDRAWAL', name: 'Withdrawal' },
    { id: 'REFUND', name: 'Refund' },
    { id: 'BONUS', name: 'Bonus' },
  ]} />,
  <SelectInput source="status" choices={[
    { id: 'PENDING', name: 'Pending' },
    { id: 'PROCESSING', name: 'Processing' },
    { id: 'COMPLETED', name: 'Completed' },
    { id: 'FAILED', name: 'Failed' },
    { id: 'CANCELLED', name: 'Cancelled' },
  ]} />,
  <DateInput source="createdAt_gte" label="From Date" />,
  <DateInput source="createdAt_lte" label="To Date" />,
];

export const PaymentList = () => (
  <List title="Payment Management" filters={paymentFilters}>
    <Datagrid rowClick="edit">
      <TextField source="id" />
      <ReferenceField source="user.id" reference="users" label="User">
        <TextField source="username" />
      </ReferenceField>
      <NumberField source="amount" options={{ style: 'currency', currency: 'USD' }} />
      <TextField source="type" />
      <StatusField source="status" />
      <TextField source="paymentMethod" />
      <DateField source="createdAt" label="Date" showTime />
      <EditButton />
    </Datagrid>
  </List>
);
