# Chaos Engineering Experiment Guide

This directory contains Chaos Toolkit experiments to test resilience patterns in the distributed system.

## Prerequisites

1. **Install Chaos Toolkit:**
   ```bash
   pip install chaostoolkit chaostoolkit-kubernetes
   ```

2. **Verify Kubernetes Access:**
   ```bash
   kubectl get pods
   ```

3. **Ensure Services are Deployed:**
   ```bash
   kubectl get pods -l app=client-service
   kubectl get pods -l app=backend-service
   ```

## Experiment: Network Partition

### Overview
This experiment simulates a network partition that prevents communication between ClientService and BackendService pods. This tests how the implemented resilience patterns (retry with exponential backoff, circuit breaker) handle network failures.

### What It Does
1. **Steady State Check**: Verifies both services are healthy before starting
2. **Network Partition**: Blocks network traffic from ClientService to BackendService
3. **Observation Period**: Maintains partition for 30 seconds
4. **Rollback**: Restores network connectivity automatically

### Execution Steps

#### Step 1: Baseline Observation (Before Chaos)
```bash
# Run observation script to capture baseline state
chmod +x observe-system.sh
./observe-system.sh
```

#### Step 2: Start Continuous Log Monitoring
In separate terminal windows:

**Terminal 1 - Client Service Logs:**
```bash
CLIENT_POD=$(kubectl get pods -l app=client-service -o jsonpath='{.items[0].metadata.name}')
kubectl logs -f $CLIENT_POD
```

**Terminal 2 - Backend Service Logs:**
```bash
BACKEND_POD=$(kubectl get pods -l app=backend-service -o jsonpath='{.items[0].metadata.name}')
kubectl logs -f $BACKEND_POD
```

**Terminal 3 - Pod Status Watch:**
```bash
watch -n 2 kubectl get pods -o wide
```

#### Step 3: Execute Chaos Experiment
```bash
# Run the experiment
chaos run network-partition-experiment.json

# Or with verbose output
chaos run network-partition-experiment.json --verbose
```

#### Step 4: Observe System Behavior

**What to Watch For:**

1. **Retry Behavior:**
   - Look for retry attempt logs: `Retry 'backendRetry' - Attempt #X`
   - Observe increasing delays (exponential backoff)
   - Note randomized delays (jitter)

2. **Circuit Breaker State Transitions:**
   - Watch for: `CircuitBreaker 'backendCB' changed state: CLOSED -> OPEN`
   - After wait period: `CLOSED -> HALF_OPEN -> CLOSED` (if recovery) or `HALF_OPEN -> OPEN` (if still failing)

3. **Fallback Responses:**
   - Look for: `Fallback response due to error: ...`
   - These occur when circuit breaker is OPEN

4. **Pod Status:**
   - Pods should remain running (not crash)
   - Check if pods restart or show errors

#### Step 5: Post-Experiment Observation
```bash
# Run observation script again
./observe-system.sh
```

#### Step 6: Compare Results
Compare the observation files before and after the experiment.

## Alternative: Manual Network Partition (if Chaos Toolkit doesn't work)

If Chaos Toolkit network partition doesn't work, you can manually simulate it:

### Option 1: Using NetworkPolicy (Recommended)
```bash
# Create network policy to block traffic
kubectl apply -f - <<EOF
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: block-backend-access
  namespace: default
spec:
  podSelector:
    matchLabels:
      app: client-service
  policyTypes:
  - Egress
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: backend-service
    # Intentionally empty - blocks all traffic to backend-service
EOF

# Wait 30 seconds, then remove
sleep 30
kubectl delete networkpolicy block-backend-access
```

### Option 2: Using iptables (if you have access to pod)
```bash
# Get client service pod name
CLIENT_POD=$(kubectl get pods -l app=client-service -o jsonpath='{.items[0].metadata.name}')

# Block traffic to backend-service
kubectl exec $CLIENT_POD -- iptables -A OUTPUT -d <BACKEND_SERVICE_IP> -j DROP

# Wait 30 seconds
sleep 30

# Restore traffic
kubectl exec $CLIENT_POD -- iptables -D OUTPUT -d <BACKEND_SERVICE_IP> -j DROP
```

## Expected Behavior

### Without Resilience Patterns (Baseline):
- ClientService would block/hang waiting for BackendService
- No automatic recovery
- Errors propagate directly to client
- System becomes unresponsive

### With Resilience Patterns:
1. **Retry Phase:**
   - Automatic retries with exponential backoff (1s, 2s, 4s, 8s, 16s)
   - Jitter prevents synchronized retries
   - Up to 5 attempts before giving up

2. **Circuit Breaker Phase:**
   - After threshold failures, circuit opens
   - Fast-fail responses (no waiting)
   - Fallback responses returned immediately

3. **Recovery Phase:**
   - After wait duration, circuit goes to HALF_OPEN
   - Limited test requests allowed
   - If successful, circuit closes; if not, reopens

## Analysis Template

See `ANALYSIS_TEMPLATE.md` for detailed analysis framework.

## Troubleshooting

### Chaos Toolkit Installation Issues
```bash
# Use virtual environment
python3 -m venv venv
source venv/bin/activate
pip install chaostoolkit chaostoolkit-kubernetes
```

### Permission Issues
```bash
# Ensure kubectl has proper permissions
kubectl auth can-i create networkpolicies
```

### Experiment Fails
- Check if pods are running: `kubectl get pods`
- Verify service names match: `kubectl get svc`
- Check Chaos Toolkit logs: `chaos run --verbose`

