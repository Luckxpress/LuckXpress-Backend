import React from 'react';
import {
    Create,
    SimpleForm,
    TextInput,
    SelectInput,
    ReferenceInput,
    NumberInput,
    required,
} from 'react-admin';

export const PaymentCreate = () => (
    <Create title="Create Payment">
        <SimpleForm>
            <ReferenceInput source="userId" reference="users" validate={[required()]}>
                <SelectInput optionText="username" />
            </ReferenceInput>
            
            <NumberInput source="amount" label="Amount" validate={[required()]} />
            
            <SelectInput source="type" choices={[
                { id: 'DEPOSIT', name: 'Deposit' },
                { id: 'WITHDRAWAL', name: 'Withdrawal' },
                { id: 'REFUND', name: 'Refund' },
                { id: 'BONUS', name: 'Bonus' },
            ]} validate={[required()]} />
            
            <SelectInput source="status" choices={[
                { id: 'PENDING', name: 'Pending' },
                { id: 'PROCESSING', name: 'Processing' },
                { id: 'COMPLETED', name: 'Completed' },
            ]} defaultValue="PENDING" validate={[required()]} />
            
            <TextInput source="paymentMethod" label="Payment Method" />
            <TextInput source="provider" label="Provider" />
            <TextInput source="notes" label="Notes" multiline fullWidth />
        </SimpleForm>
    </Create>
);
