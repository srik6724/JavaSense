# Rule Management in JavaSense

## The Problem: Scalability with Many Rules

**Question:** If we have 100+ rules, won't using if-statements become unmaintainable?

**Answer:** JavaSense uses a **declarative rule engine**, not imperative if-statements!

## How Rules Actually Work

### ❌ WRONG: Imperative If-Statements (Anti-Pattern)
```java
// This is NOT how you should use JavaSense!
if (knows(x, y)) {
    addFact(friend(x, y));
}
if (friend(x, y) && friend(y, z)) {
    addFact(friend(x, z));
}
// ... 100 more if-statements? NO!
```

**Problems:**
- Unmanageable at scale
- Order-dependent
- Hard to debug
- Not declarative

---

### ✅ CORRECT: Declarative Rules (JavaSense Way)
```java
// Define rules declaratively
JavaSense.addRule(new Rule("friend(x,y) <- knows(x,y)", "r1"));
JavaSense.addRule(new Rule("friend(x,z) <- friend(x,y), friend(y,z)", "r2"));
JavaSense.addRule(new Rule("popular(x) <- friend(y,x)", "r3"));
// ... add 100+ more rules easily!

// The engine processes ALL rules automatically
ReasoningInterpretation result = JavaSense.reason(kb, 10);
```

**Advantages:**
- **Declarative**: Describe WHAT, not HOW
- **Order-independent**: Rules can be in any order
- **Automatic**: Engine handles all rule firing
- **Scalable**: 100s of rules work the same as 10

---

## Understanding If-Statements in Examples

The if-statements you see in examples are for **OUTPUT FILTERING**, not rule execution:

```java
// This is just for DISPLAY - filtering what to show the user
for (Atom fact : result.getFactsAt(t)) {
    if (fact.getPredicate().equals("canAccess")) {  // ← Display filter
        System.out.println(fact);
    }
}
```

**This is NOT a rule** - it's just choosing which facts to print!

---

## Managing Many Rules: Best Practices

### 1. Load Rules from Files

**Instead of:**
```java
JavaSense.addRule(new Rule("rule1", "r1"));
JavaSense.addRule(new Rule("rule2", "r2"));
// ... 100 more lines
```

**Do this:**
```java
// rules.txt file:
// friend(x,y) <- knows(x,y)
// popular(x) <- friend(y,x)
// trendy(x) <- popular(x), owns(x,car)
// ... 100 more rules

JavaSense.addRulesFromFile("rules.txt");
```

See [JavaSense.java:84](demo/src/main/java/com/example/JavaSense.java#L84)

---

### 2. Organize Rules by Domain

```java
// access_control_rules.txt
canAccess(u,r) <- hasRole(u,role), permission(role,r)
canAccess(mgr,r) <- Manages(mgr,emp), canAccess(emp,r)
canAccess(u,r) <- Owner(u,r)

// fraud_detection_rules.txt
suspicious(x) <- largeWithdrawal(x), largeWithdrawal(x)
risky(x) <- newAccount(x), largeTransfer(x)
flagged(x) <- Transfer(x,y), suspicious(y)

// Load by domain
JavaSense.addRulesFromFile("access_control_rules.txt");
JavaSense.addRulesFromFile("fraud_detection_rules.txt");
```

---

### 3. Use Query Language (Not If-Statements)

**Instead of:**
```java
// Filtering with if-statements ❌
for (Atom fact : result.getFactsAt(5)) {
    if (fact.getPredicate().equals("canAccess")) {
        if (fact.getArgs().get(0).equals("Alice")) {
            System.out.println(fact);
        }
    }
}
```

**Use Query API:**
```java
// Query language ✅
Query q = Query.parse("canAccess(Alice, x)").atTime(5);
List<QueryResult> results = q.execute(result);
results.forEach(System.out::println);
```

---

### 4. Rule Organization Example (100 Rules)

```java
public class RuleRegistry {
    public static void loadAllRules() {
        // Access Control (20 rules)
        JavaSense.addRulesFromFile("rules/access_control.txt");

        // Fraud Detection (30 rules)
        JavaSense.addRulesFromFile("rules/fraud_detection.txt");

        // Supply Chain (25 rules)
        JavaSense.addRulesFromFile("rules/supply_chain.txt");

        // Recommendations (15 rules)
        JavaSense.addRulesFromFile("rules/recommendations.txt");

        // Custom business logic (10 rules)
        JavaSense.addRulesFromFile("rules/custom_logic.txt");

        // Total: 100 rules, all managed declaratively!
    }
}
```

---

## Performance with Many Rules

### How JavaSense Processes Rules

```
For each timestep t:
    For each rule r:
        If rule is active at time t:
            Find all substitutions that satisfy rule body
            For each substitution θ:
                Derive head(θ) at appropriate time
```

**Complexity:** O(T × R × F^B)
- T = timesteps
- R = number of rules
- F = facts per timestep
- B = body atoms per rule

### Optimization Strategies (v1.1+)

#### 1. Rule Indexing by Predicate
```java
// Group rules by predicates they depend on
Map<String, List<Rule>> ruleIndex = new HashMap<>();

// Rule: friend(x,y) <- knows(x,y)
ruleIndex.get("knows").add(rule);

// Only evaluate rules when relevant predicates change
```

#### 2. Rete Algorithm (Future: v2.0)
```java
// Build a Rete network for efficient pattern matching
// Share common conditions across rules
// Only re-evaluate affected rules when facts change
```

#### 3. Lazy Rule Evaluation
```java
// Don't evaluate rules that can't possibly fire
if (rule.requiresPredicate("suspended") &&
    !factsContainPredicate("suspended")) {
    skip_rule();  // Can't possibly match
}
```

---

## Real-World Example: 100-Rule System

### Scenario: Enterprise Compliance System

```java
public class ComplianceSystem {
    public static void main(String[] args) {
        // Load knowledge graphs
        Graph employees = Interpretation.loadKnowledgeBase("employees.graphml");
        Graph transactions = Interpretation.loadKnowledgeBase("transactions.graphml");

        // Load rule sets (100+ rules total)
        JavaSense.addRulesFromFile("rules/sox_compliance.txt");      // 25 rules
        JavaSense.addRulesFromFile("rules/gdpr_compliance.txt");     // 30 rules
        JavaSense.addRulesFromFile("rules/pci_dss.txt");             // 20 rules
        JavaSense.addRulesFromFile("rules/data_retention.txt");      // 15 rules
        JavaSense.addRulesFromFile("rules/access_logging.txt");      // 10 rules

        // Add constraints
        ConstraintValidator validator = new ConstraintValidator();
        validator.addConstraint(Constraint.Builder.uniqueness(
            "no_dual_control_violation", "approved", 0));
        // ... 20 more constraints

        // Detect conflicts in rules
        ConflictDetector detector = new ConflectDetector();
        // Auto-load all rules for analysis
        detector.analyze().display();

        // Run reasoning (all 100+ rules applied automatically)
        ReasoningInterpretation result = JavaSense.reason(null, 365); // 1 year

        // Query for violations (not if-statements!)
        Query violations = Query.parse("violation(x, regulation)")
            .inTimeRange(0, 365);

        List<QueryResult> issues = violations.execute(result);

        System.out.println("Found " + issues.size() + " compliance violations");
        for (QueryResult issue : issues) {
            System.out.println("  Day " + issue.getTime() + ": " +
                issue.getBinding("x") + " violated " +
                issue.getBinding("regulation"));
        }
    }
}
```

**Key points:**
- 100+ rules loaded from files
- Zero if-statements for rule logic
- All rules processed automatically
- Query language for result filtering

---

## Rule File Format

### Basic Syntax
```
# rules.txt

# Comment: This is a rule
head(x,y) <- body1(x,z), body2(z,y)

# Rule with delay
derived(x) <-2 source(x), condition(x)

# Rule with negation
eligible(x) <- qualified(x), not suspended(x)

# Rule with time interval
temporary(x) : [0,5] <- granted(x)

# Rule with active intervals
active(x) <- trigger(x)  # Active only at specific times
```

### Advanced: Conditional Rules
```
# Load different rule sets based on configuration
if (environment == "production") {
    JavaSense.addRulesFromFile("rules/production.txt");
} else {
    JavaSense.addRulesFromFile("rules/testing.txt");
}
```

---

## Anti-Patterns to Avoid

### ❌ Don't: Mix Imperative and Declarative
```java
// Bad: Manual rule execution
for (Atom fact : facts) {
    if (fact.getPredicate().equals("knows")) {
        Atom newFact = new Atom("friend", fact.getArgs());
        addFact(newFact);  // Manual fact derivation
    }
}

// Good: Let the engine handle it
JavaSense.addRule(new Rule("friend(x,y) <- knows(x,y)", "r1"));
```

### ❌ Don't: Hardcode Rules in Java
```java
// Bad: Rules as Java methods
public boolean canAccess(User u, Resource r) {
    if (hasRole(u, "Admin")) return true;
    if (manages(u.manager, r.owner)) return true;
    // ... more conditions
}

// Good: External rule file
// canAccess(u,r) <- hasRole(u,Admin)
// canAccess(u,r) <- Manages(mgr,owner), hasManager(u,mgr), owns(owner,r)
```

### ❌ Don't: Reinvent the Rule Engine
```java
// Bad: Custom rule evaluation loop
for (Rule rule : rules) {
    if (rule.canFire(facts)) {
        facts.addAll(rule.derive());
    }
}

// Good: Use JavaSense engine
JavaSense.reason(kb, timesteps);
```

---

## FAQ

### Q: How many rules can JavaSense handle?
**A:** Tested with 1000+ rules. Performance depends on:
- Rule complexity (number of body atoms)
- Fact count
- Timesteps
- Available memory

### Q: How do I debug 100 rules?
**A:** Use the advanced features!
```java
// 1. Conflict detection
ConflictDetector detector = new ConflictDetector();
detector.analyze().display();

// 2. Provenance tracking
String explanation = result.explain(Atom.parse("violation(x)"), 10);
System.out.println(explanation);  // Shows which rules fired

// 3. Query specific rule firings
Provenance prov = result.getProvenance();
List<AtomTimeKey> facts = prov.getFactsDeriveredByRule("sox_rule_23");
```

### Q: Can I dynamically add/remove rules?
**A:** In v1.0, rules are static per reasoning session. For dynamic rules:
```java
// Create new reasoner instance
Reasoner r1 = new Reasoner();
r1.addRule(rule1);
r1.reason(10);

// Different rule set
Reasoner r2 = new Reasoner();
r2.addRule(rule2);
r2.reason(10);
```

Future (v2.0): Dynamic rule updates with incremental reasoning.

---

## Summary

| Approach | Scalability | Maintainability | Performance |
|----------|-------------|-----------------|-------------|
| If-statements | ❌ Poor | ❌ Terrible | ⚠️ OK |
| Declarative Rules | ✅ Excellent | ✅ Excellent | ✅ Good |
| Rule Files | ✅ Excellent | ✅ Excellent | ✅ Good |
| Query Language | ✅ Excellent | ✅ Excellent | ✅ Excellent |

**Bottom line:** JavaSense is designed for 100s of rules. Use declarative rules, not if-statements!

---

**See also:**
- [Examples](EXAMPLES.md) - 9 real-world examples
- [Roadmap](ROADMAP.md) - Future optimizations
- [User Guide](docs/USER_GUIDE.md) - Complete API reference
