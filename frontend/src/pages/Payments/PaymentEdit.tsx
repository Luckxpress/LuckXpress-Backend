import React from 'react';
import {
    Edit,
    SimpleForm,
    TextInput,
    SelectInput,
    ReferenceField,
    TextField,
    NumberInput,
    DateTimeInput,
    useNotify,
    useRedirect,
} from 'react-admin';
import { Card, CardContent, Typography, Box, Button, Divider } from '@mui/material';

export const PaymentEdit = () => {
    const notify = useNotify();
    const redirect = useRedirect();

    const handleMarkAsComplete = () => {
        notify('Payment marked as complete', { type: 'success' });
        redirect('/payments');
    };

    const handleMarkAsFailed = () => {
        notify('Payment marked as failed', { type: 'warning' });
        redirect('/payments');
    };

    return (
        <Edit title="Payment Details">
            <SimpleForm>
                <Typography variant="h6" gutterBottom>Payment Information</Typography>
                
                <ReferenceField source="user.id" reference="users" label="User">
                    <TextField source="username" />
                </ReferenceField>
                
                <NumberInput source="amount" label="Amount" />
                
                <SelectInput source="type" choices={[
                    { id: 'DEPOSIT', name: 'Deposit' },
                    { id: 'WITHDRAWAL', name: 'Withdrawal' },
                    { id: 'REFUND', name: 'Refund' },
                    { id: 'BONUS', name: 'Bonus' },
                ]} />
                
                <SelectInput source="status" choices={[
                    { id: 'PENDING', name: 'Pending' },
                    { id: 'PROCESSING', name: 'Processing' },
                    { id: 'COMPLETED', name: 'Completed' },
                    { id: 'FAILED', name: 'Failed' },
                    { id: 'CANCELLED', name: 'Cancelled' },
                ]} />
                
                <TextInput source="paymentMethod" label="Payment Method" />
                <TextInput source="provider" label="Provider" />
                <TextInput source="providerTransactionId" label="Provider Transaction ID" />
                
                <Divider sx={{ my: 3 }} />
                
                <Typography variant="h6" gutterBottom>Additional Information</Typography>
                
                <TextInput source="notes" label="Notes" multiline fullWidth />
                <TextInput source="failureReason" label="Failure Reason" multiline fullWidth />
                
                <Box mt={3} display="flex" gap={2}>
                    <Button 
                        variant="contained" 
                        color="success" 
                        onClick={handleMarkAsComplete}
                    >
                        Mark as Complete
                    </Button>
                    <Button 
                        variant="contained" 
                        color="error" 
                        onClick={handleMarkAsFailed}
                    >
                        Mark as Failed
                    </Button>
                </Box>
            </SimpleForm>
        </Edit>
    );
};
