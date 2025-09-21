// Compliance Test Suite for Postman
// Add this to Collection > Tests

// Test State Restrictions
pm.test("Washington state users cannot use Sweeps", function () {
    pm.sendRequest({
        url: `${pm.variables.get('BASE_URL')}/api/v1/player/play-sweeps` ,
        method: 'POST',
        header: {
            'Authorization': `Bearer ${pm.variables.get('ACCESS_TOKEN')}` ,
            'Content-Type': 'application/json'
        },
        body: {
            mode: 'raw',
            raw: JSON.stringify({
                amount: "10.0000",
                game_id: "test-game",
                user_state: "WA"
            })
        }
    }, function (err, response) {
        pm.expect(response.code).to.equal(403);
        pm.expect(response.json().error).to.include("STATE_RESTRICTION");
    });
});

// Test KYC Requirements
pm.test("Cannot withdraw without KYC", function () {
    pm.sendRequest({
        url: `${pm.variables.get('BASE_URL')}/api/v1/player/withdrawal` ,
        method: 'POST',
        header: {
            'Authorization': `Bearer ${pm.variables.get('ACCESS_TOKEN')}` ,
            'Content-Type': 'application/json'
        },
        body: {
            mode: 'raw',
            raw: JSON.stringify({
                amount: "100.0000",
                method: "ACH"
            })
        }
    }, function (err, response) {
        pm.expect(response.code).to.equal(403);
        pm.expect(response.json().error).to.equal("KYC_REQUIRED");
    });
});

// Test Dual Approval Threshold
pm.test("Large transactions require dual approval", function () {
    pm.sendRequest({
        url: `${pm.variables.get('BASE_URL')}/api/v1/admin/wallet/adjust` ,
        method: 'POST',
        header: {
            'Authorization': `Bearer ${pm.variables.get('ADMIN_TOKEN')}` ,
            'Content-Type': 'application/json'
        },
        body: {
            mode: 'raw',
            raw: JSON.stringify({
                user_id: "test-user",
                amount: "1000.0000",
                currency: "SWEEPS",
                reason: "Manual adjustment"
            })
        }
    }, function (err, response) {
        pm.expect(response.code).to.equal(202);
        pm.expect(response.json().status).to.equal("PENDING_APPROVAL");
    });
});
