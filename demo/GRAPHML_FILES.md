# GraphML Files Reference

This directory contains GraphML knowledge graphs used by the JavaSense examples.

## Available Graphs

### 1. example.graphml
**Used by:** Main.java
**Domain:** Social Network
**Purpose:** Basic friendship network for popularity spreading demo

**Structure:**
- **Nodes:** People (Mary, John, Paul, Ringo, Alice, Bob)
- **Edges:** Friends relationships
- **Use Case:** Demonstrating basic temporal reasoning with social influence

---

### 2. advanced_graph.graphml
**Used by:** MainAdvanced.java
**Domain:** E-Commerce / Customer Segmentation
**Purpose:** Product ownership patterns for customer classification

**Structure:**
- **Nodes:**
  - Customers (Alice, Bob, Carol)
  - Products categorized as Car or Pet
- **Edges:** owns relationships
- **Use Case:** Multi-condition rules for customer segmentation

---

### 3. supplier_network.graphml
**Used by:** MainSupplier.java
**Domain:** Supply Chain
**Purpose:** Supplier-customer dependencies for risk analysis

**Structure:**
- **Nodes:** Suppliers and customers in a supply chain
- **Edges:** Supplier relationships (who supplies whom)
- **Use Case:** Cascading risk propagation through supply network

---

### 4. organization.graphml
**Used by:** ExampleSkillPropagation.java, ExampleAccessControl.java
**Domain:** Corporate/HR
**Purpose:** Organizational structure for skill transfer and access control

**Structure:**
- **Nodes:**
  - Employees: Alice, Bob, Carol, Dave
  - Managers: Eve, Frank
  - Guest: Grace
  - Resources: Database, AdminPanel, Reports, PublicDocs
  - Roles: Developer, Manager, Guest
- **Edges:**
  - Mentors (skill transfer via mentorship)
  - Collaborates (skill transfer via collaboration)
  - Manages (permission inheritance)
- **Use Cases:**
  - Skill propagation through mentorship and collaboration
  - Dynamic role-based access control
  - Permission inheritance through management hierarchy

---

### 5. contact_network.graphml
**Used by:** ExampleDiseaseSpreading.java
**Domain:** Public Health / Epidemiology
**Purpose:** Contact network for disease transmission modeling

**Structure:**
- **Nodes:** People (Patient0, Alice, Bob, Carol, Dave, Eve, Frank, Grace, Henry, Iris)
- **Edges:** Contact relationships (bidirectional)
- **Network Topology:**
  - Patient0 is a hub (connects to Alice, Bob, Carol)
  - Secondary contacts branch from primary contacts
  - Tertiary contacts form sparse connections
- **Use Case:** Epidemic modeling with incubation periods and contact tracing

---

### 6. transactions.graphml
**Used by:** ExampleFraudDetection.java
**Domain:** Financial Services / Banking
**Purpose:** Bank account transaction network for fraud detection

**Structure:**
- **Nodes:** Bank accounts (Account001 - Account010)
- **Edges:** Transfer relationships (money flow)
- **Patterns Encoded:**
  - Normal transfers (Account001 → Account002)
  - Rapid cascading transfers (Account006 → 007 → 008 → 009)
  - Circular patterns / layering (Account009 → 010 → 006)
  - Integration back to known accounts (Account009 → 001)
- **Use Case:** Pattern-based fraud detection, money laundering detection

---

### 7. customers.graphml
**Used by:** ExampleRecommendations.java
**Domain:** E-Commerce / Retail
**Purpose:** Customer purchase history for collaborative filtering

**Structure:**
- **Nodes:**
  - Customers: Alice, Bob, Carol, Dave, Eve, Frank
  - Products: Laptop, Phone, Tablet, Headphones, Keyboard, Mouse, Monitor, Charger
- **Edges:** purchased relationships
- **Customer Segments:**
  - Tech enthusiasts (Alice, Bob, Carol): Laptops, keyboards, etc.
  - Mobile users (Dave, Eve): Phones, tablets, chargers
  - Diverse buyers (Frank): Mix of products
- **Use Case:** Collaborative filtering recommendations ("customers who bought X also bought Y")

---

## GraphML Format

All graphs follow the GraphML standard format:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<graphml xmlns="http://graphml.graphdrawing.org/xmlns">
  <key id="label" for="node" attr.name="label" attr.type="string"/>
  <key id="label" for="edge" attr.name="label" attr.type="string"/>

  <graph id="GraphName" edgedefault="directed">
    <node id="NodeID">
      <data key="label">NodeType</data>
    </node>

    <edge id="EdgeID" source="SourceNode" target="TargetNode">
      <data key="label">RelationType</data>
    </edge>
  </graph>
</graphml>
```

## Creating Custom Graphs

### Using Graph Editing Tools:
- **yEd** (https://www.yworks.com/products/yed) - Free, powerful graph editor
- **Gephi** (https://gephi.org/) - Graph visualization and analysis
- **Cytoscape** (https://cytoscape.org/) - Network visualization

### Programmatically:
```java
// JavaSense can convert any GraphML file to facts
Graph kb = Interpretation.loadKnowledgeBase("your_graph.graphml");

// Edges become predicates
// Edge: <edge source="A" target="B"><data key="label">knows</data></edge>
// Becomes fact: knows(A, B)
```

## Graph Design Best Practices

1. **Node IDs**: Use descriptive IDs (e.g., "Alice" not "n1")
2. **Edge Labels**: Use clear relationship names (e.g., "Manages", "Transfer")
3. **Graph Size**: Start small (10-20 nodes) for testing
4. **Directionality**: Choose directed/undirected based on relationship semantics
5. **Node Types**: Use node labels to distinguish entity types

## Loading Graphs in Code

```java
// Load graph
Graph kb = Interpretation.loadKnowledgeBase("path/to/graph.graphml");

// Graph edges automatically become facts
// No need to manually add edge facts

// Add additional facts as needed
JavaSense.addFact(new Fact("infected(Patient0)", "seed", 0, 1));

// Run reasoning
ReasoningInterpretation result = JavaSense.reason(kb, timesteps);
```

## Troubleshooting

### Graph not loading?
- Check file path (relative to working directory)
- Validate XML syntax
- Ensure GraphML schema compliance

### Edges not becoming facts?
- Verify edge labels are set: `<data key="label">RelationName</data>`
- Check edge direction (source/target)

### Performance issues?
- Large graphs (>1000 nodes) may slow down reasoning
- Consider graph partitioning or sampling
- Use sparse graphs for better performance

---

## Contributing New Graphs

When adding a new example:
1. Create a descriptive GraphML file
2. Document it in this file
3. Reference it in EXAMPLES.md
4. Include sample facts/rules in the example code

---

**Need help?** See the [User Guide](../docs/USER_GUIDE.md) or open an issue.
