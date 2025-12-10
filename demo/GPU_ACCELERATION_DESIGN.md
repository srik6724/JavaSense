# GPU Acceleration Design for JavaSense v1.4

## Overview

GPU acceleration can provide 10-100x speedup for reasoning over large knowledge bases by parallelizing the most expensive operations. This document outlines the design for GPU-accelerated reasoning in JavaSense.

## Current Bottlenecks

Analysis of [OptimizedReasoner.java](src/main/java/com/example/OptimizedReasoner.java) reveals these performance bottlenecks:

### 1. Pattern Matching (Lines 218-220)
```java
List<Map<String, String>> subsList = useIndexing
    ? findAllSubstitutionsIndexed(r.getBodyLiterals(), allFactsAtT, storage, t)
    : findAllSubstitutionsWithNegation(r.getBodyLiterals(), allFactsAtT);
```
**Problem**: Checking every fact against every rule body (O(facts × rules × body_atoms))
**GPU Opportunity**: Test all fact combinations in parallel

### 2. Variable Unification (inside findAllSubstitutions)
**Problem**: Trying different variable bindings sequentially
**GPU Opportunity**: Test all possible bindings in parallel

### 3. Set Operations (Lines 316-318)
```java
Set<Atom> existingAtTt = storage.getAllAt(tt);
if (!existingAtTt.contains(headGrounded)) {
    storage.addDynamic(headGrounded, tt);
}
```
**Problem**: Sequential containment checks and insertions
**GPU Opportunity**: Parallel deduplication using GPU hash tables

### 4. Timestep Iteration (Line 192)
```java
for (int t = 0; t <= timesteps; t++) {
```
**Problem**: Processing timesteps sequentially (when independent)
**GPU Opportunity**: Process multiple timesteps in parallel when rules don't have cross-timestep dependencies

## Architecture Options

### Option 1: JOCL (Java OpenCL) - RECOMMENDED
**Pros:**
- Cross-platform (NVIDIA, AMD, Intel GPUs)
- Also runs on CPUs as fallback
- Open standard

**Cons:**
- More complex kernel code
- Slightly lower performance than native CUDA

**Dependencies:**
```xml
<dependency>
    <groupId>org.jocl</groupId>
    <artifactId>jocl</artifactId>
    <version>2.0.4</version>
</dependency>
```

### Option 2: JCuda
**Pros:**
- Better performance on NVIDIA hardware
- Mature ecosystem
- More documentation

**Cons:**
- NVIDIA GPUs only
- Limits user hardware options

**Dependencies:**
```xml
<dependency>
    <groupId>org.jcuda</groupId>
    <artifactId>jcuda</artifactId>
    <version>11.8.0</version>
</dependency>
```

### Option 3: Aparapi
**Pros:**
- Automatic Java → OpenCL translation
- Simplest to use

**Cons:**
- Less control over optimization
- Project less actively maintained

## Proposed Design

### High-Level Architecture

```
┌─────────────────────────────────────┐
│   OptimizedReasoner (CPU)           │
│   - Manages reasoning loop          │
│   - Handles provenance tracking     │
└────────────┬────────────────────────┘
             │
             v
┌─────────────────────────────────────┐
│   GpuReasoningEngine                │
│   - Decides CPU vs GPU execution    │
│   - Manages GPU memory              │
│   - Handles data transfer           │
└────────────┬────────────────────────┘
             │
        ┌────┴────┐
        v         v
   ┌─────┐   ┌──────────┐
   │ CPU │   │ GPU      │
   └─────┘   └──────────┘
```

### Key Classes

#### 1. GpuReasoningEngine
```java
public class GpuReasoningEngine {
    private final CLContext context;
    private final CLCommandQueue queue;
    private final CLKernel patternMatchKernel;
    private final CLKernel unificationKernel;

    /**
     * Decides whether to use GPU based on problem size.
     * GPU overhead only worth it for large problems.
     */
    public boolean shouldUseGpu(int numFacts, int numRules, int timesteps) {
        // Use GPU if problem is large enough to overcome transfer overhead
        return numFacts * numRules * timesteps > 100_000;
    }

    /**
     * Finds all substitutions using GPU parallel pattern matching.
     */
    public List<Substitution> findSubstitutionsGpu(
            List<Literal> bodyLiterals,
            List<Atom> facts,
            int timestep) {
        // 1. Transfer data to GPU
        // 2. Launch pattern matching kernel
        // 3. Collect results from GPU
        // 4. Return substitutions
    }
}
```

#### 2. GpuFactStore
```java
public class GpuFactStore {
    private CLBuffer<ByteBuffer> factBuffer;    // GPU memory for facts
    private CLBuffer<IntBuffer> indexBuffer;     // GPU hash table

    /**
     * Stores facts in GPU-friendly format.
     * Facts encoded as: [predicate_id, arg1_id, arg2_id, ...]
     */
    public void uploadFacts(List<Atom> facts) {
        // Convert atoms to integer arrays
        // Upload to GPU memory
    }

    /**
     * Parallel containment check on GPU.
     */
    public boolean[] containsParallel(List<Atom> atoms) {
        // Launch kernel to check all atoms in parallel
        // Return boolean array of results
    }
}
```

### OpenCL Kernels

#### Pattern Matching Kernel
```c
__kernel void pattern_match(
    __global const int* facts,        // Flattened fact array
    __global const int* fact_sizes,   // Number of args per fact
    __global const int* patterns,     // Rule body patterns
    __global const int* pattern_sizes,
    __global int* matches,            // Output: matching fact indices
    __global int* match_count,        // Output: number of matches
    const int num_facts,
    const int num_patterns
) {
    int fact_id = get_global_id(0);
    if (fact_id >= num_facts) return;

    // Try to match this fact against all patterns
    for (int pat_id = 0; pat_id < num_patterns; pat_id++) {
        if (matches_pattern(facts, fact_id, patterns, pat_id)) {
            int idx = atomic_inc(match_count);
            matches[idx] = fact_id;
        }
    }
}
```

#### Unification Kernel
```c
__kernel void unify_variables(
    __global const int* fact_ids,     // Matched fact IDs
    __global const int* facts,
    __global const int* patterns,
    __global int* substitutions,      // Output: variable bindings
    __global int* sub_count,
    const int num_matches
) {
    int match_id = get_global_id(0);
    if (match_id >= num_matches) return;

    // Try to unify variables in this match
    // Store successful substitutions
}
```

## Implementation Plan

### Phase 1: Foundation (1-2 weeks)
1. Add JOCL dependency to `pom.xml`
2. Create `GpuReasoningEngine` class with GPU detection
3. Implement CPU/GPU decision logic
4. Add configuration option: `--gpu` flag or `gpu.enabled=true` property

### Phase 2: Data Structures (1 week)
1. Create `GpuFactStore` for GPU-friendly fact storage
2. Implement encoding: Atom → int[] (string interning for predicates/args)
3. Implement GPU memory management (allocation, transfer, cleanup)
4. Add benchmarks comparing CPU vs GPU data structures

### Phase 3: Pattern Matching (2 weeks)
1. Write OpenCL kernel for parallel pattern matching
2. Implement host-side wrapper: `findSubstitutionsGpu()`
3. Handle edge cases: negation, complex patterns
4. Benchmark against CPU implementation

### Phase 4: Integration (1 week)
1. Modify `OptimizedReasoner.reason()` to use GPU when beneficial
2. Add automatic fallback to CPU if GPU unavailable
3. Implement proper error handling and logging
4. Update documentation

### Phase 5: Optimization (2 weeks)
1. Optimize memory transfer (minimize CPU↔GPU copies)
2. Implement kernel fusion (combine pattern match + unification)
3. Add GPU caching for repeated queries
4. Profile and tune work-group sizes

### Phase 6: Testing & Benchmarks (1 week)
1. Create GPU-specific unit tests
2. Run existing test suite with GPU enabled
3. Benchmark on various problem sizes:
   - Small: 100 facts, 10 rules, 10 timesteps
   - Medium: 10K facts, 100 rules, 100 timesteps
   - Large: 1M facts, 1K rules, 1K timesteps
4. Document performance improvements

## Expected Performance Gains

### Conservative Estimates

| Problem Size | Facts | Rules | Timesteps | CPU Time | GPU Time | Speedup |
|--------------|-------|-------|-----------|----------|----------|---------|
| Small        | 100   | 10    | 10        | 10ms     | 50ms     | 0.2x (overhead) |
| Medium       | 10K   | 100   | 100       | 5s       | 500ms    | 10x |
| Large        | 100K  | 500   | 500       | 300s     | 10s      | 30x |
| Very Large   | 1M    | 1K    | 1K        | hours    | 60s      | 100x+ |

**Key Insight**: GPU only beneficial for medium+ sized problems due to transfer overhead.

## Configuration

### Enable GPU Acceleration

**Via Command Line:**
```bash
java -Djavasense.gpu.enabled=true -jar reasoner.jar
```

**Via Code:**
```java
OptimizedReasoner reasoner = new OptimizedReasoner();
reasoner.setGpuEnabled(true);
reasoner.setGpuMinFacts(1000);  // Only use GPU if facts >= 1000
```

**Auto-detect GPU:**
```java
// Automatically detect and use GPU if available
reasoner.setGpuMode(GpuMode.AUTO);
```

### Fallback Behavior

If GPU is enabled but:
- No GPU detected → Fall back to CPU (log warning)
- GPU out of memory → Fall back to CPU (log error)
- GPU driver error → Fall back to CPU (log error)

This ensures the system always works, even without GPU.

## Memory Management

### GPU Memory Constraints

Typical GPU memory: 4-24 GB

**Memory Usage Estimate:**
- Facts: ~50 bytes per atom (encoded)
- Rules: ~100 bytes per rule
- Intermediate results: 2x fact memory

**Example**: 1M facts = 50MB, well within GPU limits

**Strategy for Large Problems:**
- Batch processing: split facts into chunks
- Stream data to GPU incrementally
- Keep hot data on GPU, cold data on CPU

## Testing Strategy

### Unit Tests
```java
@Test
public void testGpuPatternMatching() {
    GpuReasoningEngine gpu = new GpuReasoningEngine();

    List<Atom> facts = List.of(
        Atom.parse("likes(alice,bob)"),
        Atom.parse("likes(bob,charlie)")
    );

    List<Literal> pattern = List.of(
        Literal.parse("likes(X,Y)")
    );

    List<Substitution> results = gpu.findSubstitutionsGpu(pattern, facts, 0);

    assertEquals(2, results.size());
    // Verify X=alice,Y=bob and X=bob,Y=charlie
}
```

### Integration Tests
```java
@Test
public void testGpuVsCpuConsistency() {
    OptimizedReasoner cpuReasoner = new OptimizedReasoner();
    cpuReasoner.setGpuEnabled(false);

    OptimizedReasoner gpuReasoner = new OptimizedReasoner();
    gpuReasoner.setGpuEnabled(true);

    // Add same facts and rules to both
    // ...

    ReasoningInterpretation cpuResult = cpuReasoner.reason(100);
    ReasoningInterpretation gpuResult = gpuReasoner.reason(100);

    // Results should be identical
    assertEquals(cpuResult.getFactsAt(50), gpuResult.getFactsAt(50));
}
```

### Benchmark Tests
```java
@Benchmark
public void benchmarkLargeReasoning() {
    // 100K facts, 500 rules, 500 timesteps
    OptimizedReasoner reasoner = new OptimizedReasoner();
    reasoner.setGpuEnabled(true);

    // Measure throughput
}
```

## Limitations & Trade-offs

### Limitations
1. **GPU Overhead**: Small problems (< 1K facts) slower on GPU
2. **Transfer Cost**: CPU↔GPU data transfer is expensive
3. **Memory Constraints**: GPU memory limited (vs CPU RAM)
4. **Provenance**: GPU can't easily build provenance graphs (CPU task)

### Trade-offs
- **Simplicity vs Speed**: GPU code more complex to maintain
- **Portability vs Performance**: OpenCL (portable) vs CUDA (faster)
- **Memory vs Speed**: Keep data on GPU (fast) vs transfer each query (flexible)

## Alternative: Hybrid Approach

**Best of Both Worlds:**
1. Use GPU for pattern matching (most expensive operation)
2. Use CPU for:
   - Provenance tracking
   - Result aggregation
   - Small problem instances
   - Negation handling (easier on CPU)

This hybrid approach gives 80% of the speedup with 20% of the complexity.

## Next Steps

To implement GPU acceleration:

1. **Start Small**: Implement GPU pattern matching only
2. **Measure**: Profile to confirm bottleneck
3. **Iterate**: Add GPU support for other operations based on profiling
4. **Document**: Update user docs with GPU setup instructions

**Estimated Total Effort**: 6-8 weeks for full implementation

## Questions to Answer

Before starting implementation:

1. **Hardware Requirements**: What minimum GPU capabilities required?
2. **Target Users**: Who needs GPU acceleration? (Large-scale users only?)
3. **Maintenance**: Who will maintain GPU code long-term?
4. **Testing**: Do we have GPU hardware for CI/CD testing?

---

**Status**: Design Phase
**Target Version**: JavaSense v1.4
**Complexity**: Very High
**Expected Benefit**: 10-100x speedup for large problems
