#!/usr/bin/env python3
"""
Sentry integration script for enhanced monitoring
Run as a sidecar container or scheduled job
"""

import os
import json
import requests
import sentry_sdk
from sentry_sdk import capture_message, capture_exception
from prometheus_client import CollectorRegistry, Gauge, push_to_gateway
from datetime import datetime, timedelta

# Initialize Sentry
sentry_sdk.init(
    dsn=os.getenv("SENTRY_DSN"),
    environment=os.getenv("ENVIRONMENT", "production"),
    traces_sample_rate=0.1,
)

class ComplianceMonitor:
    def __init__(self):
        self.prometheus_gateway = os.getenv("PROMETHEUS_GATEWAY", "localhost:9091")
        self.api_base = os.getenv("API_BASE", "http://localhost:8080")
        self.registry = CollectorRegistry()
        
        # Define metrics
        self.state_violations = Gauge(
            'compliance_state_violations_total',
            'Total state restriction violations',
            ['state'],
            registry=self.registry
        )
        
        self.kyc_queue_size = Gauge(
            'kyc_queue_size',
            'Number of pending KYC verifications',
            registry=self.registry
        )
        
        self.daily_deposits = Gauge(
            'daily_deposit_total',
            'Total deposits today per user',
            ['user_id'],
            registry=self.registry
        )
    
    def check_compliance_violations(self):
        """Check for compliance violations and report to Sentry"""
        try:
            # Check state violations
            response = requests.get(f"{self.api_base}/api/v1/admin/compliance/violations")
            violations = response.json()
            
            for violation in violations:
                if violation['type'] == 'STATE_RESTRICTION':
                    self.state_violations.labels(state=violation['state']).inc()
                    
                    # Critical violation - send to Sentry
                    capture_message(
                        f"State restriction violation: {violation['state']}",
                        level="error",
                        extras={
                            "user_id": violation['user_id'],
                            "state": violation['state'],
                            "timestamp": violation['timestamp']
                        }
                    )
            
            # Check KYC queue
            kyc_response = requests.get(f"{self.api_base}/api/v1/admin/kyc/queue/size")
            queue_size = kyc_response.json()['size']
            self.kyc_queue_size.set(queue_size)
            
            if queue_size > 100:
                capture_message(
                    f"KYC queue backlog: {queue_size} cases pending",
                    level="warning"
                )
            
            # Push metrics to Prometheus
            push_to_gateway(
                self.prometheus_gateway,
                job='compliance_monitor',
                registry=self.registry
            )
            
        except Exception as e:
            capture_exception(e)
            print(f"Error in compliance check: {e}")
    
    def check_financial_integrity(self):
        """Verify financial integrity and ledger balance"""
        try:
            response = requests.get(f"{self.api_base}/api/v1/admin/financial/integrity")
            integrity = response.json()
            
            if not integrity['balanced']:
                # CRITICAL: Ledger imbalance
                capture_message(
                    "CRITICAL: Ledger imbalance detected",
                    level="fatal",
                    extras={
                        "imbalance_amount": integrity['imbalance'],
                        "affected_users": integrity['affected_users']
                    }
                )
                
                # Trigger PagerDuty alert
                self.trigger_pagerduty_alert(
                    "Ledger Imbalance",
                    f"Imbalance of {integrity['imbalance']} detected"
                )
            
        except Exception as e:
            capture_exception(e)
    
    def trigger_pagerduty_alert(self, title, message):
        """Send critical alerts to PagerDuty"""
        pagerduty_key = os.getenv("PAGERDUTY_KEY")
        if pagerduty_key:
            requests.post(
                "https://events.pagerduty.com/v2/enqueue",
                json={
                    "routing_key": pagerduty_key,
                    "event_action": "trigger",
                    "payload": {
                        "summary": title,
                        "source": "luckxpress-monitor",
                        "severity": "critical",
                        "custom_details": {
                            "message": message,
                            "timestamp": datetime.utcnow().isoformat()
                        }
                    }
                }
            )

if __name__ == "__main__":
    monitor = ComplianceMonitor()
    
    # Run checks every minute
    import time
    while True:
        monitor.check_compliance_violations()
        monitor.check_financial_integrity()
        time.sleep(60)
