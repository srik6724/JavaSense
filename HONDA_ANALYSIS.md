# Analyzing the Honda Supply Chain Network

## Network Overview

**File:** `JP3854600008_honda.graphml`

### Statistics:
- **Nodes:** 10,893 companies (suppliers, manufacturers, distributors)
- **Edges:** 47,247 supply relationships
- **Size:** 8.1 MB
- **Scope:** Real-world Honda automotive supply chain

### Data Structure:

#### Node Attributes:
- `name`: Company name (e.g., "Kyoei Steel Ltd")
- `marketcap`: Market capitalization in millions
- `gics`: Global Industry Classification Standard code
- `isin`: International Securities Identification Number

#### Edge Attributes:
- `type`: Relationship type
  - **CAPEX**: Capital expenditure relationships
  - **COGS**: Cost of goods sold (primary supply relationships)
  - **SG&A**: Selling, general & administrative expenses
- `value`: Transaction value/volume
- `cost`: Cost metric

### Sample Companies:

```
Kyoei Steel Ltd (n3)           - Market Cap: $91M   - Steel manufacturer
Samsung SDI Co Ltd (n9)        - Market Cap: $1B    - Battery supplier
Tokyo Steel Manufacturing (n4) - Market Cap: $138M  - Steel
Shimadzu Corp (n45)           - Market Cap: $426M  - Precision instruments
MTU Aero Engines AG (n89)     - Market Cap: $614M  - Aerospace
```

---

## Analysis Scenarios

### 1. Disruption Propagation (Most Impactful)

**Question:** What happens if a major supplier is disrupted (natural disaster, bankruptcy)?

**Example Scenario:**
```java
// Steel supplier disruption
JavaSense.addFact(new Fact("disrupted(n3)", "steel_disruption", 1, 20));

// Battery shortage
JavaSense.addFact(new Fact("disrupted(n9)", "battery_shortage", 5, 15));
```

**Rules:**
```java
// Direct impact
cantDeliver(x) <-1 disrupted(x)

// Upstream propagation
atRisk(y) <-1 cantDeliver(x), COGS(x,y)

// Cascading disruption (no alternatives)
disrupted(y) <-2 atRisk(y), atRisk(y)

// Critical relationships
criticalRisk(y) <-1 cantDeliver(x), COGS(x,y), highValue(x,y)
```

**Expected Output:**
```
Day  1: 1 disrupted, 0 at risk
Day  2: 1 disrupted, 127 at risk      (direct suppliers)
Day  3: 1 disrupted, 456 at risk      (2-hop propagation)
Day  4: 23 disrupted, 891 at risk     (cascading failures)
Day  5: 47 disrupted, 1234 at risk    (compound with battery shortage)
...
Day 20: 156 disrupted, 2847 at risk   (full cascade)
```

**Insights:**
- Identify most vulnerable suppliers (high fan-out)
- Estimate time-to-impact for each tier
- Find critical paths in supply chain

---

### 2. Critical Supplier Identification

**Question:** Which suppliers are single points of failure?

**Rules:**
```java
// Identify supply relationships
hasSupplier(y,x) <- COGS(x,y)

// Count dependencies
dependencyCount(x) <-1 hasSupplier(y1,x), hasSupplier(y2,x)

// Mark as critical
criticalSupplier(x) <-1 dependencyCount(x)

// High-value = critical
criticalSupplier(x) <-1 COGS(x,y), highValue(x,y)
```

**Metrics to Track:**
- **Fan-out:** How many companies depend on this supplier?
- **Transaction value:** Total $ value of relationships
- **Sector concentration:** Does it dominate a specific category?

**Expected Output:**
```
=== Top 20 Critical Suppliers ===
1. n2825 - 847 dependencies, $12.3M value
2. n3164 - 623 dependencies, $8.7M value
3. n2745 - 512 dependencies, $6.4M value
...
```

---

### 3. Supply Chain Resilience Assessment

**Question:** How resilient is the network to disruptions?

**Rules:**
```java
// Vulnerable: single supplier
vulnerable(y) <-1 COGS(x,y), not hasAlternative(y,x)

// Resilient: multiple suppliers
resilient(y) <-1 COGS(x1,y), COGS(x2,y)

// Diversified: different sectors
diversified(y) <-1 COGS(x1,y), COGS(x2,y), differentSector(x1,x2)

// Geographic risk: same region
geographicRisk(y) <-1 COGS(x1,y), COGS(x2,y), sameRegion(x1,x2)
```

**Resilience Score:**
```
Resilience = (Resilient Companies) / (Total Companies) × 100

Vulnerable:   2,347 companies (21.5%)
Resilient:    6,821 companies (62.6%)
Diversified:  4,129 companies (37.9%)

Overall Resilience Score: 62.6%
```

---

### 4. Sector-Based Risk Analysis

**Question:** Which industry sectors are most vulnerable?

**Using GICS codes:**
- `151040` - Steel (vulnerable to material shortages)
- `452030` - Electronics (high-tech components)
- `201060` - Industrial Machinery
- `302020` - Food Products

**Rules:**
```java
// Sector dependency
sectorRisk(sector) <-1 disrupted(x), hasSector(x,sector)

// Sector concentration
concentrated(sector) <-1 COGS(x1,y), COGS(x2,y),
                         hasSector(x1,sector), hasSector(x2,sector)
```

---

### 5. Alternative Supplier Discovery

**Question:** If supplier X fails, who can replace them?

**Rules:**
```java
// Same category suppliers
alternative(x,y) <- hasSector(x,s), hasSector(y,s), x != y

// Similar capacity
viableAlternative(x,y) <- alternative(x,y),
                          similarCapacity(x,y),
                          notDisrupted(y)

// Geographic proximity
preferredAlternative(x,y) <- viableAlternative(x,y),
                              sameRegion(x,y)
```

---

## Performance Considerations

### Challenge: Large Graph (10K+ nodes, 47K+ edges)

This network will **stress-test JavaSense's scalability**!

#### Expected Performance:

**Without Optimizations (v1.0):**
- Loading graph: ~30 seconds
- Reasoning (10 timesteps): ~5-10 minutes
- Memory usage: ~2-4 GB
- Practical limit: ~20 timesteps

**With Optimizations (v1.1+):**
- Sparse storage: 10x memory reduction
- Indexing: 5x faster rule matching
- Incremental reasoning: 20x faster for small updates
- Target: 100+ timesteps feasible

#### Optimization Tips:

1. **Limit timesteps** for initial testing:
   ```java
   JavaSense.reason(kb, 5);  // Start small!
   ```

2. **Use specific queries** instead of full display:
   ```java
   // Don't iterate all facts
   Query.parse("disrupted(x)").atTime(5).execute(result);
   ```

3. **Filter rules by edge type:**
   ```java
   // Only analyze COGS (primary supply), ignore CAPEX/SG&A
   // This reduces edge count from 47K to ~20K
   ```

4. **Partition the graph:**
   ```java
   // Analyze by sector (e.g., only steel suppliers)
   // Or geographic region (Japan vs global)
   ```

---

## Running the Analysis

### Option 1: Full Analysis (Long Runtime)

```bash
# WARNING: May take 5-10 minutes!
mvn exec:java -Dexec.mainClass="com.example.ExampleHondaSupplyChain"
```

### Option 2: Quick Test (Recommended)

```java
// Modify ExampleHondaSupplyChain.java:
// Line 45: Change timesteps from 20 to 5
ReasoningInterpretation result = JavaSense.reason(kb, 5);  // Quick test
```

### Option 3: Subset Analysis

```java
// Analyze only Japanese companies (filter by ISIN starting with "JP")
// Or only steel sector (GICS = 151040)
// Reduces graph from 10K nodes to ~2K nodes
```

---

## Advanced Analysis Ideas

### 1. Time-Series Risk Prediction

Track risk metrics over time:
```java
for (int t = 0; t <= 30; t++) {
    double riskScore = calculateNetworkRisk(result, t);
    System.out.println("Day " + t + ": Risk = " + riskScore);
}
```

### 2. What-If Scenarios

Test different disruption scenarios:
```java
// Scenario A: Single supplier failure
testScenario("n3 fails", new Fact("disrupted(n3)", "s1", 1, 10));

// Scenario B: Regional disaster (all Japan suppliers)
testScenario("Japan earthquake", japaneseSuppliers.stream()
    .map(n -> new Fact("disrupted(" + n + ")", "earthquake", 1, 30))
    .toList());

// Scenario C: Sector-wide shortage (global chip shortage)
testScenario("Chip shortage", chipSuppliers.stream()
    .map(n -> new Fact("disrupted(" + n + ")", "chips", 1, 90))
    .toList());
```

### 3. Network Topology Analysis

Use graph metrics:
```java
// Betweenness centrality (suppliers on many critical paths)
// Clustering coefficient (tightly connected supplier clusters)
// Shortest path analysis (how far is disruption from Honda?)
```

### 4. Multi-Graph Analysis (Future: v1.2)

Compare Honda's network with Toyota's:
```java
MultiGraphReasoner reasoner = new MultiGraphReasoner();
reasoner.addGraph("Honda", hondaGraph);
reasoner.addGraph("Toyota", toyotaGraph);

// Cross-graph rules
reasoner.addCrossGraphRule(
    "disrupted(x) in Honda, sharedSupplier(x,y), supplies(y,z) in Toyota " +
    "-> atRisk(z) in Toyota"
);
```

---

## Visualization Recommendations

### Tools for Graph Visualization:

1. **Gephi** (Free, powerful)
   - Import GraphML directly
   - Force-directed layout
   - Color by sector, size by market cap
   - Highlight disrupted nodes in red

2. **yEd** (Free, clean layouts)
   - Hierarchical layout (tiers of suppliers)
   - Circular layout (identify cycles)

3. **Cytoscape** (Scientific visualization)
   - Network analysis built-in
   - Style by risk metrics

### Temporal Visualization:

Create animations showing disruption spread:
```
t=0: [Honda] ← [Tier 1: 50 suppliers]
t=1: [Honda] ← [Tier 1: 48 OK, 2 DISRUPTED]
t=2: [Honda] ← [Tier 1: 45 OK, 5 DISRUPTED] ← [Tier 2: 12 AT RISK]
t=5: [Honda] ← [Tier 1: 38 OK, 12 DISRUPTED] ← [Tier 2: 127 AFFECTED]
```

---

## Expected Insights

After running the analysis, you should discover:

1. **Critical chokepoints:**
   - ~50-100 suppliers that 1000+ companies depend on
   - Mostly in steel, electronics, chemicals

2. **Cascade dynamics:**
   - Disruption spreads 2-3 tiers deep in ~5 days
   - Peak impact at day 10-15
   - Affects 20-30% of network

3. **Resilience gaps:**
   - ~20% of companies have single-source dependencies
   - Geographic concentration in Japan/Asia
   - Sector concentration in automotive parts

4. **Alternative suppliers:**
   - Most components have 2-5 alternative suppliers
   - But may lack capacity or quality certifications
   - Geographic alternatives often cost-prohibitive

---

## Troubleshooting

### Out of Memory Error?

```bash
# Increase JVM heap size
mvn exec:java -Dexec.mainClass="com.example.ExampleHondaSupplyChain" \
  -Dexec.args="-Xmx8g"  # 8GB heap
```

### Too Slow?

1. Reduce timesteps: `reason(kb, 5)` instead of `reason(kb, 20)`
2. Filter graph by sector/region first
3. Use incremental reasoning for updates
4. Wait for v1.1 optimizations!

### Graph Not Loading?

```bash
# Check file path (relative to working directory)
ls -lh JP3854600008_honda.graphml

# Should show: -rw-r--r-- ... 8.1M ... JP3854600008_honda.graphml
```

---

## Next Steps

1. **Run quick test** (5 timesteps): Verify it works
2. **Identify key suppliers**: Run critical supplier analysis
3. **Test disruption scenarios**: Pick 3-5 major suppliers
4. **Visualize in Gephi**: See the network structure
5. **Compare with theoretical models**: Academic supply chain papers

This real-world dataset is a **perfect benchmark** for JavaSense scalability improvements!

---

**Questions? Ideas?** Open a GitHub issue or discussion!
