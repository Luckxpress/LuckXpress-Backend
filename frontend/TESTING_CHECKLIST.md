# LuckXpress Admin Dashboard Testing Checklist

## 1. METRIC CARDS TESTING
- [ ] Revenue card displays value ($60)
- [ ] Revenue shows green up arrow (+12.3%)
- [ ] Active Users displays count (8)
- [ ] Active Users shows green up arrow (+8.1%)
- [ ] Deposits shows value ($0)
- [ ] Deposits shows red down arrow (-2.4%)
- [ ] Pending Withdrawals shows value ($0)
- [ ] All cards have proper icons
- [ ] Cards are responsive on mobile view

## 2. REVENUE TREND CHART
- [ ] Chart loads without errors
- [ ] 7D/30D/90D toggle buttons work
- [ ] Line chart displays data points
- [ ] Hover shows tooltips with values
- [ ] Chart is responsive to window resize
- [ ] API call successful (check Network tab)

## 3. CONVERSION FUNNEL
- [ ] All 4 stages display (Visitors, Signups, First Deposit, Active Players)
- [ ] Progress bars show correct percentages
- [ ] Numbers are formatted correctly (45,291)
- [ ] Green gradient colors display properly

## 4. LIVE ACTIVITY FEED
- [ ] Initial activities load
- [ ] Status chips show correct colors (green/red/yellow)
- [ ] Timestamps display correctly
- [ ] User emails are partially hidden (u****@gmail.com)
- [ ] List scrolls when more than 5 items
- [ ] WebSocket connection established (check Console)

## 5. PROVIDER STATUS
- [ ] All providers listed (Evolution Gaming, Nuxii, PayPal, Stripe)
- [ ] Status badges show correct colors
- [ ] Uptime percentages display
- [ ] Disrupted status shows yellow badge

## 6. TOP BAR ACTIONS
- [ ] Export CSV button is clickable
- [ ] Maintenance Mode button shows warning color
- [ ] Emergency Stop button shows error color
- [ ] 30 Days dropdown is functional
- [ ] "All Systems Operational" status displays

## 7. SIDEBAR NAVIGATION
- [ ] Dark theme (#2C3E50) applied
- [ ] All 18 menu items visible
- [ ] Badge notifications display (Users: 23, KYC: 13, etc.)
- [ ] Active item highlighted in green
- [ ] Hover state shows lighter background
- [ ] LuckXpress logo displays

## 8. API INTEGRATION
- [ ] GET /api/v1/dashboard/metrics returns 200
- [ ] GET /api/v1/dashboard/revenue-trend returns 200
- [ ] GET /api/v1/dashboard/conversion-funnel returns 200
- [ ] GET /api/v1/dashboard/provider-status returns 200
- [ ] CORS headers properly configured
- [ ] JWT token sent in Authorization header

## 9. WEBSOCKET TESTING
- [ ] WebSocket connects to ws://localhost:8080/ws
- [ ] Activity updates received every 5 seconds
- [ ] No connection errors in console
- [ ] Reconnection works after disconnect

## 10. PERFORMANCE TESTING
- [ ] Dashboard loads in < 3 seconds
- [ ] No memory leaks after 10 minutes
- [ ] Charts render smoothly
- [ ] No console errors or warnings
