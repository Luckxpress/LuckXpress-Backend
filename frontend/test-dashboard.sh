#!/bin/bash

echo "🧪 LuckXpress Dashboard End-to-End Testing"
echo "=========================================="

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Base URLs
BACKEND_URL="http://localhost:8080"
FRONTEND_URL="http://localhost:3000"

# Test Backend APIs
echo -e "\n${YELLOW}1. Testing Backend API Endpoints${NC}"
echo "-----------------------------------"

# Test metrics endpoint
echo -n "Testing /api/v1/dashboard/metrics... "
METRICS_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" $BACKEND_URL/api/v1/dashboard/metrics)
if [ $METRICS_RESPONSE -eq 200 ]; then
    echo -e "${GREEN}✓ PASSED${NC}"
    curl -s $BACKEND_URL/api/v1/dashboard/metrics | python -m json.tool
else
    echo -e "${RED}✗ FAILED (HTTP $METRICS_RESPONSE)${NC}"
fi

# Test revenue trend endpoint
echo -n "Testing /api/v1/dashboard/revenue-trend... "
REVENUE_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" $BACKEND_URL/api/v1/dashboard/revenue-trend)
if [ $REVENUE_RESPONSE -eq 200 ]; then
    echo -e "${GREEN}✓ PASSED${NC}"
else
    echo -e "${RED}✗ FAILED (HTTP $REVENUE_RESPONSE)${NC}"
fi

# Test conversion funnel endpoint
echo -n "Testing /api/v1/dashboard/conversion-funnel... "
FUNNEL_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" $BACKEND_URL/api/v1/dashboard/conversion-funnel)
if [ $FUNNEL_RESPONSE -eq 200 ]; then
    echo -e "${GREEN}✓ PASSED${NC}"
else
    echo -e "${RED}✗ FAILED (HTTP $FUNNEL_RESPONSE)${NC}"
fi

# Test provider status endpoint
echo -n "Testing /api/v1/dashboard/provider-status... "
PROVIDER_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" $BACKEND_URL/api/v1/dashboard/provider-status)
if [ $PROVIDER_RESPONSE -eq 200 ]; then
    echo -e "${GREEN}✓ PASSED${NC}"
else
    echo -e "${RED}✗ FAILED (HTTP $PROVIDER_RESPONSE)${NC}"
fi

# Test Frontend
echo -e "\n${YELLOW}2. Testing Frontend Dashboard${NC}"
echo "-----------------------------------"

echo -n "Testing Frontend availability... "
FRONTEND_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" $FRONTEND_URL)
if [ $FRONTEND_RESPONSE -eq 200 ]; then
    echo -e "${GREEN}✓ PASSED${NC}"
else
    echo -e "${RED}✗ FAILED (HTTP $FRONTEND_RESPONSE)${NC}"
fi

echo -e "\n${YELLOW}3. Component Testing Checklist${NC}"
echo "-----------------------------------"
echo "Please manually verify in browser ($FRONTEND_URL):"
echo "□ Dashboard loads without errors"
echo "□ Metric cards display values"
echo "□ Revenue trend chart renders"
echo "□ Conversion funnel shows data"
echo "□ Activity feed updates"
echo "□ Provider status displays correctly"
echo "□ Sidebar navigation works"
echo "□ Top bar buttons are clickable"

echo -e "\n${GREEN}Testing Complete!${NC}"
