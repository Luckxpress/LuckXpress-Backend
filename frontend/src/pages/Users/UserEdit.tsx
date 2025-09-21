import React from 'react';
import {
  Edit,
  SimpleForm,
  TextInput,
  SelectInput,
  NumberInput,
  DateInput,
  required,
  email,
} from 'react-admin';

const statusChoices = [
  { id: 'active', name: 'Active' },
  { id: 'suspended', name: 'Suspended' },
  { id: 'pending', name: 'Pending' },
];

const kycStatusChoices = [
  { id: 'verified', name: 'Verified' },
  { id: 'pending', name: 'Pending' },
  { id: 'rejected', name: 'Rejected' },
  { id: 'not_submitted', name: 'Not Submitted' },
];

export const UserEdit = () => (
  <Edit>
    <SimpleForm>
      <TextInput source="id" disabled />
      <TextInput source="username" validate={[required()]} />
      <TextInput source="fullName" validate={[required()]} />
      <TextInput source="email" validate={[required(), email()]} />
      <SelectInput source="status" choices={statusChoices} validate={[required()]} />
      <SelectInput source="kycStatus" choices={kycStatusChoices} validate={[required()]} />
      <NumberInput source="goldCoins" />
      <NumberInput source="sweepCoins" />
      <TextInput source="state" />
      <DateInput source="createdAt" disabled />
    </SimpleForm>
  </Edit>
);
