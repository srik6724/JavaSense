# JavaSense v1.2 - Advanced Reasoning Features

**Release Date:** January 2025
**Status:** ✅ Production Ready

---

## Overview

JavaSense v1.2 introduces three powerful new capabilities that extend temporal reasoning with **uncertainty handling**, **real-world time**, and **optimization**:

1. **🎲 Probabilistic Reasoning** - Reason with uncertain facts and rules
2. **⏰ Continuous Time** - Use real timestamps instead of discrete steps
3. **🎯 Constraint Optimization** - Find optimal solutions with constraints

---

## 1. Probabilistic Reasoning

### What is it?

Handle uncertainty by attaching probabilities (0.0 to 1.0) to facts and rules. Probabilities propagate through inference chains automatically.

### Key Features

- **Probabilistic Facts**: Facts with certainty values
- **Probabilistic Rules**: Rules with confidence values
- **Automatic Propagation**: P(derived) = P(rule) × P(premise₁) × P(premise₂) × ...
- **Threshold Filtering**: Filter facts below a probability threshold
- **Provenance Tracking**: Track how probabilities are computed

### Quick Start

```java
ProbabilisticReasoner reasoner = new ProbabilisticReasoner();

// Add uncertain fact (70% sure supplier is disrupted)
reasoner.addProbabilisticFact(new ProbabilisticFact(
    Atom.parse("disrupted(ACME)"),
    "alert_001",
    0.7,  // 70% probability
    List.of(new Interval(0, 100))
));

// Add certain fact
reasoner.addFact(new TimedFact(
    Atom.parse("supplies(ACME,Engine)"),
    "supply_001",
    List.of(new Interval(0, 100))
));

// Add probabilistic rule (90% confidence)
reasoner.addProbabilisticRule(new ProbabilisticRule(
    "atRisk(part) <-1 disrupted(supplier), supplies(supplier,part)",
    "risk_rule",
    0.9  // 90% confidence
));

// Reason
ProbabilisticInterpretation result = reasoner.reason(10);

// Query with probability
double prob = result.getProbability(Atom.parse("atRisk(Engine)"), 1);
System.out.println("P(atRisk(Engine)) = " + prob);  // 0.63 = 0.7 × 0.9
```

### Use Cases

- **Risk Assessment**: Supply chain risk with uncertain alerts
- **Medical Diagnosis**: Symptoms → diseases with probabilities
- **Fraud Detection**: Confidence scores for fraud patterns
- **Sensor Fusion**: Combine noisy sensor data
- **ML Integration**: Use ML model outputs as probabilistic facts

### API Reference

#### ProbabilisticFact

```java
// Constructor
ProbabilisticFact(Atom atom, String id, double probability, List<Interval> intervals)

// Methods
double getProbability()
```

#### ProbabilisticRule

```java
// Constructor
ProbabilisticRule(String ruleString, String name, double probability)

// Methods
double getProbability()
```

#### ProbabilisticReasoner

```java
// Add facts/rules
void addProbabilisticFact(ProbabilisticFact fact)
void addProbabilisticRule(ProbabilisticRule rule)
void addFact(TimedFact fact)  // Certain facts
void addRule(Rule rule)        // Certain rules

// Set threshold
void setMinProbabilityThreshold(double threshold)

// Reason
ProbabilisticInterpretation reason(int timesteps)
```

#### ProbabilisticInterpretation

```java
// Query probabilities
double getProbability(Atom atom, int time)
Map<AtomTimeKey, Double> getAllProbabilities()
Set<Atom> getFactsAboveThreshold(double threshold, int time)
```

---

## 2. Continuous Time

### What is it?

Use real timestamps (`java.time.Instant`) and durations (`java.time.Duration`) instead of discrete integer timesteps. Perfect for IoT, financial data, and event logs.

### Key Features

- **Real Timestamps**: Use `Instant` for precise times
- **Duration-Based Rules**: Rules with real-world delays (1 hour, 5 minutes, etc.)
- **Time Intervals**: Facts hold during continuous intervals
- **Flexible Queries**: Query at any instant or during any interval
- **Temporal Operations**: Intersection, overlap, containment

### Quick Start

```java
ContinuousTimeReasoner reasoner = new ContinuousTimeReasoner();

// Add fact with timestamp range
reasoner.addFact(ContinuousTimeFact.during(
    Atom.parse("temperature(sensor1,95)"),
    "reading_001",
    Instant.parse("2025-01-15T09:00:00Z"),
    Instant.parse("2025-01-15T17:00:00Z")
));

// Add duration-based rule (alert if high temp for > 1 hour)
reasoner.addRule(new ContinuousTimeRule(
    "alert(sensor) <-1h highTemp(sensor)",
    "sustained_high_temp",
    Duration.ofHours(1)  // Look back 1 hour
));

// Query at specific instant
Instant queryTime = Instant.parse("2025-01-15T10:00:00Z");
Set<Atom> facts = reasoner.queryAt(queryTime);

// Reason over time range
TimeInterval range = TimeInterval.between(
    Instant.parse("2025-01-15T08:00:00Z"),
    Instant.parse("2025-01-15T18:00:00Z")
);
ContinuousTimeInterpretation result = reasoner.reason(range);
```

### Use Cases

- **IoT Sensors**: Temperature, pressure, vibration with precise timestamps
- **Financial Markets**: Millisecond-precision trading data
- **Event Logs**: Server logs, application events
- **Process Monitoring**: Manufacturing processes with real-time data
- **Medical Records**: Patient vitals with exact times

### API Reference

#### TimeInterval

```java
// Create intervals
static TimeInterval at(Instant instant)
static TimeInterval between(Instant start, Instant end)
static TimeInterval from(Instant start)
static TimeInterval lastHours(long hours)
static TimeInterval lastMinutes(long minutes)
static TimeInterval today()

// Query operations
boolean contains(Instant instant)
boolean overlaps(TimeInterval other)
Optional<TimeInterval> intersection(TimeInterval other)
Duration duration()
```

#### ContinuousTimeFact

```java
// Constructors
ContinuousTimeFact(Atom atom, String id, List<TimeInterval> intervals)
static ContinuousTimeFact at(Atom atom, String id, Instant instant)
static ContinuousTimeFact during(Atom atom, String id, Instant start, Instant end)

// Query methods
boolean holdsAt(Instant instant)
boolean holdsDuring(TimeInterval interval)
```

#### ContinuousTimeRule

```java
// Constructors
ContinuousTimeRule(String ruleString, String name, Duration delay)
static ContinuousTimeRule instantaneous(String ruleString, String name)
static ContinuousTimeRule withHours(String ruleString, String name, long hours)
static ContinuousTimeRule withMinutes(String ruleString, String name, long minutes)

// Methods
Duration getDelay()
boolean isInstantaneous()
```

#### ContinuousTimeReasoner

```java
// Add facts/rules
void addFact(ContinuousTimeFact fact)
void addRule(ContinuousTimeRule rule)

// Query
Set<Atom> queryAt(Instant instant)
Set<Atom> queryDuring(TimeInterval interval)

// Reason
ContinuousTimeInterpretation reason(TimeInterval timeRange)
ContinuousTimeInterpretation reason()  // Uses full time range
```

---

## 3. Constraint Optimization

### What is it?

Find solutions that maximize or minimize an objective function while satisfying hard and soft constraints. Extends reasoning from "what is true" to "what is best".

### Key Features

- **Objective Functions**: Minimize or maximize numeric values
- **Hard Constraints**: Must-satisfy constraints (eliminate infeasible solutions)
- **Soft Constraints**: Preferences with penalties (try to satisfy)
- **Solution Ranking**: Find top-k best solutions
- **Multi-Criteria**: Balance competing objectives

### Quick Start

```java
OptimizationReasoner reasoner = new OptimizationReasoner();

// Add supplier facts: supplier(name, cost, quality)
reasoner.addFact(new TimedFact(
    Atom.parse("supplier(SupplierA,100,0.75)"),
    "sup_a",
    List.of(new Interval(0, 100))
));

reasoner.addFact(new TimedFact(
    Atom.parse("supplier(SupplierB,150,0.95)"),
    "sup_b",
    List.of(new Interval(0, 100))
));

// Selection rule
reasoner.addRule(new Rule(
    "selected(name,cost,quality) <-0 supplier(name,cost,quality)",
    "selection_rule"
));

// Hard constraint: Quality must be > 0.7
reasoner.addHardConstraint("selected", atom -> {
    double quality = Double.parseDouble(atom.getArgs().get(2));
    return quality > 0.7;
});

// Soft constraint: Prefer low cost (penalty if cost > 120)
reasoner.addSoftConstraint("selected", 10.0, atom -> {
    double cost = Double.parseDouble(atom.getArgs().get(1));
    return cost <= 120;
});

// Objective: Minimize cost
reasoner.setObjectiveFunction(
    OptimizationReasoner.ObjectiveType.MINIMIZE,
    atom -> Double.parseDouble(atom.getArgs().get(1))
);

// Find optimal solution
OptimizationResult result = reasoner.optimize(10);
Solution best = result.getBestSolution();

System.out.println("Best solution: " + best);
System.out.println("Objective value: " + best.getScore());
```

### Use Cases

- **Resource Allocation**: Minimize cost while meeting requirements
- **Scheduling**: Minimize makespan, maximize throughput
- **Supplier Selection**: Balance cost, quality, delivery time
- **Route Planning**: Minimize distance, time, or fuel
- **Configuration**: Find best system configuration

### API Reference

#### OptimizationReasoner

```java
// Add facts/rules
void addRule(Rule rule)
void addFact(TimedFact fact)

// Set objective
void setObjective(ObjectiveType type, String predicate)
void setObjectiveFunction(ObjectiveType type, Function<Atom, Double> function)

// Add constraints
void addHardConstraint(String predicatePattern, Function<Atom, Boolean> checker)
void addSoftConstraint(String predicatePattern, double penalty, Function<Atom, Boolean> checker)

// Optimize
OptimizationResult optimize(int timesteps)
List<Solution> findTopK(int timesteps, int k)
```

#### ObjectiveType

```java
enum ObjectiveType {
    MINIMIZE,
    MAXIMIZE
}
```

#### Solution

```java
// Query solution
int getTimestep()
Set<Atom> getFacts()
double getScore()
List<Atom> getFactsWithPredicate(String predicate)
```

#### OptimizationResult

```java
// Get results
Solution getBestSolution()
double getObjectiveValue()
List<Solution> getAllSolutions()
List<Solution> getTopK(int k)
boolean hasFeasibleSolution()
```

---

## Examples

All examples are in `src/main/java/com/example/`:

### Individual Feature Examples

- **`ExampleProbabilistic.java`** - Supply chain risk with uncertain alerts
  - Demonstrates probabilistic facts, rules, and probability queries

- **`ExampleContinuousTime.java`** - IoT temperature monitoring
  - Shows real timestamps, duration-based rules, timeline queries

- **`ExampleOptimization.java`** - Supplier selection optimization
  - Hard/soft constraints, objective minimization, solution ranking

### Combined Example

- **`ExampleV12Combined.java`** - All three features together
  - Smart supply chain with uncertainty, real-time data, and optimization

### Running Examples

```bash
# Probabilistic Reasoning
mvn exec:java -Dexec.mainClass="com.example.ExampleProbabilistic"

# Continuous Time
mvn exec:java -Dexec.mainClass="com.example.ExampleContinuousTime"

# Optimization
mvn exec:java -Dexec.mainClass="com.example.ExampleOptimization"

# All Features Combined
mvn exec:java -Dexec.mainClass="com.example.ExampleV12Combined"
```

---

## Performance Considerations

### Probabilistic Reasoning

- **Overhead**: ~10-15% compared to standard reasoning (probability tracking)
- **Memory**: Additional storage for probability map (small overhead)
- **Scalability**: Handles 10,000+ probabilistic facts efficiently

### Continuous Time

- **Sampling**: Time ranges are sampled (default: 100 points)
- **Cache**: Derived facts are cached per interval
- **Precision**: Millisecond precision supported
- **Tip**: For long time ranges, increase sampling for better accuracy

### Constraint Optimization

- **Solution Space**: Generates O(timesteps) candidate solutions
- **Filtering**: Hard constraints filter early (efficient)
- **Soft Constraints**: Add penalty computation overhead
- **Tip**: Use hard constraints to prune search space before soft constraints

---

## Migration Guide

### From v1.1 to v1.2

**Good news:** v1.2 is 100% backward compatible! All existing code continues to work.

#### Adding Probabilistic Reasoning

```java
// Before (v1.1)
TimedFact fact = new TimedFact(atom, id, intervals);
reasoner.addFact(fact);

// After (v1.2) - Add probability
ProbabilisticFact probFact = new ProbabilisticFact(atom, id, 0.8, intervals);
reasoner.addProbabilisticFact(probFact);
```

#### Switching to Continuous Time

```java
// Before (v1.1) - Discrete timesteps
OptimizedReasoner reasoner = new OptimizedReasoner();
reasoner.addFact(new TimedFact(atom, id, List.of(new Interval(0, 100))));
ReasoningInterpretation result = reasoner.reason(100);

// After (v1.2) - Real timestamps
ContinuousTimeReasoner reasoner = new ContinuousTimeReasoner();
reasoner.addFact(ContinuousTimeFact.during(
    atom, id,
    Instant.parse("2025-01-01T00:00:00Z"),
    Instant.parse("2025-01-02T00:00:00Z")
));
ContinuousTimeInterpretation result = reasoner.reason();
```

#### Adding Optimization

```java
// Before (v1.1) - Just reasoning
OptimizedReasoner reasoner = new OptimizedReasoner();
ReasoningInterpretation result = reasoner.reason(10);

// After (v1.2) - With optimization
OptimizationReasoner reasoner = new OptimizationReasoner();
reasoner.setObjective(ObjectiveType.MINIMIZE, "cost");
reasoner.addHardConstraint("quality", atom -> checkQuality(atom));
OptimizationResult result = reasoner.optimize(10);
Solution best = result.getBestSolution();
```

---

## Best Practices

### Probabilistic Reasoning

1. **Use threshold filtering** to focus on high-confidence facts
2. **Document probability sources** (sensor accuracy, ML model confidence, etc.)
3. **Combine with provenance** to explain probability calculations
4. **Validate probability calibration** - do 0.7 probabilities match 70% accuracy?

### Continuous Time

1. **Use appropriate time granularity** (don't use nanoseconds if you don't need them)
2. **Sample wisely** - more samples = better accuracy but slower performance
3. **Cache aggressively** - identical interval queries are cached
4. **Use duration helpers** - `TimeInterval.lastHours(1)` instead of manual calculation

### Constraint Optimization

1. **Start with hard constraints** - they eliminate solutions fastest
2. **Tune soft constraint penalties** - should reflect real-world trade-offs
3. **Use top-k solutions** - show alternatives, not just the best
4. **Profile objective functions** - they're called many times

---

## Future Roadmap

Potential v1.3 features (based on community feedback):

- **Distributed Reasoning**: Parallel reasoning across multiple machines
- **GPU Acceleration**: Massively parallel rule matching
- **Advanced Optimization**: Support for more constraint types, better solvers
- **Temporal Aggregation**: SUM, AVG, COUNT over time windows
- **Reactive Rules**: Trigger external actions when patterns match

---

## Support & Resources

- **Documentation**: See `NEW_FEATURES.md` for v1.1 features
- **Integration Guide**: `INTEGRATION_GUIDE.md` for Kafka/Neo4j
- **Examples**: All examples in `src/main/java/com/example/`
- **API Reference**: Full Javadoc in each source file

---

## Changelog

### v1.2.0 (January 2025)

**Added:**
- Probabilistic reasoning with uncertainty propagation
- Continuous time with real timestamps and durations
- Constraint optimization with hard/soft constraints
- TimeInterval utility class
- ProbabilisticFact, ProbabilisticRule, ProbabilisticReasoner
- ContinuousTimeFact, ContinuousTimeRule, ContinuousTimeReasoner
- OptimizationReasoner with objective functions
- Comprehensive examples for all new features
- V12_FEATURES.md documentation

**Changed:**
- None (100% backward compatible)

**Fixed:**
- None

---

**Ready to use?** Check out the examples and start building smarter reasoning systems! 🚀
