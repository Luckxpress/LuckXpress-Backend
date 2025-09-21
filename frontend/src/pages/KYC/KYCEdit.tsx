import React from 'react';
import {
    Edit,
    SimpleForm,
    TextInput,
    SelectInput,
    ReferenceField,
    TextField,
    Toolbar,
    SaveButton,
    useNotify,
    useRedirect,
    useUpdate,
} from 'react-admin';
import { Card, CardContent, Typography, Box, Button } from '@mui/material';

const KYCEditToolbar = (props: any) => {
    const notify = useNotify();
    const redirect = useRedirect();
    const [update] = useUpdate();

    const handleApprove = () => {
        update('kyc', { id: props.record.id, data: { status: 'APPROVED' } })
            .then(() => {
                notify('KYC Approved', { type: 'success' });
                redirect('/kyc');
            });
    };

    const handleReject = () => {
        const reason = prompt('Please provide a reason for rejection:');
        if (reason) {
            update('kyc', { id: props.record.id, data: { status: 'REJECTED', rejectionReason: reason } })
                .then(() => {
                    notify('KYC Rejected', { type: 'info' });
                    redirect('/kyc');
                });
        }
    };

    return (
        <Toolbar {...props}>
            <Box sx={{ flex: 1, display: 'flex', justifyContent: 'space-between' }}>
                <SaveButton />
                <Box>
                    <Button color="primary" variant="contained" onClick={handleApprove} sx={{ mr: 2 }}>Approve</Button>
                    <Button color="error" variant="contained" onClick={handleReject}>Reject</Button>
                </Box>
            </Box>
        </Toolbar>
    );
};

export const KYCEdit = () => (
    <Edit title="Review KYC Document">
        <SimpleForm toolbar={<KYCEditToolbar />}>
            <Typography variant="h6" gutterBottom>Review KYC Submission</Typography>
            
            <ReferenceField source="user.id" reference="users" label="User">
                <TextField source="username" />
            </ReferenceField>
            
            <SelectInput source="status" choices={[
                { id: 'PENDING', name: 'Pending' },
                { id: 'APPROVED', name: 'Approved' },
                { id: 'REJECTED', name: 'Rejected' },
            ]} />
            
            <TextInput source="documentType" label="Document Type" disabled />
            <TextInput source="filePath" label="Document Path" fullWidth disabled />
            
            <Typography variant="h6" gutterBottom sx={{ mt: 4 }}>Admin Actions</Typography>
            <TextInput source="rejectionReason" label="Rejection Reason" fullWidth multiline />
            <TextInput source="reviewedBy" label="Reviewed By" />

        </SimpleForm>
    </Edit>
);
