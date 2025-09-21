// Security Test Suite for Postman
// Add this to Collection > Tests

// Test JWT Token Validation
pm.test("Invalid JWT token is rejected", function () {
    pm.sendRequest({
        url: `${pm.variables.get('BASE_URL')}/api/v1/player/profile`,
        method: 'GET',
        header: {
            'Authorization': 'Bearer invalid_token_12345',
            'Content-Type': 'application/json'
        }
    }, function (err, response) {
        pm.expect(response.code).to.equal(401);
        pm.expect(response.json().error).to.equal("Unauthorized");
    });
});

// Test Rate Limiting
pm.test("Rate limiting works", function () {
    const requests = [];
    const rateLimit = parseInt(pm.environment.get('API_RATE_LIMIT')) || 10;
    
    // Send more requests than the rate limit
    for (let i = 0; i < rateLimit + 5; i++) {
        requests.push(new Promise((resolve) => {
            pm.sendRequest({
                url: `${pm.variables.get('BASE_URL')}/api/v1/public/health`,
                method: 'GET'
            }, function (err, response) {
                resolve({
                    status: response.code,
                    headers: response.headers
                });
            });
        }));
    }
    
    Promise.all(requests).then(responses => {
        const rateLimitedResponses = responses.filter(r => r.status === 429);
        pm.expect(rateLimitedResponses.length).to.be.above(0, "Should have rate limited responses");
    });
});

// Test SQL Injection Protection
pm.test("SQL injection is prevented", function () {
    const maliciousPayloads = [
        "'; DROP TABLE users; --",
        "1' OR '1'='1",
        "admin'/**/OR/**/1=1--"
    ];
    
    maliciousPayloads.forEach(payload => {
        pm.sendRequest({
            url: `${pm.variables.get('BASE_URL')}/api/v1/auth/login`,
            method: 'POST',
            header: {
                'Content-Type': 'application/json'
            },
            body: {
                mode: 'raw',
                raw: JSON.stringify({
                    email: payload,
                    password: "password"
                })
            }
        }, function (err, response) {
            pm.expect(response.code).to.not.equal(200);
            pm.expect(response.json()).to.not.have.property('access_token');
        });
    });
});

// Test XSS Protection
pm.test("XSS payloads are sanitized", function () {
    const xssPayload = "<script>alert('xss')</script>";
    
    pm.sendRequest({
        url: `${pm.variables.get('BASE_URL')}/api/v1/player/profile`,
        method: 'PUT',
        header: {
            'Authorization': `Bearer ${pm.variables.get('ACCESS_TOKEN')}`,
            'Content-Type': 'application/json'
        },
        body: {
            mode: 'raw',
            raw: JSON.stringify({
                first_name: xssPayload,
                last_name: "Test"
            })
        }
    }, function (err, response) {
        if (response.code === 200) {
            const responseBody = response.json();
            pm.expect(responseBody.first_name).to.not.include('<script>');
            pm.expect(responseBody.first_name).to.not.include('alert');
        }
    });
});

// Test CSRF Protection
pm.test("CSRF protection is enabled", function () {
    pm.sendRequest({
        url: `${pm.variables.get('BASE_URL')}/api/v1/player/deposit`,
        method: 'POST',
        header: {
            'Authorization': `Bearer ${pm.variables.get('ACCESS_TOKEN')}`,
            'Content-Type': 'application/json',
            'Origin': 'https://malicious-site.com'
        },
        body: {
            mode: 'raw',
            raw: JSON.stringify({
                amount: "1000.0000",
                payment_method: "CARD",
                payment_token: "tok_fake"
            })
        }
    }, function (err, response) {
        // Should either reject the request or require additional verification
        pm.expect([400, 403, 422]).to.include(response.code);
    });
});

// Test Sensitive Data Exposure
pm.test("Sensitive data is not exposed in responses", function () {
    pm.sendRequest({
        url: `${pm.variables.get('BASE_URL')}/api/v1/player/profile`,
        method: 'GET',
        header: {
            'Authorization': `Bearer ${pm.variables.get('ACCESS_TOKEN')}`
        }
    }, function (err, response) {
        if (response.code === 200) {
            const responseBody = JSON.stringify(response.json());
            
            // Check that sensitive fields are not present
            pm.expect(responseBody).to.not.include('password');
            pm.expect(responseBody).to.not.include('password_hash');
            pm.expect(responseBody).to.not.include('two_factor_secret');
            pm.expect(responseBody).to.not.include('ssn');
            pm.expect(responseBody).to.not.include('tax_id');
        }
    });
});
