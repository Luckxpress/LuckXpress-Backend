import React from 'react';
import {
  Create,
  SimpleForm,
  TextInput,
  EmailInput,
  SelectInput,
  NumberInput,
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

export const UserCreate = () => (
  <Create>
    <SimpleForm>
      <TextInput source="username" validate={[required()]} />
      <TextInput source="fullName" validate={[required()]} />
      <EmailInput source="email" validate={[required(), email()]} />
      <SelectInput source="status" choices={statusChoices} validate={[required()]} defaultValue="pending" />
      <SelectInput source="kycStatus" choices={kycStatusChoices} validate={[required()]} defaultValue="not_submitted" />
      <NumberInput source="goldCoins" defaultValue={0} />
      <NumberInput source="sweepCoins" defaultValue={0} />
      <TextInput source="state" />
    </SimpleForm>
  </Create>
);
