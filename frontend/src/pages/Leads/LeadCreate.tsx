import React from 'react';
import {
    Create,
    SimpleForm,
    TextInput,
    SelectInput,
    DateTimeInput,
    required,
    email,
} from 'react-admin';
import { Typography, Divider } from '@mui/material';

export const LeadCreate = () => (
    <Create title="Create Lead">
        <SimpleForm>
            <Typography variant="h6" gutterBottom>Lead Information</Typography>
            
            <TextInput source="name" validate={[required()]} fullWidth />
            <TextInput source="email" validate={[required(), email()]} fullWidth />
            <TextInput source="phone" fullWidth />
            
            <SelectInput source="status" choices={[
                { id: 'new', name: 'New' },
                { id: 'contacted', name: 'Contacted' },
                { id: 'qualified', name: 'Qualified' },
            ]} defaultValue="new" />
            
            <TextInput source="source" label="Lead Source" />
            
            <Divider sx={{ my: 3 }} />
            
            <Typography variant="h6" gutterBottom>Additional Information</Typography>
            
            <TextInput source="notes" multiline fullWidth />
            <TextInput source="campaign" label="Campaign" />
            <TextInput source="referrer" label="Referrer" />
            
            <Divider sx={{ my: 3 }} />
            
            <Typography variant="h6" gutterBottom>Follow-up Information</Typography>
            
            <DateTimeInput source="nextContactDate" label="Next Contact Date" />
            <TextInput source="assignedTo" label="Assigned To" />
            <SelectInput source="priority" choices={[
                { id: 'low', name: 'Low' },
                { id: 'medium', name: 'Medium' },
                { id: 'high', name: 'High' },
            ]} defaultValue="medium" />
        </SimpleForm>
    </Create>
);
