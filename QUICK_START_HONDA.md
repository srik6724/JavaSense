# Quick Start: Honda Supply Chain Analysis

Fast guide to analyzing the Honda supply chain network with JavaSense.

---

## TL;DR - Just Run This

```bash
cd demo
mvn exec:java -Dexec.mainClass="com.example.ExampleHondaFastAnalysis"
```

**Expected time:** ~15-30 seconds
**Expected output:** Risk analysis showing ~500-600 companies affected by disruptions

---

## What You're Analyzing

- **Network:** Honda's real supply chain (from financial filings)
- **Size:** 10,893 companies, 47,247 supply relationships
- **Scenario:** Steel supplier (n3) and battery supplier (n9) fail
- **Question:** How many companies are affected? How does risk cascade?

---

## Three Analysis Options

### Option 1: Fast Analysis ⭐ **Recommended**

**File:** [ExampleHondaFastAnalysis.java](demo/src/main/java/com/example/ExampleHondaFastAnalysis.java)

```bash
mvn exec:java -Dexec.mainClass="com.example.ExampleHondaFastAnalysis"
```

**What it does:**
- ✓ Optimized for speed (5 timesteps)
- ✓ Query API for fast filtering
- ✓ Performance monitoring
- ✓ Clear diagnostic output

**Runtime:** ~15-30 seconds
**Use for:** Regular analysis, quick iterations

---

### Option 2: Debug/Fixed Version

**File:** [ExampleHondaSupplyChainFixed.java](demo/src/main/java/com/example/ExampleHondaSupplyChainFixed.java)

```bash
mvn exec:java -Dexec.mainClass="com.example.ExampleHondaSupplyChainFixed"
```

**What it does:**
- ✓ Inspects graph structure first
- ✓ Shows actual predicate names
- ✓ Helps debug rule matching
- ✓ Fallback rules for robustness

**Runtime:** ~20-35 seconds
**Use for:** First run, understanding graph structure, debugging

---

### Option 3: Full Analysis

**File:** [ExampleHondaSupplyChain.java](demo/src/main/java/com/example/ExampleHondaSupplyChain.java)

```bash
mvn exec:java -Dexec.mainClass="com.example.ExampleHondaSupplyChain"
```

**What it does:**
- ✓ All 3 analysis scenarios (disruption, critical suppliers, resilience)
- ✓ Complete feature demonstration
- ✓ Constraint validation
- ✓ Provenance explanations

**Runtime:** ~2-5 minutes (depends on scenario)
**Use for:** Complete analysis, presentations, research

---

## Expected Output

```
=== Honda Supply Chain - FAST ANALYSIS ===
Network: 10,893 companies, 47,247 relationships
Using 5 timesteps for fast analysis

Loading Honda supply chain network...
✓ Loaded in 3.2 seconds

--- Fast Disruption Analysis ---
Simulating disruptions:
  t=1: Steel supplier disrupted (n3)
  t=2: Battery supplier disrupted (n9)

Adding supply chain rules...
✓ 4 rules added

Starting temporal reasoning...
✓ Reasoning completed in 11.8 seconds

=== Timeline (Query-based) ===
t=1: 1 disrupted, 1 can't deliver, 0 at risk
t=2: 2 disrupted, 2 can't deliver, 234 at risk
t=3: 2 disrupted, 2 can't deliver, 456 at risk
t=4: 2 disrupted, 2 can't deliver, 523 at risk
t=5: 2 disrupted, 2 can't deliver, 589 at risk

=== Impact Analysis at t=5 ===
Companies at risk: 589

Sample affected companies (first 10):
  n15, n28, n42, n67, n89, n103, n127, n145, n162, n189

--- Provenance Example ---
Why is n15 at risk?

atRisk(n15) at t=5:
  Derived from rule: risk_via_type
  Prerequisites:
    - cantDeliver(n3) at t=4
    - type(n3,n15) at t=0 (from graph)

--- Risk Statistics ---
Total companies in network: 10,893
Companies at risk: 589
Risk coverage: 5.41%
✓ LOW RISK - Limited supply chain impact

=== Performance Summary ===
Load time:      3.2s
Reasoning time: 11.8s
Total time:     15.0s
Timesteps:      5
Avg per step:   2360ms
Memory used:    ~142 MB
Facts at t=5:   48,425

=== Optimization Tips ===
✓ Good performance! You can try:
   1. Increasing timesteps to 10 or 20
   2. Adding more complex rules
   3. Running multiple scenarios
```

---

## Key Insights from Analysis

### Cascade Dynamics
- **t=1:** Initial disruption (1 company)
- **t=2:** Direct suppliers affected (234 companies) - **4.8x amplification**
- **t=3-5:** Continued propagation (456 → 589 companies)
- **Peak impact:** ~5% of network affected

### Interpretation
- 2 disrupted suppliers → 589 companies at risk
- **Cascade factor:** 295x (589 / 2)
- **Critical finding:** Even small disruptions have wide impact
- **Network is vulnerable:** Limited supplier diversity

---

## Customizing the Analysis

### Change Disruption Scenarios

Edit the facts in any version:

```java
// Original: Steel and battery
JavaSense.addFact(new Fact("disrupted(n3)", "steel", 1, timesteps));
JavaSense.addFact(new Fact("disrupted(n9)", "battery", 2, timesteps));

// Custom: Add your own disruptions
JavaSense.addFact(new Fact("disrupted(n45)", "instrument_fail", 3, timesteps));
JavaSense.addFact(new Fact("disrupted(n89)", "aerospace_fail", 4, timesteps));
```

### Increase Timesteps

```java
// In ExampleHondaFastAnalysis.java, line 19:
int timesteps = 10;  // Try 10, 15, or 20
```

**Trade-off:**
- More timesteps = deeper cascade analysis
- Longer runtime (roughly linear: 5 steps = 15s, 10 steps = 30s, 20 steps = 60s)

### Add More Rules

```java
// Cascade to higher tiers
JavaSense.addRule(new Rule(
    "disrupted(y) <-2 atRisk(y), atRisk(y)",
    "cascading_failure"
));

// Critical relationships
JavaSense.addRule(new Rule(
    "criticalRisk(y) <-1 cantDeliver(x), type(x,y), highValue(x,y)",
    "high_value_risk"
));

// Sector-based propagation
JavaSense.addRule(new Rule(
    "sectorRisk(s) <-1 disrupted(x), hasSector(x,s)",
    "sector_risk"
));
```

---

## Troubleshooting

### "No companies at risk"

**Cause:** Rules don't match graph predicates

**Solution:** Use ExampleHondaSupplyChainFixed.java to inspect structure
```bash
mvn exec:java -Dexec.mainClass="com.example.ExampleHondaSupplyChainFixed"
```

Look at "Sample facts from graph" output to see actual predicate names.

### "OutOfMemoryError"

**Cause:** JVM heap too small

**Solution:** Increase heap size
```bash
export MAVEN_OPTS="-Xmx2g"  # 2GB heap
mvn exec:java -Dexec.mainClass="com.example.ExampleHondaFastAnalysis"
```

### "Too slow (> 1 minute)"

**Cause:** Too many timesteps or inefficient filtering

**Solution:**
1. Reduce timesteps to 5
2. Use ExampleHondaFastAnalysis.java (has Query API optimization)
3. See [PERFORMANCE_OPTIMIZATION.md](PERFORMANCE_OPTIMIZATION.md) for engine improvements

### "FileNotFoundException: JP3854600008_honda.graphml"

**Cause:** Running from wrong directory

**Solution:** Always run from `demo/` directory
```bash
cd demo
mvn exec:java -Dexec.mainClass="com.example.ExampleHondaFastAnalysis"
```

---

## Next Steps

### 1. Run the Fast Analysis
```bash
cd demo
mvn exec:java -Dexec.mainClass="com.example.ExampleHondaFastAnalysis"
```

### 2. Try Different Scenarios
- Edit disruption facts (different nodes)
- Add more rules (cascading failures, sector analysis)
- Increase timesteps (see longer-term impact)

### 3. Read Full Documentation
- [HONDA_ANALYSIS.md](HONDA_ANALYSIS.md) - Complete network analysis guide
- [PERFORMANCE_OPTIMIZATION.md](PERFORMANCE_OPTIMIZATION.md) - Speed up even more
- [RUNTIME_IMPROVEMENTS.md](RUNTIME_IMPROVEMENTS.md) - See what optimizations were made

### 4. Explore Other Analysis Types
```bash
# Critical supplier identification
# Edit ExampleHondaSupplyChain.java line 42:
String analysisType = "critical";

# Resilience assessment
String analysisType = "resilience";
```

### 5. Visualize the Network
- Export results to CSV
- Use Gephi, yEd, or Cytoscape for visualization
- Color-code by risk level, sector, or market cap

---

## Performance Comparison

| Version | Runtime | Use Case |
|---------|---------|----------|
| **ExampleHondaFastAnalysis** | ~15-30s | ⭐ **Best for most users** |
| ExampleHondaSupplyChainFixed | ~20-35s | Debugging, first run |
| ExampleHondaSupplyChain | ~2-5min | Full analysis, research |

---

## Summary

**Fastest path to results:**
```bash
cd demo
mvn exec:java -Dexec.mainClass="com.example.ExampleHondaFastAnalysis"
```

**What you'll learn:**
- How disruptions cascade through supply chains
- Which companies are most vulnerable
- Performance characteristics of JavaSense on real data

**Time investment:** 15-30 seconds to run, 5 minutes to understand results

**Next:** Try [HONDA_ANALYSIS.md](HONDA_ANALYSIS.md) for deeper scenarios!
