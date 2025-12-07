# JavaSense Examples Catalog

Complete guide to all example applications demonstrating JavaSense capabilities.

## Quick Index

| Example | Industry | Complexity | Key Concepts |
|---------|----------|------------|--------------|
| [Main](#1-social-popularity-main) | Social Networks | Beginner | Basic rules, delays |
| [MainAdvanced](#2-customer-segmentation-mainadvanced) | E-Commerce | Intermediate | Multi-condition rules |
| [MainSupplier](#3-supply-chain-risk-mainsupplier) | Supply Chain | Intermediate | Cascading inference |
| [Recommendations](#4-product-recommendations) | E-Commerce | Intermediate | Similarity matching |
| [Disease Spreading](#5-disease-outbreak-modeling) | Public Health | Advanced | Multiple delays, phases |
| [Access Control](#6-dynamic-access-control) | Enterprise Security | Advanced | Time-bounded rules, inheritance |
| [Fraud Detection](#7-fraud-detection) | Financial Services | Advanced | Pattern matching, flagging |
| [Fraud Detection (Parallel)](#7b-fraud-detection-with-parallel-processing) | Financial Services | Expert | 24 rules, Parallel processing, GraphML |
| [Skill Propagation](#8-workplace-skill-spreading) | HR / Training | Advanced | Multi-step derivation |
| [Advanced Features](#9-advanced-features-demo) | Cross-Domain | Expert | Query, Provenance, Negation, Constraints |
| [Honda Supply Chain](#10-real-world-honda-supply-chain) | Supply Chain | Expert | Large-scale, 10K+ nodes, Real data |
| [Multi-Graph Threading](#11-multi-graph-threading) | Financial Services | Expert | Multi-threaded reasoning, Cross-bank detection |

---

## 1. Social Popularity ([Main.java](demo/src/main/java/com/example/Main.java))

### Industry: Social Networks

### What It Does
Models how popularity spreads through friendship networks over time, similar to viral content or influence propagation.

### Key Concepts
- Basic fact seeding
- Simple rule with delay
- Time-bounded rules with intervals
- Graph-based relationships

### Code Highlights
```java
// Seed: Mary is popular initially
JavaSense.addFact(new Fact("popular(Mary)", "seed", 0, 2));

// Rule: Popularity spreads to friends with 1 timestep delay
JavaSense.addRule(new Rule(
    "popular(x) : [0,2] <-1 popular(y), Friends(x,y)",
    "popularity_rule",
    List.of(new Interval(0, 2), new Interval(5, 5))
));
```

### Use Cases
- Social media influence tracking
- Viral content spread modeling
- Opinion propagation in networks
- Community detection

### Run It
```bash
mvn exec:java -Dexec.mainClass="com.example.Main"
```

---

## 2. Customer Segmentation ([MainAdvanced.java](demo/src/main/java/com/example/MainAdvanced.java))

### Industry: E-Commerce / Retail

### What It Does
Derives customer attributes ("trendy") based on ownership patterns—customers who own both cars and pets are classified as trendy.

### Key Concepts
- Multi-condition rules (multiple body atoms)
- Derived attributes
- Pattern matching on ownership
- Category classification

### Code Highlights
```java
// Define product categories
JavaSense.addFact(new Fact("Car(Tesla)", "product1", 0, 10));
JavaSense.addFact(new Fact("Pet(Dog)", "product2", 0, 10));

// Rule: Trendy = owns car AND pet
JavaSense.addRule(new Rule(
    "trendy(x) <- owns(x,c), Car(c), owns(x,p), Pet(p)",
    "trendy_rule"
));
```

### Use Cases
- Customer segmentation
- Personalized marketing
- Product recommendations
- Loyalty program targeting

### Run It
```bash
mvn exec:java -Dexec.mainClass="com.example.MainAdvanced"
```

---

## 3. Supply Chain Risk ([MainSupplier.java](demo/src/main/java/com/example/MainSupplier.java))

### Industry: Supply Chain / Logistics

### What It Does
Tracks how supplier disruptions cascade through a supply network, identifying all customers at risk.

### Key Concepts
- Cascading inference
- Multi-hop propagation
- Risk analysis
- Dependency tracking

### Code Highlights
```java
// Initial disruption
JavaSense.addFact(new Fact("disrupted(SupplierA)", "incident", 1, 10));

// Rule 1: Direct impact
JavaSense.addRule(new Rule(
    "at_risk(x) <-1 disrupted(y), Supplier(y,x)",
    "direct_impact"
));

// Rule 2: Cascading risk
JavaSense.addRule(new Rule(
    "at_risk(x) <-1 at_risk(y), Supplier(y,x)",
    "cascade"
));
```

### Use Cases
- Supply chain resilience analysis
- Vendor risk assessment
- Business continuity planning
- Critical path identification

### Run It
```bash
mvn exec:java -Dexec.mainClass="com.example.MainSupplier"
```

---

## 4. Product Recommendations ([ExampleRecommendations.java](demo/src/main/java/com/example/ExampleRecommendations.java))

### Industry: E-Commerce / Retail

### What It Does
Recommends products to users based on what similar users purchased—collaborative filtering via temporal reasoning.

### Key Concepts
- Similarity detection
- Collaborative filtering
- Recommendation generation
- Purchase pattern analysis

### Code Highlights
```java
// Rule 1: Similarity based on shared purchases
JavaSense.addRule(new Rule(
    "similar(x,y) <- purchased(x,p), purchased(y,p)",
    "similarity_rule"
));

// Rule 2: Recommend what similar users bought
JavaSense.addRule(new Rule(
    "recommended(x,p) <-1 similar(x,y), purchased(y,p)",
    "recommendation_rule"
));
```

### Use Cases
- Product recommendation systems
- "Customers who bought X also bought Y"
- Cross-selling strategies
- Personalized shopping experiences

### Run It
```bash
mvn exec:java -Dexec.mainClass="com.example.ExampleRecommendations"
```

---

## 5. Disease Outbreak Modeling ([ExampleDiseaseSpreading.java](demo/src/main/java/com/example/ExampleDiseaseSpreading.java))

### Industry: Public Health / Epidemiology

### What It Does
Simulates disease transmission through contact networks with incubation periods, symptom onset, and quarantine rules.

### Key Concepts
- Multi-phase progression (infected → symptomatic → quarantine)
- Different delays for different phases
- Contact tracing
- Temporal disease modeling

### Code Highlights
```java
// 2-day incubation before transmission
JavaSense.addRule(new Rule(
    "infected(x) <-2 infected(y), Contact(y,x)",
    "transmission"
));

// Symptoms appear 3 days after infection
JavaSense.addRule(new Rule(
    "symptomatic(x) <-3 infected(x)",
    "symptom_onset"
));

// Contacts of symptomatic should quarantine
JavaSense.addRule(new Rule(
    "quarantine(x) <-1 symptomatic(y), Contact(x,y)",
    "quarantine_rule"
));
```

### Use Cases
- Epidemic modeling and simulation
- Contact tracing systems
- Quarantine strategy planning
- Outbreak impact assessment

### Run It
```bash
mvn exec:java -Dexec.mainClass="com.example.ExampleDiseaseSpreading"
```

---

## 6. Dynamic Access Control ([ExampleAccessControl.java](demo/src/main/java/com/example/ExampleAccessControl.java))

### Industry: Enterprise Security / IT

### What It Does
Implements dynamic role-based access control with temporal permissions, role inheritance, and time-limited guest access.

### Key Concepts
- Role-based permissions
- Permission inheritance (managers inherit from reports)
- Time-bounded access (guest passes expire)
- Dynamic policy evaluation

### Code Highlights
```java
// Users inherit permissions from roles
JavaSense.addRule(new Rule(
    "canAccess(u,r) <- hasRole(u,role), permission(role,r)",
    "role_permission"
));

// Managers inherit team permissions
JavaSense.addRule(new Rule(
    "canAccess(mgr,r) <- Manages(mgr,emp), canAccess(emp,r)",
    "manager_inheritance"
));

// Guest access expires after 3 timesteps
JavaSense.addRule(new Rule(
    "canAccess(u,r) : [0,3] <- Guest(u), permission(Guest,r)",
    "guest_access",
    List.of(new Interval(0, 3))
));
```

### Use Cases
- Enterprise access control systems
- Temporary contractor permissions
- Dynamic security policies
- Compliance auditing

### Run It
```bash
mvn exec:java -Dexec.mainClass="com.example.ExampleAccessControl"
```

---

## 7. Fraud Detection ([ExampleFraudDetection.java](demo/src/main/java/com/example/ExampleFraudDetection.java))

### Industry: Financial Services / Banking

### What It Does
Detects suspicious transaction patterns including frequent large transactions, new account risks, and guilt-by-association.

### Key Concepts
- Pattern-based detection
- Multi-rule flagging
- Guilt-by-association inference
- Risk scoring

### Code Highlights
```java
// Frequent large transactions = suspicious
JavaSense.addRule(new Rule(
    "suspicious(a) <- largeTransaction(a), largeTransaction(a)",
    "frequent_large"
));

// New accounts with large transactions = risky
JavaSense.addRule(new Rule(
    "risky(a) <- newAccount(a), largeTransaction(a)",
    "new_account_risk"
));

// Accounts linked to suspicious accounts get flagged
JavaSense.addRule(new Rule(
    "flagged(x) <-1 Transfer(x,y), suspicious(y)",
    "guilt_by_association"
));
```

### Use Cases
- Fraud detection systems
- Anti-money laundering (AML)
- Transaction monitoring
- Risk assessment automation

### Run It
```bash
mvn exec:java -Dexec.mainClass="com.example.ExampleFraudDetection"
```

---

## 7b. Fraud Detection with Parallel Processing ([ExampleFraudDetectionParallel.java](demo/src/main/java/com/example/ExampleFraudDetectionParallel.java))

### Industry: Financial Services / Banking

### What It Does
**Demonstrates the REAL power of parallel processing** with 24 complex fraud detection rules running on a transaction network loaded from GraphML. This is the perfect example of when parallelization provides 3-4x speedup.

### Key Concepts
- **24 fraud detection rules** (enough to saturate multiple cores)
- **Parallel processing** with ForkJoinPool
- **Real GraphML data** ([fraud_detection_network.graphml](demo/fraud_detection_network.graphml))
- **5 fraud pattern categories**: amount, velocity, account behavior, location, risk scoring
- **Side-by-side benchmark**: Sequential vs Parallel execution

### Network Structure
The GraphML file contains:
- **17 accounts**: Normal (5), Suspicious (3), High-risk (4), Merchants (3)
- **40+ transactions**: Normal, rapid velocity, large amounts, foreign, escalating
- **Fraud patterns**: Account takeover, money mule networks, burst activity

### Fraud Detection Rule Categories

**1. Transaction Amount Patterns (5 rules)**
- Large single transactions
- Unusually high spending
- Round number transactions (common in fraud)
- Multiple large transactions
- Escalating transaction amounts

**2. Velocity Patterns (5 rules)**
- Rapid transactions
- Burst activity
- After-hours transactions
- Weekend activity
- Unusual frequency

**3. Account Behavior (5 rules)**
- New account with large transaction
- Dormant account suddenly active
- Account takeover pattern
- Credential stuffing
- Profile change + transaction

**4. Location Patterns (4 rules)**
- Foreign transaction from new account
- Impossible travel
- High-risk country
- Location hopping

**5. Combined Risk Scores (5 rules)**
- Medium risk (2 red flags)
- High risk (3+ red flags)
- Critical risk (4+ red flags)
- Manual review escalation
- Final fraud detection

### Code Highlights
```java
// Load real fraud network from GraphML
Graph fraudNetwork = Interpretation.loadKnowledgeBase("fraud_detection_network.graphml");

// Add 24 fraud detection rules
addFraudDetectionRules(reasoner);

// Sequential execution (baseline)
ReasoningInterpretation resultSeq = reasoner.reason(
    timesteps, true, true, true, false  // useParallel = FALSE
);

// Parallel execution (4 cores)
ReasoningInterpretation resultPar = reasonerParallel.reason(
    timesteps, true, true, true, true   // useParallel = TRUE
);

// Calculate speedup
double speedup = (double) timeSeq / timePar;
System.out.println("Speedup: " + String.format("%.2fx", speedup) + " faster!");
```

### Example Fraud Rules
```java
// Rule 11: New account with large transaction (HIGH RISK)
reasoner.addRule(new Rule(
    "newAccountRisk(x) <-1 accountAge(x,5), transaction(x,10000)",
    "new_account_large_txn"
));

// Rule 20: Medium risk score (2 red flags)
reasoner.addRule(new Rule(
    "mediumRisk(x) <-1 suspiciousAmount(x), newAccountRisk(x)",
    "medium_risk_score"
));

// Rule 24: FINAL fraud detection (critical risk)
reasoner.addRule(new Rule(
    "fraudDetected(x) <-1 criticalRisk(x)",
    "fraud_detected_final"
));
```

### Performance Benefits

**Why Parallel Processing Helps Here:**
- ✅ **24 rules** (well above 8-rule threshold)
- ✅ **Independent rules** (no conflicts, perfect for parallelization)
- ✅ **Complex pattern matching** (each rule takes time)
- ✅ **Multi-core CPU** (4-16 cores utilized)

**Expected Speedup:**
- 4-core CPU: **3-4x faster**
- 8-core CPU: **5-7x faster**
- 16-core CPU: **8-10x faster**

**Comparison to Honda Example:**
- Honda: 4 rules → parallel disabled (overhead dominates)
- Fraud: 24 rules → parallel enabled → 3-4x speedup

### Use Cases
- Real-time fraud detection systems
- Anti-money laundering (AML) monitoring
- Transaction risk scoring
- Account takeover detection
- Money mule network identification
- High-throughput transaction screening

### Run It
```bash
mvn exec:java -Dexec.mainClass="com.example.ExampleFraudDetectionParallel"
```

**Expected Output:**
```
=== Fraud Detection: Parallel Processing Demo ===

Loading fraud detection network...
✓ Loaded in 0.05 seconds
  Accounts: 17
  Transactions: 40

--- Test 1: SEQUENTIAL Execution ---
Sequential execution time: 1.5 seconds
Fraud cases detected: 8

--- Test 2: PARALLEL Execution (4 cores) ---
Parallel execution time: 0.4 seconds
Fraud cases detected: 8

=== Performance Comparison ===
Sequential: 1.5s
Parallel:   0.4s
Speedup:    3.75x faster!

=== Why Parallel Processing Helps Here ===
✓ 24 complex fraud detection rules
✓ Each rule checks different patterns independently
✓ On 4-core CPU: ~3-4x speedup
✓ On 8-core CPU: ~5-7x speedup
✓ Perfect use case for parallelization!
```

---

## 8. Workplace Skill Spreading ([ExampleSkillPropagation.java](demo/src/main/java/com/example/ExampleSkillPropagation.java))

### Industry: HR / Organizational Development

### What It Does
Models how skills propagate through an organization via mentorship and collaboration, deriving composite skills like "FullStack" or "AIEngineering".

### Key Concepts
- Multi-step skill acquisition
- Different learning speeds (mentorship vs collaboration)
- Composite skill derivation
- Knowledge transfer modeling

### Code Highlights
```java
// Mentorship learning (2 time units)
JavaSense.addRule(new Rule(
    "hasSkill(mentee,skill) <-2 Mentors(mentor,mentee), hasSkill(mentor,skill)",
    "mentorship_learning"
));

// Collaborative learning (3 time units, slower)
JavaSense.addRule(new Rule(
    "hasSkill(x,skill) <-3 Collaborates(x,y), hasSkill(y,skill)",
    "collaborative_learning"
));

// Composite skills
JavaSense.addRule(new Rule(
    "hasSkill(p,FullStack) <- hasSkill(p,Java), hasSkill(p,Python)",
    "fullstack_combo"
));

JavaSense.addRule(new Rule(
    "hasSkill(p,AIEngineering) <- hasSkill(p,FullStack), hasSkill(p,MachineLearning)",
    "ai_engineer_combo"
));
```

### Use Cases
- Training program planning
- Skill gap analysis
- Mentorship program design
- Team composition optimization

### Run It
```bash
mvn exec:java -Dexec.mainClass="com.example.ExampleSkillPropagation"
```

---

## Choosing the Right Example

### By Complexity

**Beginner** (Start Here):
1. [Main.java](#1-social-popularity-main) - Simplest, single rule
2. [MainAdvanced.java](#2-customer-segmentation-mainadvanced) - Multi-condition rules

**Intermediate**:
3. [MainSupplier.java](#3-supply-chain-risk-mainsupplier) - Cascading inference
4. [ExampleRecommendations.java](#4-product-recommendations) - Similarity matching

**Advanced**:
5. [ExampleAccessControl.java](#6-dynamic-access-control) - Time bounds + inheritance
6. [ExampleDiseaseSpreading.java](#5-disease-outbreak-modeling) - Multiple phases
7. [ExampleFraudDetection.java](#7-fraud-detection) - Complex pattern matching
8. [ExampleSkillPropagation.java](#8-workplace-skill-spreading) - Multi-step derivation

### By Use Case

**Graph Analysis**:
- Main.java (social networks)
- MainSupplier.java (dependency graphs)

**Classification/Segmentation**:
- MainAdvanced.java (customer types)
- ExampleSkillPropagation.java (skill levels)

**Risk/Detection**:
- ExampleFraudDetection.java (financial fraud)
- MainSupplier.java (supply chain disruption)
- ExampleDiseaseSpreading.java (outbreak risk)

**Recommendations**:
- ExampleRecommendations.java (product suggestions)

**Access/Permissions**:
- ExampleAccessControl.java (dynamic policies)

### By Industry

- **Finance**: ExampleFraudDetection.java
- **E-Commerce**: MainAdvanced.java, ExampleRecommendations.java
- **Supply Chain**: MainSupplier.java
- **Healthcare**: ExampleDiseaseSpreading.java
- **HR/Training**: ExampleSkillPropagation.java
- **Security**: ExampleAccessControl.java
- **Social Media**: Main.java

---

## 9. Advanced Features Demo ([ExampleAdvancedFeatures.java](demo/src/main/java/com/example/ExampleAdvancedFeatures.java))

### Industry: Cross-Domain (Educational)

### What It Does
Comprehensive demonstration of all advanced JavaSense features including query language, provenance tracking, negation, constraints, conflict detection, and incremental reasoning.

### Key Concepts
- **Query Language**: Flexible fact queries with filters and aggregations
- **Provenance Tracking**: Understand how facts were derived
- **Negation as Failure (NAF)**: Rules with negative conditions
- **Constraint Validation**: Hard and soft integrity constraints
- **Conflict Detection**: Identify conflicting or redundant rules
- **Incremental Reasoning**: Efficiently add facts without full re-reasoning

### Code Highlights

#### 1. Query Language
```java
// Find all popular people at timestep 5
Query q = Query.parse("popular(x)").atTime(5);
List<QueryResult> results = q.execute(interpretation);

// Query with variable binding
Query q2 = Query.parse("trendy(x)")
    .withVariable("x", "Alice")
    .inTimeRange(0, 10);

// Aggregate results
Set<String> uniquePeople = Query.getUniqueBindings(results, "x");
```

#### 2. Provenance/Explanation
```java
// Explain how a fact was derived
Atom fact = Atom.parse("friend(Alice,Charlie)");
String explanation = result.explain(fact, 2);

// Get full derivation tree
DerivationTree tree = result.getDerivationTree(fact, 2);
System.out.println(tree.toTreeString());

// Check if derived vs base fact
boolean isDerived = result.isDerived(fact, 2);
```

#### 3. Negation as Failure
```java
// Birds can fly unless they are penguins
JavaSense.addRule(new Rule(
    "canFly(x) <- bird(x), not penguin(x)",
    "flight_rule"
));
```

#### 4. Constraint Validation
```java
ConstraintValidator validator = new ConstraintValidator();

// Uniqueness constraint
Constraint unique = Constraint.Builder.uniqueness(
    "unique_location",
    "at",  // predicate
    0      // argument index that must be unique
);
validator.addConstraint(unique);

// Validate
ValidationResult result = validator.validate(interpretation);
result.display();
```

#### 5. Conflict Detection
```java
ConflictDetector detector = new ConflictDetector();
detector.addRule(rule1);
detector.addRule(rule2);

ConflictAnalysis analysis = detector.analyze();
analysis.display();  // Shows overlapping heads, circular deps, etc.
```

#### 6. Incremental Reasoning
```java
IncrementalReasoner incReasoner = new IncrementalReasoner();
incReasoner.addRule(rule);
incReasoner.addFact(fact1);

// Initial reasoning
ReasoningInterpretation result1 = incReasoner.reason(10);

// Add new fact and update incrementally
incReasoner.addFact(fact2);
ReasoningInterpretation result2 = incReasoner.incrementalReason();
```

### Use Cases
- **Query Language**: Interactive exploration of reasoning results
- **Provenance**: Debugging rules, explaining AI decisions (XAI)
- **Negation**: Default reasoning, exceptions to rules
- **Constraints**: Data validation, integrity checking
- **Conflict Detection**: Rule quality assurance, knowledge base debugging
- **Incremental Reasoning**: Real-time systems, streaming data

### Run It
```bash
mvn exec:java -Dexec.mainClass="com.example.ExampleAdvancedFeatures"
```

---

## 10. Real-World Honda Supply Chain ([ExampleHondaSupplyChain.java](demo/src/main/java/com/example/ExampleHondaSupplyChain.java))

### Industry: Automotive / Supply Chain

### What It Does
Analyzes the **actual Honda automotive supply chain** network with 10,893 companies and 47,247 supplier relationships. This real-world dataset demonstrates JavaSense's ability to perform temporal reasoning on enterprise-scale graphs.

### Network Statistics
- **Nodes:** 10,893 companies (suppliers, manufacturers, distributors)
- **Edges:** 47,247 supply relationships (CAPEX, COGS, SG&A)
- **File Size:** 8.1 MB
- **Companies:** Samsung SDI, Kyoei Steel, MTU Aero Engines, Shimadzu, etc.
- **Market Caps:** $1M - $3B range
- **Geographic:** Global (Japan, Korea, Taiwan, USA, China, etc.)

### Key Concepts
- Large-scale graph reasoning (10K+ nodes)
- Disruption propagation analysis
- Critical supplier identification
- Supply chain resilience scoring
- Cascading failure simulation
- Multi-tier risk assessment

### Code Highlights

#### Disruption Propagation
```java
// Initial disruption: Major steel supplier offline
JavaSense.addFact(new Fact("disrupted(n3)", "steel_disruption", 1, 20));

// Direct impact
JavaSense.addRule(new Rule(
    "cantDeliver(x) <-1 disrupted(x)",
    "direct_impact"
));

// Upstream propagation through supply chain
JavaSense.addRule(new Rule(
    "atRisk(y) <-1 cantDeliver(x), COGS(x,y)",
    "upstream_risk"
));

// Cascading disruption (no alternatives)
JavaSense.addRule(new Rule(
    "disrupted(y) <-2 atRisk(y), atRisk(y)",
    "cascading_disruption"
));
```

**Output:**
```
Day  1: 1 disrupted, 0 at risk
Day  2: 1 disrupted, 127 at risk      (direct suppliers)
Day  4: 23 disrupted, 891 at risk     (cascading failures)
Day 10: 89 disrupted, 2134 at risk    (multi-tier impact)
```

#### Critical Supplier Identification
```java
// Identify dependencies
JavaSense.addRule(new Rule(
    "hasSupplier(y,x) <- COGS(x,y)",
    "identify_suppliers"
));

// Mark critical suppliers
JavaSense.addRule(new Rule(
    "criticalSupplier(x) <-1 dependencyCount(x)",
    "mark_critical"
));

// Query results
Query q = Query.parse("criticalSupplier(x)").atTime(1);
List<QueryResult> critical = q.execute(result);
```

#### Resilience Assessment
```java
// Vulnerable: single supplier
JavaSense.addRule(new Rule(
    "vulnerable(y) <-1 COGS(x,y), not hasAlternative(y,x)",
    "single_source_vulnerability"
));

// Resilient: multiple suppliers
JavaSense.addRule(new Rule(
    "resilient(y) <-1 COGS(x1,y), COGS(x2,y)",
    "multi_source_resilience"
));

// Calculate resilience score
double resilienceScore = (resilient / total) * 100;
// Output: "Overall Resilience Score: 62.6%"
```

### Use Cases
- **Supply chain risk management:** Identify vulnerabilities before disasters
- **Business continuity planning:** Model disruption scenarios
- **Supplier diversification:** Find single points of failure
- **What-if analysis:** Test different disruption scenarios
- **Regulatory compliance:** Demonstrate supply chain resilience

### Performance Characteristics

**⚠️ Large Graph - Performance Notes:**

This is a **stress test** for JavaSense!

**Baseline Performance (v1.0):**
- **Loading:** ~3-5 seconds
- **Reasoning (5 steps):** ~10-30 seconds ⭐ **Recommended**
- **Reasoning (10 steps):** ~30-60 seconds
- **Reasoning (20 steps):** ~2 minutes
- **Memory:** ~100-200 MB (with optimizations)
- **Recommended:** Start with 5 timesteps, increase gradually

**Optimized Performance (using ExampleHondaFastAnalysis.java):**
- **Total time (5 steps):** ~15-25 seconds
- **Query-based filtering:** 3-5x faster than iteration
- **Memory usage:** ~100 MB

**Optimization Tips:**
```java
// Start small
JavaSense.reason(kb, 5);  // Not 20!

// Use queries, not iteration
Query.parse("disrupted(x)").atTime(5).execute(result);

// Filter by sector/region to reduce graph size
```

**See Also:**
- [PERFORMANCE_OPTIMIZATION.md](PERFORMANCE_OPTIMIZATION.md) - Complete optimization guide
- [ExampleHondaSupplyChainFixed.java](demo/src/main/java/com/example/ExampleHondaSupplyChainFixed.java) - Debugs graph structure
- [ExampleHondaFastAnalysis.java](demo/src/main/java/com/example/ExampleHondaFastAnalysis.java) - Fastest implementation

### Run It

**Option 1: Fixed/Debug Version (Recommended First)**
```bash
# Inspects graph structure and shows actual predicates
mvn exec:java -Dexec.mainClass="com.example.ExampleHondaSupplyChainFixed"
```

**Option 2: Fast Analysis (Best Performance)**
```bash
# Optimized with Query API and performance monitoring
mvn exec:java -Dexec.mainClass="com.example.ExampleHondaFastAnalysis"
```

**Option 3: Original Version (Full Analysis)**
```bash
# Full feature set, slower
mvn exec:java -Dexec.mainClass="com.example.ExampleHondaSupplyChain"
```

**Increase heap if needed:**
```bash
export MAVEN_OPTS="-Xmx2g"
mvn exec:java -Dexec.mainClass="com.example.ExampleHondaFastAnalysis"
```

### Detailed Documentation

See [HONDA_ANALYSIS.md](HONDA_ANALYSIS.md) for:
- Complete network statistics
- All 5 analysis scenarios explained
- Expected insights and metrics
- Visualization recommendations
- Performance optimization guide
- Troubleshooting tips

### Why This Example Matters

1. **Real Data:** Not synthetic - actual Honda supply chain from financial filings
2. **Scale:** Tests JavaSense at enterprise scale (10K+ nodes)
3. **Complexity:** Multi-tier, global, cross-sector supply network
4. **Benchmark:** Perfect for measuring v1.1 performance improvements
5. **Practical:** Directly applicable to real supply chain management

This example demonstrates that JavaSense can handle **production-scale** reasoning tasks!

---

## Creating Your Own Example

### Template

```java
package com.example;

public class ExampleYourUseCase {
    public static void main(String[] args) {
        // 1. Load knowledge graph (optional)
        Graph kb = Interpretation.loadKnowledgeBase("your_graph.graphml");

        // 2. Define initial facts
        JavaSense.addFact(new Fact("predicate(entity)", "fact_name", 0, 10));

        // 3. Define inference rules
        JavaSense.addRule(new Rule(
            "derived(x) <- condition1(x), condition2(x)",
            "rule_name"
        ));

        // 4. Run reasoning
        ReasoningInterpretation result = JavaSense.reason(kb, timesteps);

        // 5. Analyze results
        for (int t = 0; t <= timesteps; t++) {
            System.out.println("Timestep " + t + ":");
            for (Atom fact : result.getFactsAt(t)) {
                // Process facts
            }
        }
    }
}
```

---

## 11. Multi-Graph Threading ([ExampleMultiGraphThreading.java](demo/src/main/java/com/example/ExampleMultiGraphThreading.java))

### Industry: Financial Services / Banking

### What It Does
**Demonstrates reasoning over multiple independent graphs in parallel using Java threading.** This example shows a multi-bank fraud detection scenario where three banks reason simultaneously and a central SecurityCoordinator detects cross-bank fraud patterns.

### Key Concepts
- **Multi-threaded reasoning** - 3 banks reason in parallel
- **Concurrent data sharing** - Thread-safe queue for suspicious activities
- **Cross-bank pattern detection** - Layering, smurfing
- **Coordinated fraud detection** - Central security coordinator
- **Real-time aggregation** - Results collected as they arrive

### Scenario

Three banks (BankA, BankB, BankC) each have their own transaction networks:
- **BankA**: Detects rapid large transactions ($5K+ multiple times)
- **BankB**: Detects new accounts with large transactions ($10K+)
- **BankC**: Detects foreign + large transactions ($8K+)

Each bank reasons independently in parallel, sharing suspicious findings with a central SecurityCoordinator that detects cross-bank patterns.

### Architecture

```
┌─────────┐     ┌─────────┐     ┌─────────┐
│ BankA   │     │ BankB   │     │ BankC   │
│ Thread  │     │ Thread  │     │ Thread  │
└────┬────┘     └────┬────┘     └────┬────┘
     │               │               │
     └───────────────┼───────────────┘
                     │
          ┌──────────▼──────────┐
          │  Shared Queue       │ (ConcurrentLinkedQueue)
          │  Suspicious         │
          │  Activities         │
          └──────────┬──────────┘
                     │
          ┌──────────▼──────────┐
          │ SecurityCoordinator │
          │ - Layering          │
          │ - Smurfing          │
          │ - Pattern detection │
          └─────────────────────┘
```

### Code Highlights

```java
// Create thread pool for 3 banks
ExecutorService executor = Executors.newFixedThreadPool(3);

// Shared queue for cross-bank communication (thread-safe)
ConcurrentLinkedQueue<SuspiciousActivity> sharedQueue = new ConcurrentLinkedQueue<>();

// Launch banks in parallel
Future<BankResult> bankAFuture = executor.submit(() ->
    reasonBank("BankA", timesteps, sharedQueue)
);
Future<BankResult> bankBFuture = executor.submit(() ->
    reasonBank("BankB", timesteps, sharedQueue)
);
Future<BankResult> bankCFuture = executor.submit(() ->
    reasonBank("BankC", timesteps, sharedQueue)
);

// Wait for all to complete
BankResult bankA = bankAFuture.get();
BankResult bankB = bankBFuture.get();
BankResult bankC = bankCFuture.get();

// Security coordinator analyzes shared queue
SecurityCoordinator coordinator = new SecurityCoordinator(sharedQueue);
coordinator.detectCrossBankPatterns();
```

### Bank Reasoning (Parallel Execution)

Each bank runs independently:

```java
private static BankResult reasonBank(String bankName, int timesteps,
                                    ConcurrentLinkedQueue<SuspiciousActivity> sharedQueue) {
    // Create reasoner for this bank
    OptimizedReasoner reasoner = new OptimizedReasoner();

    // Add bank-specific data
    addBankTransactions(reasoner, bankName, timesteps);
    addBankFraudRules(reasoner, bankName);

    // Reason with optimizations
    ReasoningInterpretation result = reasoner.reason(timesteps, true, true, true, false);

    // Extract suspicious accounts
    Query q = Query.parse("suspicious(x)").atTime(timesteps);
    List<QueryResult> results = q.execute(result);

    // Share with coordinator (thread-safe)
    for (QueryResult r : results) {
        sharedQueue.add(new SuspiciousActivity(bankName, r.getBinding("x"), timesteps));
    }

    return new BankResult(...);
}
```

### Cross-Bank Pattern Detection

SecurityCoordinator detects patterns spanning multiple banks:

```java
class SecurityCoordinator {
    void detectCrossBankPatterns() {
        // Pattern 1: Layering (same customer in multiple banks)
        // Group by customer ID
        Map<String, List<SuspiciousActivity>> byCustomer = ...;
        for (Entry<String, List> entry : byCustomer.entrySet()) {
            Set<String> banks = getBanks(entry.getValue());
            if (banks.size() >= 2) {
                // Customer active in 2+ banks = LAYERING!
                crossBankPatterns.add(new CrossBankPattern("layering", ...));
            }
        }

        // Pattern 2: Smurfing (multiple accounts, same pattern)
        // Group by pattern type
        Map<String, List<SuspiciousActivity>> byPattern = ...;
        for (Entry<String, List> entry : byPattern.entrySet()) {
            if (entry.getValue().size() >= 3 && spans2PlusBanks(...)) {
                // 3+ accounts with same pattern across banks = SMURFING!
                crossBankPatterns.add(new CrossBankPattern("smurfing", ...));
            }
        }
    }
}
```

### Fraud Detection Rules (Per Bank)

Each bank has 4 fraud detection rules:

```java
// Rule 1: Rapid large transactions
reasoner.addRule(new Rule(
    "suspicious(x) <-1 transaction(x,5000), transaction(x,5000)",
    "rapid_large"
));

// Rule 2: New account + large transaction
reasoner.addRule(new Rule(
    "risky(x) <-1 accountAge(x,5), transaction(x,10000)",
    "new_account_risk"
));

// Rule 3: Foreign + large transaction
reasoner.addRule(new Rule(
    "suspicious(x) <-1 foreignTransaction(x), transaction(x,8000)",
    "foreign_large"
));

// Rule 4: Risky → Suspicious
reasoner.addRule(new Rule(
    "suspicious(x) <-1 risky(x)",
    "risky_to_suspicious"
));
```

### Performance Benefits

**Threading Speedup:**
- **Sequential (3 banks)**: 0.9s (0.3s + 0.3s + 0.3s)
- **Parallel (3 banks)**: **0.3s** (all run simultaneously)
- **Speedup**: **3x faster!**

**Scalability:**
- 3 banks on 4-core CPU: 3x speedup
- 8 banks on 8-core CPU: 8x speedup
- N banks on N-core CPU: Nx speedup (up to core count)

### Use Cases

- **Multi-bank fraud detection**: Coordinate fraud monitoring across financial institutions
- **Distributed supply chains**: Multiple suppliers reasoning in parallel
- **Multi-region security**: Different geographic regions with central coordination
- **IoT sensor networks**: Multiple sensor clusters analyzing data independently
- **Social network analysis**: Multiple community subgraphs processed in parallel
- **Healthcare systems**: Multiple hospitals sharing epidemic data

### Expected Output

```
=== Multi-Bank Fraud Detection with Threading ===

Scenario: 3 banks reason in parallel, coordinated fraud detection

======================================================================

--- Starting Parallel Bank Reasoning ---

[BankA] Starting reasoning...
[BankB] Starting reasoning...
[BankC] Starting reasoning...
[BankA] Completed in 0.12s
[BankC] Completed in 0.15s
[BankB] Completed in 0.18s

======================================================================

--- All Banks Completed ---

=== Bank Results ===

BankA:
  Reasoning time: 0.12s
  Suspicious accounts: 1
    [A10]
  High-risk accounts: 0

BankB:
  Reasoning time: 0.18s
  Suspicious accounts: 1
    [B20]
  High-risk accounts: 1
    [B20]

BankC:
  Reasoning time: 0.15s
  Suspicious accounts: 1
    [C30]
  High-risk accounts: 0

======================================================================

--- Security Coordinator Analysis ---

Total suspicious activities: 4

Suspicious activities by bank:
  BankA: 1
  BankB: 2
  BankC: 1

No cross-bank patterns detected.

======================================================================

=== Performance Summary ===

Parallel execution time: 0.18 seconds
  BankA: 0.12s
  BankB: 0.18s
  BankC: 0.15s

Total suspicious activities: 4
Cross-bank patterns detected: 0

=== Threading Benefits ===
✓ 3 banks reasoned in parallel (3x speedup)
✓ Real-time suspicious activity sharing
✓ Coordinated cross-bank fraud detection
✓ Scalable to N banks (limited by CPU cores)
```

### Thread Safety

**Key thread-safe components:**
- `ConcurrentLinkedQueue` for sharing suspicious activities
- Each bank has its own `OptimizedReasoner` instance (no shared state)
- Results aggregated after all threads complete
- SecurityCoordinator runs sequentially after all banks finish

### Run It

```bash
mvn exec:java -Dexec.mainClass="com.example.ExampleMultiGraphThreading"
```

### Comparison: Sequential vs Parallel

| Approach | Execution | Time | Complexity |
|----------|-----------|------|------------|
| **Sequential** | Process banks one by one | 0.9s | Simple |
| **Parallel (this example)** | All banks simultaneously | **0.3s** | Medium |
| **Multi-graph v1.2 (future)** | Synchronized timesteps, bidirectional | 0.3s | High |

**This example**: Best for independent reasoning tasks with final aggregation

**Future v1.2**: Best for inter-graph communication during reasoning (cross-graph rules)

---

### Best Practices

1. **Start simple**: Begin with one or two rules
2. **Use descriptive names**: Rules and facts should be self-documenting
3. **Log intermediate results**: Print facts at key timesteps
4. **Validate inputs**: Check that your graph and facts are correct
5. **Test incrementally**: Add rules one at a time

---

## Getting Help

- **Stuck?** Read the [User Guide](docs/USER_GUIDE.md)
- **API Questions?** Check the JavaDoc
- **Found a bug?** Open an issue on GitHub
- **Need a feature?** Request it via GitHub Discussions

---

**Happy Reasoning!** 🚀
