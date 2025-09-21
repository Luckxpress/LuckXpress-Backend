-- LuckXpress Database Initialization Script
-- This script sets up the initial database structure and configuration

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create schema for audit logging
CREATE SCHEMA IF NOT EXISTS audit;

-- Create audit trigger function
CREATE OR REPLACE FUNCTION audit.log_changes()
RETURNS TRIGGER AS $$
BEGIN
    -- Insert audit record for any changes
    IF TG_OP = 'DELETE' THEN
        INSERT INTO audit.audit_log (
            table_name,
            operation,
            old_data,
            changed_by,
            changed_at
        ) VALUES (
            TG_TABLE_NAME,
            TG_OP,
            row_to_json(OLD),
            current_user,
            current_timestamp
        );
        RETURN OLD;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO audit.audit_log (
            table_name,
            operation,
            old_data,
            new_data,
            changed_by,
            changed_at
        ) VALUES (
            TG_TABLE_NAME,
            TG_OP,
            row_to_json(OLD),
            row_to_json(NEW),
            current_user,
            current_timestamp
        );
        RETURN NEW;
    ELSIF TG_OP = 'INSERT' THEN
        INSERT INTO audit.audit_log (
            table_name,
            operation,
            new_data,
            changed_by,
            changed_at
        ) VALUES (
            TG_TABLE_NAME,
            TG_OP,
            row_to_json(NEW),
            current_user,
            current_timestamp
        );
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Create audit log table
CREATE TABLE IF NOT EXISTS audit.audit_log (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL,
    operation VARCHAR(10) NOT NULL,
    old_data JSONB,
    new_data JSONB,
    changed_by VARCHAR(100) NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    correlation_id UUID
);

-- Create indexes for audit log
CREATE INDEX IF NOT EXISTS idx_audit_log_table_name ON audit.audit_log(table_name);
CREATE INDEX IF NOT EXISTS idx_audit_log_changed_at ON audit.audit_log(changed_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_correlation_id ON audit.audit_log(correlation_id);

-- Create compliance-specific tables
CREATE TABLE IF NOT EXISTS state_restrictions (
    id BIGSERIAL PRIMARY KEY,
    state_code CHAR(2) NOT NULL UNIQUE,
    sweeps_allowed BOOLEAN DEFAULT false,
    gold_allowed BOOLEAN DEFAULT true,
    kyc_required BOOLEAN DEFAULT true,
    max_deposit_amount DECIMAL(19,4),
    max_withdrawal_amount DECIMAL(19,4),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insert initial state restrictions
INSERT INTO state_restrictions (state_code, sweeps_allowed, gold_allowed, kyc_required, max_deposit_amount, max_withdrawal_amount)
VALUES 
    ('WA', false, true, true, 1000.0000, 0.0000),
    ('ID', false, true, true, 1000.0000, 0.0000)
ON CONFLICT (state_code) DO NOTHING;

-- Create KYC status lookup table
CREATE TABLE IF NOT EXISTS kyc_status_types (
    id SERIAL PRIMARY KEY,
    status_name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    is_active BOOLEAN DEFAULT true
);

INSERT INTO kyc_status_types (status_name, description)
VALUES 
    ('PENDING', 'KYC verification is pending'),
    ('APPROVED', 'KYC verification approved'),
    ('REJECTED', 'KYC verification rejected'),
    ('EXPIRED', 'KYC verification has expired'),
    ('UNDER_REVIEW', 'KYC verification under manual review')
ON CONFLICT (status_name) DO NOTHING;

-- Create transaction types lookup table
CREATE TABLE IF NOT EXISTS transaction_types (
    id SERIAL PRIMARY KEY,
    type_name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    requires_kyc BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true
);

INSERT INTO transaction_types (type_name, description, requires_kyc)
VALUES 
    ('GOLD_PURCHASE', 'Purchase of Gold Coins', true),
    ('SWEEPS_WITHDRAWAL', 'Withdrawal of Sweeps Coins', true),
    ('BONUS_CREDIT', 'Bonus Sweeps Coins credit', false),
    ('GAME_WIN', 'Winnings from game play', false),
    ('GAME_BET', 'Bet placed in game', false),
    ('REFUND', 'Refund of previous transaction', false)
ON CONFLICT (type_name) DO NOTHING;

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create compliance monitoring view
CREATE OR REPLACE VIEW compliance_summary AS
SELECT 
    sr.state_code,
    sr.sweeps_allowed,
    sr.gold_allowed,
    sr.kyc_required,
    COUNT(CASE WHEN u.state = sr.state_code THEN 1 END) as user_count,
    COUNT(CASE WHEN u.state = sr.state_code AND u.kyc_status = 'APPROVED' THEN 1 END) as approved_users,
    COUNT(CASE WHEN u.state = sr.state_code AND u.kyc_status = 'PENDING' THEN 1 END) as pending_kyc_users
FROM state_restrictions sr
LEFT JOIN users u ON u.state = sr.state_code
GROUP BY sr.state_code, sr.sweeps_allowed, sr.gold_allowed, sr.kyc_required;

-- Grant necessary permissions
GRANT USAGE ON SCHEMA audit TO luckxpress;
GRANT SELECT, INSERT ON audit.audit_log TO luckxpress;
GRANT USAGE, SELECT ON SEQUENCE audit.audit_log_id_seq TO luckxpress;

-- Create database user for read-only reporting
CREATE USER luckxpress_readonly WITH PASSWORD 'readonly123';
GRANT CONNECT ON DATABASE luckxpress TO luckxpress_readonly;
GRANT USAGE ON SCHEMA public TO luckxpress_readonly;
GRANT USAGE ON SCHEMA audit TO luckxpress_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO luckxpress_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA audit TO luckxpress_readonly;

-- Set up default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO luckxpress_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit GRANT SELECT ON TABLES TO luckxpress_readonly;

COMMIT;
