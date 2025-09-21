import React from 'react';
import {
    List,
    Datagrid,
    TextField,
    DateField,
    ReferenceField,
    SelectInput,
    TextInput,
    EditButton,
    ChipField,
} from 'react-admin';

const kycFilters = [
    <TextInput source="q" label="Search" alwaysOn />,
    <ReferenceField source="userId" label="User" reference="users" />,
    <SelectInput source="status" choices={[
        { id: 'PENDING', name: 'Pending' },
        { id: 'APPROVED', name: 'Approved' },
        { id: 'REJECTED', name: 'Rejected' },
    ]} />,
    <SelectInput source="documentType" label="Document Type" choices={[
        { id: 'PASSPORT', name: 'Passport' },
        { id: 'DRIVERS_LICENSE', name: 'Drivers License' },
        { id: 'NATIONAL_ID', name: 'National ID' },
        { id: 'UTILITY_BILL', name: 'Utility Bill' },
    ]} />,
];

export const KYCList = () => (
    <List filters={kycFilters} title="KYC Management">
        <Datagrid rowClick="edit">
            <TextField source="id" />
            <ReferenceField source="user.id" reference="users" label="User">
                <TextField source="username" />
            </ReferenceField>
            <ChipField source="status" />
            <TextField source="documentType" label="Document Type" />
            <DateField source="submittedAt" label="Submitted" showTime />
            <DateField source="reviewedAt" label="Reviewed" showTime />
            <TextField source="reviewedBy" label="Reviewed By" />
            <EditButton />
        </Datagrid>
    </List>
);
