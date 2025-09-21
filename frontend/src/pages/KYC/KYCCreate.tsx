import React from 'react';
import {
    Create,
    SimpleForm,
    ReferenceInput,
    SelectInput,
    TextInput,
} from 'react-admin';

export const KYCCreate = () => (
    <Create title="Create a KYC Record">
        <SimpleForm>
            <ReferenceInput source="userId" reference="users" />
            <SelectInput source="documentType" label="Document Type" choices={[
                { id: 'PASSPORT', name: 'Passport' },
                { id: 'DRIVERS_LICENSE', name: 'Drivers License' },
                { id: 'NATIONAL_ID', name: 'National ID' },
                { id: 'UTILITY_BILL', name: 'Utility Bill' },
            ]} />
            <TextInput source="filePath" label="Document Path" fullWidth />
        </SimpleForm>
    </Create>
);
