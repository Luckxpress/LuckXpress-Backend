import React from 'react';
import { List, Datagrid, TextField, DateField, EmailField } from 'react-admin';
import { Chip } from '@mui/material';

const StatusField = ({ record }: any) => (
  <Chip 
    label={record.status || 'new'} 
    color="primary" 
    size="small" 
    variant="outlined"
  />
);

export const LeadList = () => (
  <List title="Lead Management">
    <Datagrid>
      <TextField source="id" />
      <TextField source="name" />
      <EmailField source="email" />
      <TextField source="phone" />
      <StatusField source="status" />
      <DateField source="createdAt" showTime />
    </Datagrid>
  </List>
);
