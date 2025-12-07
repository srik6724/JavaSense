package com.example;

import java.util.*;

/**
 * Real-World Example: Honda Supply Chain Network Analysis
 *
 * <p>Analyzes the actual Honda supply chain network (JP3854600008_honda.graphml)
 * with 10,893 companies and 47,247 supplier relationships.</p>
 *
 * <p>This demonstrates JavaSense's ability to perform temporal reasoning on
 * large-scale, real-world graphs.</p>
 *
 * <h2>Network Statistics:</h2>
 * <ul>
 *   <li>Nodes: 10,893 companies (suppliers, manufacturers, distributors)</li>
 *   <li>Edges: 47,247 supply relationships</li>
 *   <li>Edge types: CAPEX, COGS, SG&A</li>
 *   <li>Attributes: company name, market cap, GICS sector, value, cost</li>
 * </ul>
 *
 * <h2>Analysis Scenarios:</h2>
 * <ul>
 *   <li>Disruption propagation (natural disaster, bankruptcy)</li>
 *   <li>Critical supplier identification (single points of failure)</li>
 *   <li>Cascading risk analysis</li>
 *   <li>Alternative supplier discovery</li>
 *   <li>Supply chain resilience scoring</li>
 * </ul>
 */
public class ExampleHondaSupplyChain {

    public static void main(String[] args) {
        System.out.println("=== Honda Supply Chain Network Analysis ===\n");
        System.out.println("Network: 10,893 companies, 47,247 relationships");
        System.out.println("Source: JP3854600008_honda.graphml\n");

        // Load the massive real-world supply chain
        Graph kb = Interpretation.loadKnowledgeBase("JP3854600008_honda.graphml");

        // Choose analysis type
        String analysisType = "disruption"; // Options: disruption, critical, resilience

        switch (analysisType) {
            case "disruption":
                analyzeDisruptionPropagation(kb);
                break;
            case "critical":
                identifyCriticalSuppliers(kb);
                break;
            case "resilience":
                assessSupplyChainResilience(kb);
                break;
            default:
                System.out.println("Unknown analysis type: " + analysisType);
        }
    }

    /**
     * Scenario 1: Disruption Propagation Analysis
     *
     * Simulates a major supplier disruption (e.g., natural disaster, bankruptcy)
     * and tracks how the impact cascades through the supply network over time.
     */
    private static void analyzeDisruptionPropagation(Graph kb) {
        System.out.println("--- Scenario 1: Disruption Propagation ---\n");
        System.out.println("Simulating disruption at a major steel supplier...\n");

        // Initial disruption: Major steel supplier goes offline
        // (Kyoei Steel Ltd - node n3, market cap $91M)
        JavaSense.addFact(new Fact("disrupted(n3)", "steel_disruption", 1, 20));

        // Additional disruptions for compound scenario
        JavaSense.addFact(new Fact("disrupted(n9)", "battery_shortage", 5, 15));  // Samsung SDI

        // Rule 1: Direct impact - suppliers can't deliver if disrupted
        JavaSense.addRule(new Rule(
            "cantDeliver(x) <-1 disrupted(x)",
            "direct_impact"
        ));

        // Rule 2: Upstream impact - customers affected by disrupted suppliers
        // If X supplies Y via COGS, and X is disrupted, then Y is at risk
        JavaSense.addRule(new Rule(
            "atRisk(y) <-1 cantDeliver(x), COGS(x,y)",
            "upstream_risk"
        ));

        // Rule 3: Cascading disruption - at-risk companies may also be disrupted
        // (after 2 timesteps without alternative suppliers)
        JavaSense.addRule(new Rule(
            "disrupted(y) <-2 atRisk(y), atRisk(y)",  // Double condition = persistent risk
            "cascading_disruption"
        ));

        // Rule 4: High-value relationships are critical
        JavaSense.addRule(new Rule(
            "criticalRisk(y) <-1 cantDeliver(x), COGS(x,y), highValue(x,y)",
            "critical_relationship_risk"
        ));

        // Rule 5: CAPEX disruptions affect long-term capacity
        JavaSense.addRule(new Rule(
            "capacityRisk(y) <-1 disrupted(x), CAPEX(x,y)",
            "capacity_impact"
        ));

        // Run temporal analysis - see how disruption spreads over 20 days
        ReasoningInterpretation result = JavaSense.reason(kb, 20);

        // Analyze results
        System.out.println("=== Disruption Timeline ===");

        for (int t = 0; t <= 20; t++) {
            Set<Atom> facts = result.getFactsAt(t);

            long disrupted = facts.stream()
                .filter(f -> f.getPredicate().equals("disrupted"))
                .count();

            long atRisk = facts.stream()
                .filter(f -> f.getPredicate().equals("atRisk"))
                .count();

            long criticalRisk = facts.stream()
                .filter(f -> f.getPredicate().equals("criticalRisk"))
                .count();

            if (disrupted > 0 || atRisk > 0 || criticalRisk > 0) {
                System.out.println(String.format(
                    "Day %2d: %d disrupted, %d at risk, %d critical",
                    t, disrupted, atRisk, criticalRisk
                ));
            }
        }

        // Deep dive at specific timesteps
        System.out.println("\n=== Impact Analysis at Day 10 ===");
        analyzeImpactAtTime(result, 10);

        // Use Query API to find most affected sectors
        System.out.println("\n=== Most Affected Companies (Day 10) ===");
        Query q = Query.parse("atRisk(x)").atTime(10);
        List<QueryResult> affected = q.execute(result);

        System.out.println("Total companies at risk: " + affected.size());
        if (affected.size() > 0) {
            System.out.println("Sample affected: " + affected.subList(0, Math.min(10, affected.size())));
        }

        // Provenance: Explain why a specific company is disrupted
        System.out.println("\n=== Provenance Example ===");
        if (!affected.isEmpty()) {
            QueryResult sample = affected.get(0);
            Atom affectedCompany = sample.getFact();
            System.out.println("Explaining: " + affectedCompany);
            System.out.println(result.explain(affectedCompany, 10));
        }
    }

    /**
     * Scenario 2: Critical Supplier Identification
     *
     * Identifies single points of failure - suppliers whose disruption
     * would have the most severe impact on the network.
     */
    private static void identifyCriticalSuppliers(Graph kb) {
        System.out.println("--- Scenario 2: Critical Supplier Identification ---\n");

        // Rule 1: A supplier is critical if many companies depend on it
        JavaSense.addRule(new Rule(
            "hasSupplier(y,x) <- COGS(x,y)",
            "identify_suppliers"
        ));

        // Rule 2: Count dependencies (simplified - in practice use aggregation)
        JavaSense.addRule(new Rule(
            "dependencyCount(x) <-1 hasSupplier(y1,x), hasSupplier(y2,x)",
            "count_dependencies"
        ));

        // Rule 3: Critical if high dependency count
        JavaSense.addRule(new Rule(
            "criticalSupplier(x) <-1 dependencyCount(x)",
            "mark_critical"
        ));

        // Rule 4: High-value transactions indicate criticality
        JavaSense.addRule(new Rule(
            "criticalSupplier(x) <-1 COGS(x,y), highValue(x,y)",
            "high_value_critical"
        ));

        ReasoningInterpretation result = JavaSense.reason(kb, 5);

        System.out.println("=== Critical Suppliers ===");
        Query q = Query.parse("criticalSupplier(x)").atTime(1);
        List<QueryResult> critical = q.execute(result);

        System.out.println("Identified " + critical.size() + " critical suppliers");
        System.out.println("Top critical suppliers:");
        critical.stream().limit(20).forEach(r ->
            System.out.println("  " + r.getBinding("x"))
        );

        // Use ConflictDetector to find redundant suppliers (good for resilience)
        System.out.println("\n=== Redundancy Analysis ===");
        System.out.println("Companies with multiple suppliers for same input:");
        System.out.println("(High redundancy = better resilience)");
        // ... analyze supply redundancy
    }

    /**
     * Scenario 3: Supply Chain Resilience Assessment
     *
     * Scores the overall resilience of the supply chain by identifying
     * vulnerabilities, alternative paths, and redundancies.
     */
    private static void assessSupplyChainResilience(Graph kb) {
        System.out.println("--- Scenario 3: Supply Chain Resilience Assessment ---\n");

        // Rule 1: Company is vulnerable if it has a single critical supplier
        JavaSense.addRule(new Rule(
            "vulnerable(y) <-1 COGS(x,y), not hasAlternative(y,x)",
            "single_source_vulnerability"
        ));

        // Rule 2: Resilient if multiple suppliers for same category
        JavaSense.addRule(new Rule(
            "resilient(y) <-1 COGS(x1,y), COGS(x2,y)",  // Multiple suppliers
            "multi_source_resilience"
        ));

        // Rule 3: Diversified if suppliers from different sectors
        JavaSense.addRule(new Rule(
            "diversified(y) <-1 COGS(x1,y), COGS(x2,y), differentSector(x1,x2)",
            "sector_diversification"
        ));

        // Rule 4: Geographic risk if suppliers concentrated in one region
        JavaSense.addRule(new Rule(
            "geographicRisk(y) <-1 COGS(x1,y), COGS(x2,y), sameRegion(x1,x2)",
            "geographic_concentration"
        ));

        ReasoningInterpretation result = JavaSense.reason(kb, 5);

        System.out.println("=== Resilience Metrics ===");

        Query vulnerableQ = Query.parse("vulnerable(x)").atTime(1);
        Query resilientQ = Query.parse("resilient(x)").atTime(1);
        Query diversifiedQ = Query.parse("diversified(x)").atTime(1);

        List<QueryResult> vulnerable = vulnerableQ.execute(result);
        List<QueryResult> resilient = resilientQ.execute(result);
        List<QueryResult> diversified = diversifiedQ.execute(result);

        System.out.println("Vulnerable companies: " + vulnerable.size());
        System.out.println("Resilient companies: " + resilient.size());
        System.out.println("Diversified companies: " + diversified.size());

        double resilienceScore = (double) resilient.size() /
                                (vulnerable.size() + resilient.size()) * 100;

        System.out.println(String.format("\nOverall Resilience Score: %.1f%%", resilienceScore));

        // Recommendations
        System.out.println("\n=== Recommendations ===");
        if (resilienceScore < 50) {
            System.out.println("⚠️  LOW RESILIENCE - Urgent action needed");
            System.out.println("   - Identify alternative suppliers for critical components");
            System.out.println("   - Diversify supplier base across regions");
            System.out.println("   - Build strategic inventory buffers");
        } else if (resilienceScore < 75) {
            System.out.println("⚠️  MODERATE RESILIENCE - Improvements recommended");
            System.out.println("   - Review single-source dependencies");
            System.out.println("   - Consider secondary suppliers for high-risk items");
        } else {
            System.out.println("✅ HIGH RESILIENCE - Supply chain is well-diversified");
            System.out.println("   - Maintain current supplier diversity");
            System.out.println("   - Continue monitoring for emerging risks");
        }
    }

    // Helper method
    private static void analyzeImpactAtTime(ReasoningInterpretation result, int time) {
        Set<Atom> facts = result.getFactsAt(time);

        Map<String, Long> predicateCounts = new HashMap<>();
        for (Atom fact : facts) {
            predicateCounts.merge(fact.getPredicate(), 1L, Long::sum);
        }

        System.out.println("Fact distribution:");
        predicateCounts.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(10)
            .forEach(e -> System.out.println("  " + e.getKey() + ": " + e.getValue()));
    }

    /**
     * BONUS: Constraint-based validation
     */
    private static void validateSupplyChainConstraints(ReasoningInterpretation result) {
        System.out.println("\n--- Supply Chain Constraint Validation ---\n");

        ConstraintValidator validator = new ConstraintValidator();

        // Constraint 1: No circular dependencies (A supplies B, B supplies A)
        validator.addConstraint(new Constraint(
            "no_circular_supply",
            "Circular supply relationships detected",
            (facts, time) -> {
                // Check for cycles in supply graph
                // Simplified version - real implementation would use graph algorithms
                return true;
            },
            true  // hard constraint
        ));

        // Constraint 2: Critical suppliers must have backups
        validator.addConstraint(new Constraint(
            "critical_supplier_backup",
            "Critical suppliers without backups",
            (facts, time) -> {
                long criticalWithoutBackup = facts.stream()
                    .filter(f -> f.getPredicate().equals("criticalSupplier"))
                    .filter(f -> !facts.contains(Atom.parse("hasBackup(" + f.getArgs().get(0) + ")")))
                    .count();
                return criticalWithoutBackup == 0;
            },
            false  // soft constraint (warning)
        ));

        ConstraintValidator.ValidationResult validationResult = validator.validate(result);
        validationResult.display();
    }
}
