# JavaSense User Guide

Complete guide to using JavaSense for temporal logical reasoning.

## Table of Contents

1. [Introduction](#introduction)
2. [Installation](#installation)
3. [Core Concepts](#core-concepts)
4. [Getting Started](#getting-started)
5. [Tutorial 1: Social Network Reasoning](#tutorial-1-social-network-reasoning)
6. [Tutorial 2: Supply Chain Analysis](#tutorial-2-supply-chain-analysis)
7. [Tutorial 3: E-Commerce Customer Insights](#tutorial-3-e-commerce-customer-insights)
8. [Tutorial 4: Working with Files](#tutorial-4-working-with-files)
9. [Tutorial 5: Advanced Temporal Rules](#tutorial-5-advanced-temporal-rules)
10. [Best Practices](#best-practices)
11. [Performance Tuning](#performance-tuning)
12. [Troubleshooting](#troubleshooting)

---

## Introduction

JavaSense is a temporal logical reasoning engine that allows you to:

- Define facts about entities and their relationships
- Create rules that derive new facts from existing ones
- Reason across discrete time steps
- Track how knowledge evolves over time

**Key Use Cases:**
- Supply chain risk propagation
- Social influence modeling
- Business rule inference
- Temporal knowledge graphs
- Event sequence analysis

---

## Installation

### Maven Project

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>javasense</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### Build from Source

```bash
git clone https://github.com/yourusername/javasense.git
cd javasense/demo
mvn clean install
```

---

## Core Concepts

### 1. Facts

**Facts** are ground truth statements about the world. Each fact has:
- **Predicate**: What the fact asserts (e.g., `popular`, `owns`)
- **Arguments**: Entities involved (e.g., `Mary`, `Car123`)
- **Time interval**: When the fact is true `[start, end]`

```java
// Mary is popular from timestep 0 to 2
Fact fact = new Fact("popular(Mary)", "fact1", 0, 2);
JavaSense.addFact(fact);
```

### 2. Rules

**Rules** define logical inference patterns. They follow Horn clause syntax:

```
head(vars) <- body1(vars), body2(vars), ...
```

**Anatomy of a rule:**

```
popular(x) <-1 popular(y), Friends(x,y)
│          │  │            │
│          │  │            └─ Body atoms (conditions)
│          │  └─ Delay (inference happens 1 timestep later)
│          └─ Arrow separator
└─ Head (what gets inferred)
```

**Rule components:**
- **Variables**: Lowercase starting letter (x, y, person)
- **Constants**: Uppercase starting letter (Mary, Car123)
- **Delay**: Number after `<-` (0 = immediate, 1 = next timestep, etc.)
- **Body**: Comma-separated conditions that must all be true

### 3. Atoms

**Atoms** are the building blocks of facts and rules:

```java
Atom atom = Atom.parse("popular(Mary)");
System.out.println(atom.getPredicate()); // "popular"
System.out.println(atom.getArgs());      // ["Mary"]
```

### 4. Time Intervals

Time in JavaSense is discrete (0, 1, 2, 3, ...). Facts and rules can be constrained to specific intervals:

```java
// Fact valid only in [0,2] and [5,8]
TimedFact tf = new TimedFact(
    Atom.parse("popular(Mary)"),
    "mary_fact",
    List.of(new Interval(0, 2), new Interval(5, 8))
);
```

### 5. Knowledge Graphs

Knowledge graphs represent entities and relationships:

```xml
<!-- knowledge_base.graphml -->
<edge source="Mary" target="John" label="Friends"/>
<edge source="John" target="Alice" label="Friends"/>
```

Edges automatically become facts:
- `Friends(Mary, John)` is true at all timesteps

---

## Getting Started

### Minimal Example

```java
import com.example.*;

public class MinimalExample {
    public static void main(String[] args) {
        // Add initial fact
        JavaSense.addFact(new Fact("popular(Mary)", "init", 0, 0));

        // Add rule: popularity spreads
        JavaSense.addRule(new Rule(
            "popular(x) <-1 popular(y), Friends(x,y)",
            "spread"
        ));

        // Load graph
        Graph kb = Interpretation.loadKnowledgeBase("graph.graphml");

        // Run reasoning for 5 timesteps
        ReasoningInterpretation result = JavaSense.reason(kb, 5);

        // Display results
        result.display();
    }
}
```

---

## Tutorial 1: Social Network Reasoning

**Goal**: Model how popularity spreads through a social network.

### Step 1: Create the Knowledge Graph

Create `social_network.graphml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<graphml>
  <graph edgedefault="directed">
    <node id="Alice"/>
    <node id="Bob"/>
    <node id="Carol"/>
    <node id="Dave"/>

    <edge source="Alice" target="Bob">
      <data key="label">Friends</data>
    </edge>
    <edge source="Bob" target="Carol">
      <data key="label">Friends</data>
    </edge>
    <edge source="Carol" target="Dave">
      <data key="label">Friends</data>
    </edge>
  </graph>
</graphml>
```

### Step 2: Write the Reasoning Code

```java
import com.example.*;
import java.util.Set;

public class SocialPopularity {
    public static void main(String[] args) {
        // Load social network
        Graph kb = Interpretation.loadKnowledgeBase("social_network.graphml");

        // Alice starts popular at timestep 0
        JavaSense.addFact(new Fact("popular(Alice)", "seed", 0, 10));

        // Rule: if you're friends with someone popular, you become popular
        // (with 1 timestep delay)
        JavaSense.addRule(new Rule(
            "popular(x) <-1 popular(y), Friends(y,x)",
            "popularity_spread"
        ));

        // Run reasoning
        ReasoningInterpretation result = JavaSense.reason(kb, 5);

        // Analyze results
        for (int t = 0; t <= 5; t++) {
            System.out.println("\nTimestep " + t + ":");
            Set<Atom> facts = result.getFactsAt(t);

            for (Atom fact : facts) {
                if (fact.getPredicate().equals("popular")) {
                    String person = fact.getArgs().get(0);
                    System.out.println("  " + person + " is popular");
                }
            }
        }
    }
}
```

### Expected Output

```
Timestep 0:
  Alice is popular

Timestep 1:
  Alice is popular
  Bob is popular

Timestep 2:
  Alice is popular
  Bob is popular
  Carol is popular

Timestep 3:
  Alice is popular
  Bob is popular
  Carol is popular
  Dave is popular
```

### Key Takeaways

- Facts from the graph (Friends edges) are available at all timesteps
- Initial facts seed the reasoning process
- The delay `<-1` means inference happens one timestep later
- Popularity spreads like a wave through the network

---

## Tutorial 2: Supply Chain Analysis

**Goal**: Track how supplier disruptions propagate to customers.

### Step 1: Model the Supply Chain

```java
import com.example.*;

public class SupplyChainRisk {
    public static void main(String[] args) {
        // Load supply chain graph with Supplier relationships
        Graph kb = Interpretation.loadKnowledgeBase("supply_chain.graphml");

        // Supplier A is disrupted starting at timestep 1
        JavaSense.addFact(new Fact("disrupted(SupplierA)", "incident", 1, 10));

        // Rule 1: If supplier is disrupted, customers are at risk
        JavaSense.addRule(new Rule(
            "at_risk(x) <-1 disrupted(y), Supplier(y,x)",
            "direct_impact"
        ));

        // Rule 2: Cascading risk through supply chain
        JavaSense.addRule(new Rule(
            "at_risk(x) <-1 at_risk(y), Supplier(y,x)",
            "cascade"
        ));

        // Reason over 10 timesteps
        ReasoningInterpretation result = JavaSense.reason(kb, 10);

        // Find all at-risk entities
        System.out.println("Supply Chain Impact Analysis:");
        for (int t = 0; t <= 10; t++) {
            Set<Atom> facts = result.getFactsAt(t);
            long atRiskCount = facts.stream()
                .filter(a -> a.getPredicate().equals("at_risk"))
                .count();

            if (atRiskCount > 0) {
                System.out.println("t=" + t + ": " + atRiskCount + " entities at risk");
            }
        }
    }
}
```

### Step 2: Understanding Cascading Effects

With two rules, disruption propagates:

1. **t=1**: SupplierA disrupted
2. **t=2**: Direct customers of SupplierA at risk
3. **t=3**: Customers of those customers at risk
4. **t=4+**: Risk continues cascading

---

## Tutorial 3: E-Commerce Customer Insights

**Goal**: Derive customer attributes from purchase patterns.

### Example: Trendy Customers

```java
import com.example.*;

public class CustomerSegmentation {
    public static void main(String[] args) {
        // Load customer ownership graph
        Graph kb = Interpretation.loadKnowledgeBase("customers.graphml");

        // Define product categories as facts
        JavaSense.addFact(new Fact("Car(Tesla)", "product1", 0, 10));
        JavaSense.addFact(new Fact("Pet(Dog)", "product2", 0, 10));
        JavaSense.addFact(new Fact("Car(BMW)", "product3", 0, 10));

        // Rule: Customers who own both a car and a pet are "trendy"
        JavaSense.addRule(new Rule(
            "trendy(x) <- owns(x,c), Car(c), owns(x,p), Pet(p)",
            "trendy_rule"
        ));

        // Reason
        ReasoningInterpretation result = JavaSense.reason(kb, 1);

        // Find trendy customers
        Set<Atom> facts = result.getFactsAt(1);
        System.out.println("Trendy Customers:");
        facts.stream()
            .filter(a -> a.getPredicate().equals("trendy"))
            .forEach(a -> System.out.println("  " + a.getArgs().get(0)));
    }
}
```

### Multiple Rules for Segmentation

```java
// Premium customers: own luxury cars
JavaSense.addRule(new Rule(
    "premium(x) <- owns(x,c), LuxuryCar(c)",
    "premium_rule"
));

// Budget-conscious: own used items
JavaSense.addRule(new Rule(
    "budget(x) <- owns(x,i), Used(i)",
    "budget_rule"
));

// Tech-savvy: own multiple electronics
JavaSense.addRule(new Rule(
    "tech_savvy(x) <- owns(x,e1), Electronics(e1), owns(x,e2), Electronics(e2)",
    "tech_rule"
));
```

---

## Tutorial 4: Working with Files

### Rules File Format

Create `rules.txt`:

```
# Comment lines start with #
# Format: head <- body

# Popularity spreading
popular(x) <-1 popular(y), Friends(x,y)

# Influence through multiple hops
influential(x) <- popular(x), Friends(x,y), Friends(y,z)

# Time-bounded rule (active only in [0,5])
trending(x) : [0,5] <- popular(x)
```

Load rules:

```java
JavaSense.addRulesFromFile("rules.txt");
```

### Facts File Format

Create `facts.txt`:

```
# Format: predicate(args),fact_name,start_time,end_time

popular(Mary),mary_popular,0,2
popular(John),john_popular,1,3
owns(Alice,Car123),alice_car,0,10
disrupted(SupplierX),disruption,5,8
```

Load facts:

```java
JavaSense.addFactsFromFile("facts.txt");
```

### Complete File-Based Example

```java
public class FileBasedReasoning {
    public static void main(String[] args) {
        // Load everything from files
        Graph kb = Interpretation.loadKnowledgeBase("graph.graphml");
        JavaSense.addRulesFromFile("rules.txt");
        JavaSense.addFactsFromFile("facts.txt");

        // Run reasoning
        ReasoningInterpretation result = JavaSense.reason(kb, 10);
        result.display();
    }
}
```

---

## Tutorial 5: Advanced Temporal Rules

### Time-Bounded Rules

Rules active only during specific intervals:

```java
// Rule only fires between timesteps 0-5
JavaSense.addRule(new Rule(
    "special_offer(x) : [0,5] <- premium(x)",
    "limited_offer",
    List.of(new Interval(0, 5))
));
```

### Variable Delays

Different rules with different delays:

```java
// Immediate inference (delay = 0)
JavaSense.addRule(new Rule(
    "connected(x,y) <- Friends(x,y)",
    "immediate"
));

// 2-timestep delay
JavaSense.addRule(new Rule(
    "influenced(x) <-2 popular(y), Friends(x,y)",
    "delayed"
));
```

### Multiple Intervals for Facts

Facts true at non-contiguous periods:

```java
// Store open Monday-Wednesday (t=0-2) and Friday (t=4)
TimedFact storeFact = new TimedFact(
    Atom.parse("open(Store)"),
    "store_hours",
    List.of(
        new Interval(0, 2),  // Mon-Wed
        new Interval(4, 4)   // Fri
    )
);
JavaSense.addTimedFact(storeFact);
```

### Head Intervals

Derived facts valid only in specific intervals:

```java
// Inferred fact only valid in [0,3]
JavaSense.addRule(new Rule(
    "sale_eligible(x) : [0,3] <- premium(x), active(x)",
    "sale_window"
));
```

---

## Best Practices

### 1. Naming Conventions

- **Variables**: Use descriptive lowercase names (`customer`, `supplier`, not just `x`, `y`)
- **Predicates**: Use verb phrases (`owns`, `connected_to`) or adjectives (`popular`, `trendy`)
- **Constants**: Use PascalCase (`SupplierA`, `Car123`)
- **Fact names**: Use descriptive identifiers (`initial_popularity`, `q1_disruption`)

### 2. Rule Design

**Good rule (specific):**
```java
popular(customer) <-1 follows(customer, influencer), popular(influencer)
```

**Bad rule (vague):**
```java
p(x) <- q(x,y), r(y)
```

### 3. Performance

- **Minimize rule body size**: Fewer atoms = faster unification
- **Use specific predicates**: Avoid overly general rules
- **Limit timesteps**: Only reason as far as needed
- **Index large graphs**: Consider pre-processing for massive graphs

### 4. Debugging

Enable debug logging in `logback.xml`:

```xml
<logger name="com.example" level="DEBUG" />
```

View reasoning steps:

```java
// Check facts at each timestep
for (int t = 0; t <= maxTime; t++) {
    System.out.println("Facts at t=" + t + ": " + result.getFactsAt(t).size());
}
```

---

## Performance Tuning

### Optimizing for Large Graphs

```java
// For graphs with >10,000 nodes
// 1. Limit timesteps
int maxTimesteps = 10; // Instead of 100

// 2. Use specific rules (not overly general)
// Bad: Many unifications
popular(x) <- connected(x,y), connected(y,z), connected(z,w)

// Good: Specific pattern
high_centrality(x) <- has_degree(x,d), GreaterThan(d,100)
```

### Memory Management

For very long reasoning sessions:

```java
// Process in batches
for (int batch = 0; batch < 10; batch++) {
    int start = batch * 10;
    int end = start + 10;

    ReasoningInterpretation result = JavaSense.reason(kb, end);
    // Process results for this batch
    processTimesteps(result, start, end);
}
```

---

## Troubleshooting

### Issue: No facts derived

**Symptom**: `getFactsAt(t)` returns empty or only graph facts

**Causes**:
1. Initial facts not in valid time range
2. Rule delay too large for timesteps
3. Variables don't unify

**Solution**:
```java
// Check initial facts
System.out.println("Initial facts: " + facts);

// Verify rule syntax
JavaSense.addRule(new Rule("popular(x) <- Friends(x,y)", "test"));

// Increase timesteps
ReasoningInterpretation result = JavaSense.reason(kb, 20); // Instead of 5
```

### Issue: Too many facts derived

**Symptom**: Exponential growth of facts

**Cause**: Rule creates cycles or redundant derivations

**Solution**: Add constraints or time bounds

```java
// Add time bound to prevent infinite derivation
popular(x) : [0,10] <-1 popular(y), Friends(x,y)
```

### Issue: GraphML file not loading

**Symptom**: `NullPointerException` or `FileNotFoundException`

**Solution**:
```java
// Use absolute path or verify working directory
Graph kb = Interpretation.loadKnowledgeBase("c:/full/path/graph.graphml");

// Or check current directory
System.out.println("Working dir: " + System.getProperty("user.dir"));
```

### Issue: Slow performance

**Symptom**: Reasoning takes minutes for small graphs

**Cause**: Complex rules with many variables

**Solution**:
```java
// Simplify rule bodies
// Instead of: result(w) <- a(w,x), b(x,y), c(y,z), d(z,w)
// Use: result(w) <- direct_pattern(w)
```

---

## Next Steps

- **API Reference**: See JavaDoc at `target/site/apidocs/`
- **Examples**: Explore `MainAdvanced.java` and `MainSupplier.java`
- **Community**: Join discussions at GitHub
- **Advanced Topics**: Constraint handling, negation (coming soon)

---

**Happy Reasoning!** 🚀
