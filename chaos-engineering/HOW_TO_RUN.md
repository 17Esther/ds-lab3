# How to Run the Chaos Experiment

Yes! You can simply run:
```bash
cd chaos-engineering
chaos run network-partition-experiment.json
```

## Prerequisites

1. **Install Chaos Toolkit:**
   ```bash
   pip install chaostoolkit chaostoolkit-kubernetes
   ```

2. **Verify your pods are running:**
   ```bash
   kubectl get pods -l app=client-service
   kubectl get pods -l app=backend-service
   ```

## Running the Experiment

### Basic Run:
```bash
cd chaos-engineering
chaos run network-partition-experiment.json
```

### With Verbose Output:
```bash
chaos run network-partition-experiment.json --verbose
```

### Dry Run (Test without executing):
```bash
chaos run network-partition-experiment.json --dry
```

## What Happens

1. **Steady State Check**: Verifies backend-service is accessible
2. **Network Partition**: Blocks traffic from client-service to backend-service for 30 seconds
3. **Automatic Rollback**: Restores network connectivity

## Observing Results

While the experiment runs, watch the logs in another terminal:

```bash
# Get client pod name
CLIENT_POD=$(kubectl get pods -l app=client-service -o jsonpath='{.items[0].metadata.name}')

# Watch logs
kubectl logs -f $CLIENT_POD | grep -E "(Retry|CircuitBreaker|Fallback|Attempting|Response)"
```

## Troubleshooting

### If iptables doesn't work:
Your pod might not have NET_ADMIN capability. You can:

1. **Update your deployment** to add security context:
   ```yaml
   securityContext:
     capabilities:
       add:
       - NET_ADMIN
   ```

2. **Or use the simple version** that handles errors gracefully:
   ```bash
   chaos run network-partition-experiment-simple.json
   ```

### If pod name matching fails:
The experiment uses label selectors. Make sure your pods have:
- `app=client-service` label

Check with:
```bash
kubectl get pods --show-labels
```

### Alternative: Manual Observation
If Chaos Toolkit doesn't work, you can still observe the system manually:

1. **Start watching logs:**
   ```bash
   kubectl logs -f <client-pod-name>
   ```

2. **Manually block traffic** (in another terminal):
   ```bash
   CLIENT_POD=$(kubectl get pods -l app=client-service -o jsonpath='{.items[0].metadata.name}')
   BACKEND_IP=$(kubectl get svc backend-service -o jsonpath='{.spec.clusterIP}')
   kubectl exec $CLIENT_POD -- iptables -A OUTPUT -d $BACKEND_IP -j DROP
   ```

3. **Wait 30 seconds**, then restore:
   ```bash
   kubectl exec $CLIENT_POD -- iptables -D OUTPUT -d $BACKEND_IP -j DROP
   ```

## Expected Output

You should see:
- Retry attempts with exponential backoff
- Circuit breaker state transitions (CLOSED → OPEN)
- Fallback responses when circuit is open
- Automatic recovery when network is restored

## After the Experiment

The experiment automatically:
- ✅ Restores network connectivity (rollback)
- ✅ Reports success/failure
- ✅ Shows what was observed

You can then analyze the logs to see how resilience patterns handled the failure.

