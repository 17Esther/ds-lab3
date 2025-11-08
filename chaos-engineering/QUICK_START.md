# Quick Start Guide: Chaos Experiment Execution

## Prerequisites Check

```bash
# 1. Check if services are deployed
kubectl get pods -l app=client-service
kubectl get pods -l app=backend-service

# 2. Check if Chaos Toolkit is installed
chaos --version || echo "Chaos Toolkit not installed. Install with: pip install chaostoolkit chaostoolkit-kubernetes"

# 3. Verify kubectl access
kubectl cluster-info
```

## Method 1: Using Chaos Toolkit (Recommended)

### Step 1: Install Chaos Toolkit
```bash
pip install chaostoolkit chaostoolkit-kubernetes
```

### Step 2: Run Baseline Observation
```bash
cd chaos-engineering
./observe-system.sh
```

### Step 3: Start Log Monitoring (3 separate terminals)

**Terminal 1 - Client Service:**
```bash
CLIENT_POD=$(kubectl get pods -l app=client-service -o jsonpath='{.items[0].metadata.name}')
kubectl logs -f $CLIENT_POD | grep -E "(Retry|CircuitBreaker|Fallback|Attempting|Response)"
```

**Terminal 2 - All Client Logs:**
```bash
CLIENT_POD=$(kubectl get pods -l app=client-service -o jsonpath='{.items[0].metadata.name}')
kubectl logs -f $CLIENT_POD
```

**Terminal 3 - Pod Status:**
```bash
watch -n 1 kubectl get pods -o wide
```

### Step 4: Execute Chaos Experiment
```bash
cd chaos-engineering
chaos run network-partition-experiment.json
```

### Step 5: Collect Post-Experiment Data
```bash
./observe-system.sh
```

---

## Method 2: Manual Simulation (If Chaos Toolkit doesn't work)

### Step 1: Run Baseline Observation
```bash
cd chaos-engineering
./observe-system.sh
```

### Step 2: Start Log Monitoring (same as Method 1)

### Step 3: Run Manual Partition Script
```bash
cd chaos-engineering
./simulate-partition.sh
```

This script will:
- Block traffic for 30 seconds
- Automatically restore traffic
- Show you what to observe

### Step 4: Collect Post-Experiment Data
```bash
./observe-system.sh
```

---

## Method 3: Using NetworkPolicy (Simplest)

### Step 1: Create NetworkPolicy to Block Traffic
```bash
# Get backend service IP
BACKEND_IP=$(kubectl get svc backend-service -o jsonpath='{.spec.clusterIP}')
CLIENT_POD=$(kubectl get pods -l app=client-service -o jsonpath='{.items[0].metadata.name}')

# Block using iptables (if pod has NET_ADMIN)
kubectl exec $CLIENT_POD -- iptables -A OUTPUT -d $BACKEND_IP -j DROP

# Wait 30 seconds
echo "Network partition active for 30 seconds..."
sleep 30

# Restore
kubectl exec $CLIENT_POD -- iptables -D OUTPUT -d $BACKEND_IP -j DROP
```

---

## What to Observe During Experiment

### 1. Retry Attempts
Look for logs like:
```
Retry 'backendRetry' - Attempt #1 (with exponential backoff + jitter)
Retry 'backendRetry' - Attempt #2 (with exponential backoff + jitter)
...
```

**What to note:**
- Number of retry attempts (should be up to 5)
- Time between retries (should increase exponentially)
- Randomization in delays (jitter)

### 2. Circuit Breaker State Changes
Look for logs like:
```
⚡ CircuitBreaker 'backendCB' changed state: CLOSED -> OPEN
```

**What to note:**
- When circuit opens (after how many failures)
- Wait duration before HALF_OPEN
- Recovery behavior

### 3. Fallback Responses
Look for logs like:
```
Fallback response due to error: ...
```

**What to note:**
- When fallback is triggered
- Response time (should be fast)
- Error messages

### 4. System Health
- Pod status (should remain Running)
- No pod restarts
- Service continues operating

---

## Expected Timeline

```
Time 0s:   Network partition starts
Time 0-1s: First retry attempt fails
Time 1-3s: Second retry (with backoff + jitter)
Time 3-7s: Third retry (longer backoff)
Time 7-15s: Fourth retry (even longer)
Time 15-31s: Fifth retry (longest backoff)
Time ~20s: Circuit breaker opens (after threshold failures)
Time 20-30s: Fast-fail responses with fallback
Time 30s: Network partition ends
Time 30-35s: Circuit breaker goes to HALF_OPEN
Time 35s+: Circuit closes if recovery successful
```

---

## Post-Experiment Analysis

1. **Compare observation files:**
   ```bash
   # Compare before and after observations
   diff observations_*/retry_events.txt
   diff observations_*/circuit_breaker_events.txt
   ```

2. **Fill out analysis template:**
   - Open `ANALYSIS_TEMPLATE.md`
   - Fill in observed metrics
   - Compare with baseline behavior
   - Document findings

3. **Key Questions to Answer:**
   - Did retry work as expected?
   - Did circuit breaker open at the right time?
   - Did fallback prevent complete failure?
   - How did response times compare?
   - Did the system recover automatically?

---

## Troubleshooting

### Chaos Toolkit not working?
- Use Method 2 (Manual Simulation) or Method 3 (NetworkPolicy)

### Can't see logs?
- Check pod names: `kubectl get pods`
- Verify services: `kubectl get svc`

### Network partition not working?
- Check if pod has NET_ADMIN capability
- Try NetworkPolicy method instead
- Verify service IPs: `kubectl get svc`

### Need help?
- Check README.md for detailed instructions
- Review ANALYSIS_TEMPLATE.md for what to look for

