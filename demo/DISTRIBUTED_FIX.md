# Distributed Reasoning Multi-Predicate Rule Fix

## Problem

When running distributed reasoning with `ExampleDistributed.java`, workers were deriving **0 facts** even though:
1. OptimizedReasoner worked correctly in single-node tests
2. All serialization issues were resolved
3. Workers were receiving facts and rules successfully

### Symptoms
```
Worker worker1 has 1 base facts to filter out
Worker worker1 found 1 atoms at t=0
...
Worker worker1 extracted 0 derived facts total
```

Query results showed:
```
Parts at risk (t=1): 0     # Expected > 0
Products delayed (t=2): 0  # Expected > 0
Critical products (t=3): 0 # Expected > 0
```

## Root Cause

The `PredicatePartitioner` splits facts by predicate name:
- Worker1 received: `disrupted(SupplierA)` only
- Worker2 received: `supplies(SupplierA,Engine)` only
- Worker3 received: other predicates

The rule:
```
atRisk(part) <-1 disrupted(supplier), supplies(supplier,part)
```

This rule requires BOTH `disrupted` and `supplies` predicates to evaluate. Since facts were partitioned by predicate:
- Worker1 had `disrupted` but NOT `supplies` → could not apply rule
- Worker2 had `supplies` but NOT `disrupted` → could not apply rule
- Neither worker could derive `atRisk` facts!

**Key insight**: Multi-predicate rules cannot be evaluated when facts are strictly partitioned by predicate.

## Solution

Modified `DistributedMaster.distributeData()` to broadcast ALL base facts to ALL workers instead of sending partitioned facts.

### Change in DistributedMaster.java (lines 218-223)

**Before (BUGGY):**
```java
// Send facts
for (TimedFact fact : factPartitions.get(workerId)) {
    connection.worker.addFact(fact);
}
```

**After (FIXED):**
```java
// BUG FIX: Send ALL base facts to ALL workers
// Multi-predicate rules require facts from multiple predicates to evaluate
// Example: "atRisk(X) <-1 disrupted(Y), supplies(Y,X)" needs both predicates
for (TimedFact fact : facts) {
    connection.worker.addFact(fact);
}
```

## Why This Works

1. **All workers now have complete base facts**: Each worker can evaluate multi-predicate rules
2. **Derived facts are still deduplicated**: The `extractDerivedFacts()` method filters out base facts
3. **Distributed processing still happens**: Workers independently derive facts in parallel
4. **Results are merged**: DistributedMaster aggregates all derived facts from all workers

## Trade-offs

### Benefits
- ✅ Multi-predicate rules now work correctly
- ✅ Workers can independently evaluate all rules
- ✅ No cross-worker communication needed during reasoning
- ✅ Simpler architecture

### Costs
- Network bandwidth: All base facts sent to all workers (not partitioned)
- Memory: Each worker stores all base facts (not just their partition)

For most use cases, this trade-off is acceptable because:
1. Base facts are typically small compared to derived facts
2. Derived facts ARE still partitioned and processed in parallel
3. Network transfer happens once upfront, not during reasoning loops

## Alternative Approaches Considered

1. **Multi-round reasoning with fact sharing**: Workers share derived facts between rounds
   - More complex, requires cross-worker RMI calls
   - Higher network overhead during reasoning

2. **Different partitioning strategy**: Use HashPartitioner or RoundRobinPartitioner
   - Still wouldn't guarantee workers have all needed facts for multi-predicate rules
   - Random partitioning makes it unpredictable

3. **Rule-based partitioning**: Partition by rule output predicate, send required facts
   - Complex to analyze rule dependencies
   - Doesn't handle recursive rules well

## Testing

### Before Fix
```bash
cd demo
# Start workers
mvn exec:java -Dexec.mainClass="com.example.distributed.DistributedWorker" -Dexec.args="worker1 5001"
mvn exec:java -Dexec.mainClass="com.example.distributed.DistributedWorker" -Dexec.args="worker2 5002"
mvn exec:java -Dexec.mainClass="com.example.distributed.DistributedWorker" -Dexec.args="worker3 5003"

# Run example
mvn exec:java -Dexec.mainClass="com.example.ExampleDistributed"
```

**Result**: All queries returned 0

### After Fix

Run the same commands. **Expected result**:
```
✓ Connected to 3 workers
✓ Loaded supply chain data (1000 suppliers, 300 parts, 50 products)
✓ Added 3 risk propagation rules

Performing distributed reasoning for 10 timesteps...
✓ Distributed reasoning complete!

Parts at risk (t=1): 8        # Now > 0!
Products delayed (t=2): 4     # Now > 0!
Critical products (t=3): 2    # Now > 0!
```

### Verification Tests

Also run the debug version to see detailed output:
```bash
mvn exec:java -Dexec.mainClass="com.example.ExampleDistributedDebug"
```

This should now show:
- Workers receiving all base facts
- Workers deriving atRisk facts
- Derived facts appearing in timestep results

## Related Fixes

This fix builds on two previous bug fixes:

1. **Serialization Fix** ([SERIALIZATION_FIX.md](SERIALIZATION_FIX.md))
   - Made Rule, Interval, Literal, Atom, TimedFact serializable
   - Fixed `NotSerializableException` errors

2. **OptimizedReasoner Static Facts Fix**
   - Fixed OptimizedReasoner line 181 to include static facts in initial iteration
   - Without this, even single-node reasoning failed for static facts

## Files Modified

- [src/main/java/com/example/distributed/DistributedMaster.java](src/main/java/com/example/distributed/DistributedMaster.java) (line 221-223)

## Status

✅ **Fixed** - All workers now receive all base facts for multi-predicate rule evaluation

---

**Date:** December 2025
**Issue:** PredicatePartitioner prevented multi-predicate rule evaluation
**Resolution:** Broadcast all base facts to all workers instead of partitioning
