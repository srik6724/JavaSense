package com.example;

import java.util.List;

/**
 * Example: Probabilistic Reasoning
 *
 * <p>Demonstrates reasoning with uncertain facts and rules, with automatic
 * probability propagation through inference chains.</p>
 *
 * <h2>Scenario: Supply Chain Risk Assessment</h2>
 * <ul>
 *   <li>Uncertain alerts about supplier disruptions (70-90% confidence)</li>
 *   <li>Supply chain relationships (certain facts)</li>
 *   <li>Risk propagation rules with confidence levels</li>
 *   <li>Filter results by probability threshold</li>
 * </ul>
 */
public class ExampleProbabilistic {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("JavaSense v1.2 - Probabilistic Reasoning Example");
        System.out.println("=".repeat(80));
        System.out.println();

        // === SCENARIO: Supply Chain Risk Assessment ===
        System.out.println("Scenario: Supply Chain Risk Assessment with Uncertain Data");
        System.out.println("-".repeat(80));
        System.out.println();

        ProbabilisticReasoner reasoner = new ProbabilisticReasoner();

        // Add uncertain disruption alerts
        System.out.println("Loading disruption alerts (with uncertainty)...");

        reasoner.addProbabilisticFact(new ProbabilisticFact(
            Atom.parse("disrupted(ACME)"),
            "alert_001",
            0.8,  // 80% confidence supplier is disrupted
            List.of(new Interval(0, 10))
        ));

        reasoner.addProbabilisticFact(new ProbabilisticFact(
            Atom.parse("disrupted(GLOBEX)"),
            "alert_002",
            0.6,  // 60% confidence
            List.of(new Interval(0, 10))
        ));

        System.out.println("  • P(disrupted(ACME)) = 0.80");
        System.out.println("  • P(disrupted(GLOBEX)) = 0.60");
        System.out.println();

        // Add certain supply chain relationships
        System.out.println("Loading supply chain relationships (certain)...");

        reasoner.addFact(new TimedFact(
            Atom.parse("supplies(ACME,ENGINE)"),
            "supply_001",
            List.of(new Interval(0, 10))
        ));

        reasoner.addFact(new TimedFact(
            Atom.parse("supplies(GLOBEX,WHEEL)"),
            "supply_002",
            List.of(new Interval(0, 10))
        ));

        reasoner.addFact(new TimedFact(
            Atom.parse("supplies(INITECH,BATTERY)"),
            "supply_003",
            List.of(new Interval(0, 10))
        ));

        reasoner.addFact(new TimedFact(
            Atom.parse("dependsOn(CAR,ENGINE)"),
            "dep_001",
            List.of(new Interval(0, 10))
        ));

        reasoner.addFact(new TimedFact(
            Atom.parse("dependsOn(CAR,WHEEL)"),
            "dep_002",
            List.of(new Interval(0, 10))
        ));

        System.out.println("  • supplies(ACME, ENGINE) - certain");
        System.out.println("  • supplies(GLOBEX, WHEEL) - certain");
        System.out.println("  • supplies(INITECH, BATTERY) - certain");
        System.out.println("  • dependsOn(CAR, ENGINE) - certain");
        System.out.println("  • dependsOn(CAR, WHEEL) - certain");
        System.out.println();

        // Add probabilistic rules
        System.out.println("Adding probabilistic rules...");

        // Rule 1: Part at risk if supplier is disrupted (90% confidence)
        reasoner.addProbabilisticRule(new ProbabilisticRule(
            "atRisk(part) <-1 disrupted(supplier), supplies(supplier,part)",
            "risk_propagation",
            0.9  // 90% confidence in this rule
        ));

        // Rule 2: Product delayed if critical part at risk (95% confidence)
        reasoner.addProbabilisticRule(new ProbabilisticRule(
            "delayed(product) <-1 atRisk(part), dependsOn(product,part)",
            "delay_propagation",
            0.95  // 95% confidence
        ));

        // Rule 3: Critical situation if multiple parts at risk (certain rule)
        reasoner.addRule(new Rule(
            "critical(product) <-2 atRisk(part1), atRisk(part2), " +
            "dependsOn(product,part1), dependsOn(product,part2)",
            "critical_assessment"
        ));

        System.out.println("  • Risk propagation rule (confidence: 0.90)");
        System.out.println("  • Delay propagation rule (confidence: 0.95)");
        System.out.println("  • Critical assessment rule (certain)");
        System.out.println();

        // Set probability threshold
        double threshold = 0.5;
        reasoner.setMinProbabilityThreshold(threshold);
        System.out.println("Setting probability threshold: " + threshold);
        System.out.println("  (Only show facts with P >= 0.5)");
        System.out.println();

        // Perform probabilistic reasoning
        System.out.println("Performing probabilistic reasoning...");
        ProbabilisticReasoner.ProbabilisticInterpretation result = reasoner.reason(10);
        System.out.println("✓ Reasoning complete");
        System.out.println();

        // Query results with probabilities
        System.out.println("=== Results with Probabilities ===");
        System.out.println();

        // Check parts at risk
        System.out.println("Parts at Risk:");
        Atom atRiskEngine = Atom.parse("atRisk(ENGINE)");
        Atom atRiskWheel = Atom.parse("atRisk(WHEEL)");
        Atom atRiskBattery = Atom.parse("atRisk(BATTERY)");

        double probEngine = result.getProbability(atRiskEngine, 1);
        double probWheel = result.getProbability(atRiskWheel, 1);
        double probBattery = result.getProbability(atRiskBattery, 1);

        if (probEngine > 0) {
            System.out.println("  • ENGINE");
            System.out.println("      P(atRisk) = " + String.format("%.2f", probEngine));
            System.out.println("      Calculation: P(disrupted(ACME)) × P(rule)");
            System.out.println("                   = 0.80 × 0.90 = 0.72");
            printRiskLevel(probEngine);
        }

        if (probWheel > 0) {
            System.out.println("  • WHEEL");
            System.out.println("      P(atRisk) = " + String.format("%.2f", probWheel));
            System.out.println("      Calculation: P(disrupted(GLOBEX)) × P(rule)");
            System.out.println("                   = 0.60 × 0.90 = 0.54");
            printRiskLevel(probWheel);
        }

        if (probBattery > 0) {
            System.out.println("  • BATTERY");
            System.out.println("      P(atRisk) = " + String.format("%.2f", probBattery));
            printRiskLevel(probBattery);
        }

        if (probEngine == 0 && probWheel == 0 && probBattery == 0) {
            System.out.println("  (No parts above threshold)");
        }

        System.out.println();

        // Check products delayed
        System.out.println("Products Delayed:");
        Atom delayedCar = Atom.parse("delayed(CAR)");
        double probDelayedCar = result.getProbability(delayedCar, 2);

        if (probDelayedCar > 0) {
            System.out.println("  • CAR");
            System.out.println("      P(delayed) = " + String.format("%.2f", probDelayedCar));
            System.out.println("      Calculation: P(atRisk(ENGINE)) × P(delay_rule)");
            System.out.println("                   = 0.72 × 0.95 = 0.68");
            printDelayLevel(probDelayedCar);
        } else {
            System.out.println("  (No products above threshold)");
        }

        System.out.println();

        // Check critical situations
        System.out.println("Critical Situations:");
        Atom criticalCar = Atom.parse("critical(CAR)");
        double probCritical = result.getProbability(criticalCar, 3);

        if (probCritical > 0) {
            System.out.println("  • CAR is CRITICAL");
            System.out.println("      P(critical) = " + String.format("%.2f", probCritical));
            System.out.println("      Reason: Multiple parts at risk (ENGINE + WHEEL)");
            System.out.println("      Calculation: P(atRisk(ENGINE)) × P(atRisk(WHEEL))");
            System.out.println("                   = 0.72 × 0.54 = 0.39");
        } else {
            System.out.println("  (No critical situations above threshold)");
        }

        System.out.println();

        // Show facts above different thresholds
        System.out.println("=== Threshold Analysis ===");
        System.out.println();

        double[] thresholds = {0.3, 0.5, 0.7, 0.9};
        for (double t : thresholds) {
            int count = result.getFactsAboveThreshold(t, 2).size();
            System.out.println("Facts with P >= " + String.format("%.1f", t) + ": " + count);
        }

        System.out.println();

        // Summary
        System.out.println("=== Summary ===");
        System.out.println();
        System.out.println("Uncertainty Sources:");
        System.out.println("  • Disruption alerts from sensors/ML models (60-80% confidence)");
        System.out.println("  • Risk propagation rules (90-95% confidence)");
        System.out.println();
        System.out.println("Probability Propagation:");
        System.out.println("  • Multiplies probabilities along inference chains");
        System.out.println("  • P(derived) = P(rule) × P(premise1) × P(premise2) × ...");
        System.out.println("  • Uncertainty compounds through multi-step reasoning");
        System.out.println();
        System.out.println("Benefits:");
        System.out.println("  ✓ Quantify confidence in derived conclusions");
        System.out.println("  ✓ Filter low-confidence facts to reduce false positives");
        System.out.println("  ✓ Integrate ML/sensor data with logical reasoning");
        System.out.println("  ✓ Make risk-aware decisions");
        System.out.println();

        System.out.println("=".repeat(80));
    }

    private static void printRiskLevel(double probability) {
        if (probability >= 0.8) {
            System.out.println("      Risk Level: 🔴 HIGH");
        } else if (probability >= 0.6) {
            System.out.println("      Risk Level: 🟡 MEDIUM");
        } else if (probability >= 0.4) {
            System.out.println("      Risk Level: 🟢 LOW");
        } else {
            System.out.println("      Risk Level: ⚪ MINIMAL");
        }
    }

    private static void printDelayLevel(double probability) {
        if (probability >= 0.7) {
            System.out.println("      Delay Likelihood: Very High");
        } else if (probability >= 0.5) {
            System.out.println("      Delay Likelihood: High");
        } else if (probability >= 0.3) {
            System.out.println("      Delay Likelihood: Medium");
        } else {
            System.out.println("      Delay Likelihood: Low");
        }
    }
}
