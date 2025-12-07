# JavaSense

**JavaSense** is a powerful Java-based temporal logical reasoning engine for knowledge graph inference. It enables time-aware forward chaining to perform logical deduction across discrete time steps, making it ideal for applications requiring temporal reasoning, such as supply chain analysis, social network modeling, and business intelligence.

## Features

- **Temporal Reasoning**: Perform logical inference with time-aware rules and facts
- **Knowledge Graph Integration**: Load and reason over GraphML-formatted knowledge graphs
- **Forward Chaining Engine**: Automatic derivation of new facts using rule-based inference
- **Flexible Rule Language**: Horn clause syntax with temporal delays and constraints
- **Multiple Time Intervals**: Facts and rules can span multiple disjoint time periods
- **Variable Unification**: Automatic pattern matching and variable substitution
- **Java Native**: High-performance reasoning engine built for the JVM ecosystem

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Installation

Clone the repository and build with Maven:

```bash
git clone https://github.com/yourusername/javasense.git
cd javasense/demo
mvn clean install
```

### Basic Example

```java
import com.example.*;

public class Example {
    public static void main(String[] args) {
        // Load a knowledge graph
        Graph kb = Interpretation.loadKnowledgeBase("knowledge_base.graphml");

        // Add an initial fact: Mary is popular from timestep 0 to 2
        JavaSense.addFact(new Fact("popular(Mary)", "initial_fact", 0, 2));

        // Define a rule: popularity spreads through friendships with 1 timestep delay
        JavaSense.addRule(new Rule(
            "popular(x) <-1 popular(y), Friends(x,y)",
            "popularity_rule"
        ));

        // Run reasoning for 10 timesteps
        ReasoningInterpretation result = JavaSense.reason(kb, 10);

        // Display results
        for (int t = 0; t <= 10; t++) {
            System.out.println("Timestep " + t + ":");
            for (Atom fact : result.getFactsAt(t)) {
                System.out.println("  " + fact);
            }
        }
    }
}
```

## Core Concepts

### Facts

Facts represent true statements in your knowledge base:

```java
// Simple fact: popular(Mary) is true from time 0 to 2
JavaSense.addFact(new Fact("popular(Mary)", "fact_name", 0, 2));

// Timed fact with multiple intervals
JavaSense.addTimedFact(new TimedFact(
    Atom.parse("popular(Mary)"),
    "fact_name",
    List.of(new Interval(0, 2), new Interval(5, 8))
));
```

### Rules

Rules define how new facts can be inferred:

```java
// Basic rule: if y is popular and x is friends with y, then x becomes popular
JavaSense.addRule(new Rule(
    "popular(x) <- popular(y), Friends(x,y)",
    "rule_name"
));

// Rule with delay: inference happens 1 timestep later
JavaSense.addRule(new Rule(
    "popular(x) <-1 popular(y), Friends(x,y)",
    "delayed_rule"
));

// Time-bounded rule: only active during specific intervals
JavaSense.addRule(new Rule(
    "popular(x) : [0,5] <- popular(y), Friends(x,y)",
    "bounded_rule",
    List.of(new Interval(0, 5))
));
```

### Knowledge Graphs

Load relationship data from GraphML files:

```java
Graph kb = Interpretation.loadKnowledgeBase("my_graph.graphml");
```

GraphML edges are automatically converted to facts. For example, an edge with label "Friends" from John to Mary becomes `Friends(John, Mary)`.

## Use Cases

JavaSense excels at temporal reasoning scenarios:

### Supply Chain Risk Analysis
Track how supplier disruptions propagate through a supply network over time.

### Social Network Dynamics
Model how influence, popularity, or information spreads through social connections.

### Customer Segmentation
Derive customer attributes (e.g., "trendy") based on ownership patterns and relationships.

### Temporal Knowledge Graphs
Reason over facts that change over time with complex temporal constraints.

## Examples

The project includes three complete examples:

- **[Main.java](demo/src/main/java/com/example/Main.java)**: Social popularity spreading through friendships
- **[MainAdvanced.java](demo/src/main/java/com/example/MainAdvanced.java)**: E-commerce customer attributes based on ownership
- **[MainSupplier.java](demo/src/main/java/com/example/MainSupplier.java)**: Supply chain disruption tracking

Run an example:

```bash
cd demo
mvn exec:java -Dexec.mainClass="com.example.Main"
```

## Rule Language Syntax

JavaSense uses Horn clause syntax with temporal extensions:

```
head(vars) [: interval] <-delay body_atom1(vars), body_atom2(vars), ...
```

**Components:**
- `head(vars)`: The fact to be inferred
- `: [start,end]`: Optional time interval when head is valid
- `<-delay`: Inference delay (0 = immediate, 1 = next timestep, etc.)
- `body_atom(vars)`: Conditions that must be satisfied

**Examples:**

```
# Basic inference
popular(x) <- popular(y), Friends(x,y)

# With 1 timestep delay
popular(x) <-1 popular(y), Friends(x,y)

# Head valid only in interval [0,5]
popular(x) : [0,5] <-1 popular(y), Friends(x,y)

# Multiple body atoms
trendy(x) <- owns(x,y), Car(y), owns(x,z), Pet(z)
```

## Loading Rules and Facts from Files

### From Rules File

Create a text file with one rule per line:

```
# rules.txt
popular(x) <-1 popular(y), Friends(x,y)
trendy(x) <- owns(x,y), Car(y)
```

Load it:

```java
JavaSense.addRulesFromFile("rules.txt");
```

### From Facts File

Create a text file with facts in CSV format:

```
# facts.txt
popular(Mary),initial_fact,0,2
popular(John),john_fact,1,3
```

Load it:

```java
JavaSense.addFactsFromFile("facts.txt");
```

## API Documentation

### JavaSense Class

Main API facade for the reasoning engine.

**Methods:**
- `addFact(Fact fact)`: Add a simple fact
- `addTimedFact(TimedFact fact)`: Add a fact with multiple time intervals
- `addFactsFromFile(String fileName)`: Load facts from a file
- `addRule(Rule rule)`: Add a reasoning rule
- `addRulesFromFile(String fileName)`: Load rules from a file
- `reason(Graph kb, int timesteps)`: Run inference and return results

### ReasoningInterpretation Class

Stores and retrieves inference results.

**Methods:**
- `getFactsAt(int timestep)`: Get all facts true at a specific timestep
- `getAllFacts()`: Get all derived facts across all timesteps

## Architecture

```
JavaSense (API Facade)
    ├── Reasoner (Forward Chaining Engine)
    │   ├── Rule (Rule definitions)
    │   ├── TimedFact (Facts with intervals)
    │   └── Atom (Logical predicates)
    ├── Graph (Knowledge graph wrapper)
    │   └── GraphToFactsConverter (GraphML → Facts)
    └── ReasoningInterpretation (Results)
```

## Performance

JavaSense is designed for:
- **Knowledge graphs**: Up to 10,000 nodes and 100,000 edges
- **Rules**: Up to 1,000 rules
- **Timesteps**: Typically 10-100 timesteps
- **Fact derivations**: Millions of derived facts

For larger-scale applications, consider optimizations like rule indexing and incremental reasoning.

## Comparison to PyReason

| Feature | JavaSense | PyReason |
|---------|-----------|----------|
| Language | Java | Python |
| Performance | High (JVM) | Moderate |
| Type Safety | Strong typing | Dynamic typing |
| Integration | Maven ecosystem | pip/conda |
| Temporal Logic | Full support | Full support |
| Graph Support | GraphML | GraphML, NetworkX |

## Roadmap

- [ ] Query language for filtering results
- [ ] Explanation/provenance tracking
- [ ] Negation-as-failure support
- [ ] Constraint handling (inequalities, arithmetic)
- [ ] Incremental reasoning
- [ ] Database connectors (SQL, Neo4j)
- [ ] REST API for remote inference
- [ ] Performance optimizations

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes with tests
4. Submit a pull request

## License

Apache License 2.0 - see [LICENSE](LICENSE) file for details.

## Support

- **Documentation**: [User Guide](docs/USER_GUIDE.md)
- **Issues**: [GitHub Issues](https://github.com/yourusername/javasense/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/javasense/discussions)

## Citation

If you use JavaSense in academic work, please cite:

```bibtex
@software{javasense2025,
  title={JavaSense: Temporal Logical Reasoning for Knowledge Graphs},
  author={Your Name},
  year={2025},
  url={https://github.com/yourusername/javasense}
}
```

## Acknowledgments

Built with [JGraphT](https://jgrapht.org/) for graph data structures and algorithms.

---

**JavaSense** - Bringing temporal reasoning to the JVM ecosystem.
