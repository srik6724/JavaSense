# Performance Optimization Guide for Large Graphs

## Problem: Honda Network Taking 2 Minutes for 20 Timesteps

**Network Size:**
- 10,893 nodes (companies)
- 47,247 edges (supply relationships)
- File size: 8.1 MB
- Runtime: ~2 minutes for 20 timesteps

**Goal:** Reduce runtime to under 10 seconds for initial analysis

---

## Optimization Strategy: Layered Approach

### Level 1: Application-Level Optimizations (Immediate)

These can be implemented right now without changing JavaSense core:

#### 1.1 Reduce Timesteps for Testing
```java
// Instead of:
JavaSense.reason(kb, 20);  // 2 minutes

// Use:
JavaSense.reason(kb, 5);   // ~30 seconds
```

**Impact:** 4x faster for initial testing
**Trade-off:** Less temporal depth

#### 1.2 Use Query API Instead of Iteration
```java
// SLOW: Iterate all facts
for (Atom fact : result.getFactsAt(t)) {
    if (fact.getPredicate().equals("atRisk")) {
        // process
    }
}

// FAST: Use Query API
Query.parse("atRisk(x)").atTime(t).execute(result);
```

**Impact:** 2-5x faster for result filtering
**Reason:** Query API can use indexes and skip irrelevant facts

#### 1.3 Filter Graph by Edge Type
```java
// Only process COGS edges (primary supply), ignore CAPEX/SG&A
// Reduces edges from 47K to ~20K
```

**Impact:** 2x faster reasoning
**Implementation:** Pre-filter during graph loading or add edge-type conditions to rules

#### 1.4 Partition the Network
```java
// Analyze subgraphs instead of full network
// Example: Only Japanese companies (ISIN starts with "JP")
// Or: Only steel sector (GICS code 151040)
```

**Impact:** 5-10x faster for 1/5th of the network
**Use case:** Focused analysis on specific sectors or regions

---

### Level 2: Engine Optimizations (Short-term)

These require modifications to JavaSense core but are straightforward:

#### 2.1 Rule Indexing
**Current:** Rules check all facts for every substitution
**Optimized:** Index facts by predicate name

```java
// In Reasoner.java
private Map<String, Set<Atom>> factIndex = new HashMap<>();

// When adding fact:
factIndex.computeIfAbsent(atom.getPredicate(), k -> new HashSet<>()).add(atom);

// When matching rules:
Set<Atom> candidates = factIndex.get(bodyAtom.getPredicate());
// Only check these candidates, not all facts!
```

**Impact:** 10-50x faster rule matching
**Complexity:** Moderate (2-3 hours to implement)

#### 2.2 Sparse Storage for Static Facts
**Current:** Graph facts duplicated at every timestep
**Optimized:** Store static facts once, mark as "always true"

```java
// Mark graph edges as static (don't change over time)
public class TimedFact {
    private boolean isStatic = false;  // New field

    public TimedFact(Atom atom, String id, boolean isStatic) {
        this.atom = atom;
        this.id = id;
        this.isStatic = isStatic;
        this.startTime = 0;
        this.endTime = Integer.MAX_VALUE;
    }
}

// In reasoning: Only copy dynamic facts per timestep
```

**Impact:** 90% memory reduction, 3-5x speed improvement
**Reason:** Graph has 47K edges × 20 timesteps = 940K fact-instances
**After:** 47K static facts + ~100 dynamic facts

#### 2.3 Lazy Fact Computation
**Current:** Compute all derived facts at every timestep
**Optimized:** Only compute facts needed for queries

```java
// Compute on-demand when query is executed
// Cache results for repeated queries
```

**Impact:** 2-10x faster for sparse queries
**Trade-off:** First query is slower, subsequent queries are instant

---

### Level 3: Algorithmic Optimizations (Medium-term)

Fundamental algorithm improvements:

#### 3.1 Semi-Naive Evaluation
**Current:** Re-evaluate all rules at every timestep
**Optimized:** Only evaluate rules affected by new facts

```java
// Track which facts are new at each timestep
Set<Atom> newFacts = /* only facts derived this timestep */;

// Only evaluate rules whose body atoms match newFacts
for (Rule r : rules) {
    if (r.canMatchNewFacts(newFacts)) {
        // Evaluate this rule
    }
}
```

**Impact:** 10-100x faster for large timesteps
**Reason:** At t=10, most facts are unchanged from t=9

#### 3.2 Magic Sets Optimization
Pre-filter facts based on query goals to avoid computing irrelevant facts.

**Impact:** 5-50x faster when only querying specific predicates
**Complexity:** High (research-level implementation)

#### 3.3 Stratified Negation
Organize rules into strata based on dependencies, evaluate in order.

**Impact:** 2-5x faster when using negation
**Benefit:** Also guarantees correctness for negation

---

### Level 4: Parallel Processing (Long-term)

Exploit multi-core CPUs:

#### 4.1 Parallel Rule Evaluation
```java
// Evaluate independent rules in parallel
rules.parallelStream()
    .forEach(rule -> evaluateRule(rule, facts, t));
```

**Impact:** 2-8x faster on multi-core machines
**Challenge:** Thread-safe fact storage

#### 4.2 Timestep Pipelining
```java
// Process timestep t+1 while finalizing timestep t
ExecutorService executor = Executors.newFixedThreadPool(2);
Future<Set<Atom>> nextTimestep = executor.submit(() -> reasonAtTime(t + 1));
finalizeFacts(t);
Set<Atom> facts = nextTimestep.get();
```

**Impact:** 1.5-2x faster
**Benefit:** Overlaps computation and I/O

---

## Recommended Implementation Plan

### Phase 1: Quick Wins (1 day)
1. ✅ Add timestep reduction to ExampleHondaSupplyChainFixed.java (done)
2. ✅ Use Query API for result filtering (done)
3. Add edge-type filtering option
4. Create partition-by-sector example

**Expected result:** 2 minutes → 15-30 seconds

### Phase 2: Engine Improvements (1 week)
1. Implement rule indexing by predicate
2. Add sparse storage for static facts
3. Benchmark and profile

**Expected result:** 15-30 seconds → 2-5 seconds

### Phase 3: Advanced Algorithms (2-4 weeks)
1. Implement semi-naive evaluation
2. Add stratified negation
3. Research magic sets optimization

**Expected result:** 2-5 seconds → 0.5-1 second

### Phase 4: Parallelization (2-4 weeks)
1. Thread-safe fact storage
2. Parallel rule evaluation
3. Timestep pipelining

**Expected result:** 0.5-1 second → 0.1-0.3 seconds

---

## Benchmarking Results (Target)

| Optimization Level | Honda Network (20 timesteps) | 100K Node Network |
|-------------------|------------------------------|-------------------|
| Baseline (current) | 120 seconds | N/A (too slow) |
| Phase 1 (quick wins) | 20 seconds | N/A |
| Phase 2 (engine) | 4 seconds | 60 seconds |
| Phase 3 (algorithms) | 1 second | 10 seconds |
| Phase 4 (parallel) | 0.2 seconds | 2 seconds |

---

## Memory Optimization

### Current Memory Usage
```
Graph facts: 47,247 edges × 20 timesteps = 945,000 atom instances
Derived facts: ~1,000 per timestep × 20 = 20,000 atoms
Total: ~965,000 atoms in memory
Memory per atom: ~100 bytes
Total memory: ~95 MB
```

### Optimized Memory Usage (Sparse Storage)
```
Static graph facts: 47,247 edges (stored once)
Dynamic facts: ~1,000 per timestep × 20 = 20,000 atoms
Total: ~67,000 atoms
Memory: ~6.7 MB (14x reduction!)
```

---

## Code Examples

### Optimization 1: Timestep Reduction
```java
public class ExampleHondaSupplyChainFixed {
    public static void main(String[] args) {
        // OPTIMIZATION 1: Start with 5 timesteps for testing
        int timesteps = 5;  // Not 20!

        Graph kb = Interpretation.loadKnowledgeBase("JP3854600008_honda.graphml");
        runOptimizedAnalysis(kb, timesteps);
    }
}
```

### Optimization 2: Query API Usage
```java
// Replace iteration with Query API
for (int t = 0; t <= timesteps; t++) {
    // FAST: Use queries
    long disrupted = Query.parse("disrupted(x)").atTime(t).execute(result).size();
    long atRisk = Query.parse("atRisk(x)").atTime(t).execute(result).size();

    System.out.println(String.format("t=%d: disrupted=%d, atRisk=%d", t, disrupted, atRisk));
}
```

### Optimization 3: Rule Indexing (Future)
```java
// In Reasoner.java (future implementation)
public class Reasoner {
    private Map<String, Set<Atom>> factIndex = new HashMap<>();

    private void addToIndex(Atom atom, int time) {
        String key = atom.getPredicate();
        factIndex.computeIfAbsent(key, k -> new HashSet<>()).add(atom);
    }

    private Set<Atom> getCandidateFacts(String predicate) {
        return factIndex.getOrDefault(predicate, Collections.emptySet());
    }
}
```

### Optimization 4: Sparse Storage (Future)
```java
// Mark graph edges as static
public static Graph loadKnowledgeBase(String graphmlPath) {
    Graph g = new Graph();
    // ... parse GraphML ...

    for (Edge edge : edges) {
        TimedFact staticFact = new TimedFact(
            edgeAtom,
            edgeId,
            true  // Mark as static (doesn't change over time)
        );
        g.addFact(staticFact);
    }

    return g;
}
```

---

## Profiling Commands

To identify bottlenecks:

```bash
# Run with profiling
java -agentlib:hprof=cpu=samples,depth=10,interval=1 \
  -cp target/javasense-demo.jar \
  com.example.ExampleHondaSupplyChainFixed

# Analyze heap usage
java -Xms512m -Xmx2g -XX:+HeapDumpOnOutOfMemoryError \
  -cp target/javasense-demo.jar \
  com.example.ExampleHondaSupplyChainFixed

# Garbage collection logging
java -Xlog:gc* -XX:+UseG1GC \
  -cp target/javasense-demo.jar \
  com.example.ExampleHondaSupplyChainFixed
```

---

## Trade-offs Summary

| Optimization | Speed Gain | Memory | Complexity | Correctness Impact |
|-------------|-----------|--------|------------|-------------------|
| Reduce timesteps | 4x | Same | Trivial | Less temporal depth |
| Query API | 3x | Same | Low | None |
| Edge filtering | 2x | 50% less | Low | May miss relationships |
| Partitioning | 10x | 90% less | Medium | Only analyzes subset |
| Rule indexing | 20x | 10% more | Medium | None |
| Sparse storage | 5x | 90% less | Medium | None |
| Semi-naive | 50x | Same | High | None |
| Parallelization | 4x | 20% more | High | Must ensure thread-safety |

---

## Next Steps

1. **Run ExampleHondaSupplyChainFixed.java** to see current optimizations in action
2. **Profile the code** to identify actual bottlenecks
3. **Implement Phase 1 optimizations** (edge filtering, partitioning)
4. **Plan Phase 2 engine improvements** (indexing, sparse storage)

---

## Related Files

- [ExampleHondaSupplyChainFixed.java](demo/src/main/java/com/example/ExampleHondaSupplyChainFixed.java) - Optimized Honda analysis
- [ROADMAP.md](ROADMAP.md) - Future feature roadmap
- [HONDA_ANALYSIS.md](HONDA_ANALYSIS.md) - Honda network documentation
