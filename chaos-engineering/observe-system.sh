#!/bin/bash

# Script to observe system behavior during chaos experiment
# This script collects metrics and logs from both services

echo "=========================================="
echo "System Observation Script"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

TIMESTAMP=$(date +"%Y-%m-%d_%H-%M-%S")
OBSERVATION_DIR="observations_${TIMESTAMP}"
mkdir -p "$OBSERVATION_DIR"

echo -e "${GREEN}Starting observation at: $(date)${NC}"
echo "Observation directory: $OBSERVATION_DIR"
echo ""

# Function to get pod names
get_client_pod() {
    kubectl get pods -l app=client-service -o jsonpath='{.items[0].metadata.name}' 2>/dev/null
}

get_backend_pod() {
    kubectl get pods -l app=backend-service -o jsonpath='{.items[0].metadata.name}' 2>/dev/null
}

CLIENT_POD=$(get_client_pod)
BACKEND_POD=$(get_backend_pod)

if [ -z "$CLIENT_POD" ]; then
    echo -e "${RED}ERROR: Client service pod not found!${NC}"
    exit 1
fi

if [ -z "$BACKEND_POD" ]; then
    echo -e "${RED}ERROR: Backend service pod not found!${NC}"
    exit 1
fi

echo -e "${GREEN}Found pods:${NC}"
echo "  Client Service: $CLIENT_POD"
echo "  Backend Service: $BACKEND_POD"
echo ""

# 1. Get pod status
echo -e "${YELLOW}[1/8] Collecting pod status...${NC}"
kubectl get pods -l app=client-service -o wide > "$OBSERVATION_DIR/pod_status_client.txt"
kubectl get pods -l app=backend-service -o wide > "$OBSERVATION_DIR/pod_status_backend.txt"
kubectl get pods -o wide > "$OBSERVATION_DIR/pod_status_all.txt"
echo "✓ Pod status collected"

# 2. Get service endpoints
echo -e "${YELLOW}[2/8] Collecting service endpoints...${NC}"
kubectl get endpoints client-service > "$OBSERVATION_DIR/endpoints_client.txt" 2>&1
kubectl get endpoints backend-service > "$OBSERVATION_DIR/endpoints_backend.txt" 2>&1
echo "✓ Service endpoints collected"

# 3. Get recent logs from client service (last 50 lines)
echo -e "${YELLOW}[3/8] Collecting client service logs (last 50 lines)...${NC}"
kubectl logs "$CLIENT_POD" --tail=50 > "$OBSERVATION_DIR/client_logs_recent.txt" 2>&1
echo "✓ Client logs collected"

# 4. Get recent logs from backend service (last 50 lines)
echo -e "${YELLOW}[4/8] Collecting backend service logs (last 50 lines)...${NC}"
kubectl logs "$BACKEND_POD" --tail=50 > "$OBSERVATION_DIR/backend_logs_recent.txt" 2>&1
echo "✓ Backend logs collected"

# 5. Count retry events in client logs
echo -e "${YELLOW}[5/8] Analyzing retry events...${NC}"
kubectl logs "$CLIENT_POD" | grep -i "retry" | tail -20 > "$OBSERVATION_DIR/retry_events.txt" 2>&1
RETRY_COUNT=$(kubectl logs "$CLIENT_POD" | grep -c "Retry 'backendRetry'" 2>/dev/null || echo "0")
echo "  Retry attempts found: $RETRY_COUNT" > "$OBSERVATION_DIR/retry_summary.txt"
echo "✓ Retry events analyzed"

# 6. Count circuit breaker state transitions
echo -e "${YELLOW}[6/8] Analyzing circuit breaker events...${NC}"
kubectl logs "$CLIENT_POD" | grep -i "circuitbreaker" | tail -20 > "$OBSERVATION_DIR/circuit_breaker_events.txt" 2>&1
CB_TRANSITIONS=$(kubectl logs "$CLIENT_POD" | grep -c "CircuitBreaker 'backendCB' changed state" 2>/dev/null || echo "0")
echo "  Circuit breaker transitions: $CB_TRANSITIONS" > "$OBSERVATION_DIR/circuit_breaker_summary.txt"
echo "✓ Circuit breaker events analyzed"

# 7. Count fallback responses
echo -e "${YELLOW}[7/8] Analyzing fallback responses...${NC}"
kubectl logs "$CLIENT_POD" | grep -i "fallback" | tail -20 > "$OBSERVATION_DIR/fallback_responses.txt" 2>&1
FALLBACK_COUNT=$(kubectl logs "$CLIENT_POD" | grep -c "Fallback response" 2>/dev/null || echo "0")
echo "  Fallback responses: $FALLBACK_COUNT" > "$OBSERVATION_DIR/fallback_summary.txt"
echo "✓ Fallback responses analyzed"

# 8. Get network policies (if any)
echo -e "${YELLOW}[8/8] Checking network policies...${NC}"
kubectl get networkpolicies -o yaml > "$OBSERVATION_DIR/network_policies.yaml" 2>&1
echo "✓ Network policies collected"

# Create summary report
echo ""
echo -e "${GREEN}Creating summary report...${NC}"
cat > "$OBSERVATION_DIR/SUMMARY.txt" << EOF
==========================================
CHAOS EXPERIMENT OBSERVATION SUMMARY
==========================================
Timestamp: $(date)
Observation Directory: $OBSERVATION_DIR

POD INFORMATION:
- Client Service Pod: $CLIENT_POD
- Backend Service Pod: $BACKEND_POD

METRICS COLLECTED:
- Retry Attempts: $RETRY_COUNT
- Circuit Breaker Transitions: $CB_TRANSITIONS
- Fallback Responses: $FALLBACK_COUNT

FILES GENERATED:
1. pod_status_*.txt - Pod status information
2. endpoints_*.txt - Service endpoint information
3. *_logs_recent.txt - Recent service logs
4. retry_events.txt - Retry attempt logs
5. circuit_breaker_events.txt - Circuit breaker state transitions
6. fallback_responses.txt - Fallback response logs
7. network_policies.yaml - Network policy configuration

NEXT STEPS:
1. Review the logs in this directory
2. Analyze retry patterns and delays
3. Observe circuit breaker state transitions
4. Compare behavior with baseline (no resilience patterns)
5. Document findings in analysis template

EOF

cat "$OBSERVATION_DIR/SUMMARY.txt"
echo ""
echo -e "${GREEN}Observation complete!${NC}"
echo -e "All data saved to: ${YELLOW}$OBSERVATION_DIR${NC}"

