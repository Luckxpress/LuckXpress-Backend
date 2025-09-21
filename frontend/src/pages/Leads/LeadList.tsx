import React from 'react';
import { 
  List, 
  Datagrid, 
  TextField, 
  DateField, 
  EmailField, 
  SelectInput,
  TextInput,
  DateInput,
  EditButton 
} from 'react-admin';
import { Chip } from '@mui/material';

const StatusField = ({ record }: any) => (
  <Chip 
    label={record.status || 'new'} 
    color="primary" 
    size="small" 
    variant="outlined"
  />
);

const leadFilters = [
  <TextInput source="q" label="Search" alwaysOn />,
  <SelectInput source="status" choices={[
    { id: 'new', name: 'New' },
    { id: 'contacted', name: 'Contacted' },
    { id: 'qualified', name: 'Qualified' },
    { id: 'converted', name: 'Converted' },
    { id: 'lost', name: 'Lost' },
  ]} />,
  <TextInput source="source" label="Lead Source" />,
  <DateInput source="createdAt_gte" label="From Date" />,
  <DateInput source="createdAt_lte" label="To Date" />,
];

export const LeadList = () => (
  <List title="Lead Management" filters={leadFilters}>
    <Datagrid rowClick="edit">
      <TextField source="id" />
      <TextField source="name" />
      <EmailField source="email" />
      <TextField source="phone" />
      <StatusField source="status" />
      <TextField source="source" label="Lead Source" />
      <DateField source="createdAt" showTime />
      <EditButton />
    </Datagrid>
  </List>
);
