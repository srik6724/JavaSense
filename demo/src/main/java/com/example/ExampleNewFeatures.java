package com.example;

import com.example.integration.KafkaReasoner;

import java.util.List;

/**
 * Comprehensive Example: New JavaSense v1.1 Features
 *
 * <p>Demonstrates the new high-impact features:</p>
 * <ul>
 *   <li><b>Streaming Reasoning:</b> Real-time incremental updates</li>
 *   <li><b>Explainability:</b> Visual provenance and HTML reports</li>
 *   <li><b>Kafka Integration:</b> Stream reasoning from Kafka topics</li>
 *   <li><b>Rule Validation:</b> Comprehensive rule quality checks</li>
 * </ul>
 */
public class ExampleNewFeatures {

    public static void main(String[] args) throws Exception {
        System.out.println("=".repeat(80));
        System.out.println("JavaSense v1.1 - New Features Demo");
        System.out.println("=".repeat(80));
        System.out.println();

        // Demo 1: Streaming Reasoning
        demonstrateStreamingReasoning();

        System.out.println("\n" + "=".repeat(80) + "\n");

        // Demo 2: Explainability
        demonstrateExplainability();

        System.out.println("\n" + "=".repeat(80) + "\n");

        // Demo 3: Kafka Integration
        demonstrateKafkaIntegration();

        System.out.println("\n" + "=".repeat(80) + "\n");

        // Demo 4: Rule Validation
        demonstrateRuleValidation();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("Demo complete! 🎉");
        System.out.println("=".repeat(80));
    }

    /**
     * Demo 1: Streaming Reasoning with Real-Time Fraud Detection
     */
    private static void demonstrateStreamingReasoning() {
        System.out.println("📊 DEMO 1: Streaming Reasoning - Real-Time Fraud Detection\n");

        // Create streaming reasoner
        StreamingReasoner reasoner = new StreamingReasoner(100);

        // Add fraud detection rules
        reasoner.addRule(new Rule(
            "largeTransaction(x) <-0 transaction(x,amt), highAmount(amt)",
            "large_transaction_rule"
        ));

        reasoner.addRule(new Rule(
            "fraudDetected(x) <-1 largeTransaction(x), newAccount(x)",
            "fraud_detection_rule"
        ));

        // Register callback for fraud alerts
        reasoner.onNewFact("fraudDetected", fact -> {
            String account = fact.getArgs().get(0);
            System.out.println("  🚨 FRAUD ALERT: Account " + account + " flagged!");
        });

        // Simulate streaming facts
        System.out.println("Simulating transaction stream...\n");

        // Fact 1: Account A123 is new
        reasoner.addFactIncremental(new TimedFact(
            Atom.parse("newAccount(A123)"),
            "fact1",
            List.of(new Interval(0, 100))
        ));
        System.out.println("  ✓ Added: newAccount(A123)");

        // Fact 2: High amount threshold
        reasoner.addFactIncremental(new TimedFact(
            Atom.parse("highAmount(10000)"),
            "fact2",
            List.of(new Interval(0, 100))
        ));
        System.out.println("  ✓ Added: highAmount(10000)");

        // Fact 3: Large transaction on A123 (triggers fraud!)
        List<Provenance.AtomTimeKey> newDerivations = reasoner.addFactIncremental(new TimedFact(
            Atom.parse("transaction(A123,10000)"),
            "fact3",
            List.of(new Interval(5, 5))
        ));
        System.out.println("  ✓ Added: transaction(A123,10000)");
        System.out.println("  → Derived " + newDerivations.size() + " new facts");

        // Show statistics
        System.out.println("\nStatistics:");
        reasoner.getStatistics().forEach((key, value) ->
            System.out.println("  " + key + ": " + value)
        );
    }

    /**
     * Demo 2: Explainability - Why was fraud detected?
     */
    private static void demonstrateExplainability() {
        System.out.println("🔍 DEMO 2: Explainability - Understanding Decisions\n");

        // Create reasoner with provenance tracking
        OptimizedReasoner reasoner = new OptimizedReasoner();

        // Add rules
        reasoner.addRule(new Rule(
            "largeTransaction(x) <-0 transaction(x,10000)",
            "large_txn_rule"
        ));
        reasoner.addRule(new Rule(
            "fraudDetected(x) <-1 largeTransaction(x), newAccount(x)",
            "fraud_rule"
        ));

        // Add facts
        reasoner.addFact(new TimedFact(
            Atom.parse("transaction(A123,10000)"),
            "txn1",
            List.of(new Interval(5, 5))
        ));
        reasoner.addFact(new TimedFact(
            Atom.parse("newAccount(A123)"),
            "acct1",
            List.of(new Interval(0, 100))
        ));

        // Reason
        ReasoningInterpretation result = reasoner.reason(10);

        // Create explainability UI
        Provenance provenance = result.getProvenance();
        ExplainabilityUI ui = new ExplainabilityUI(provenance);

        // Explain why fraud was detected
        Atom fraudFact = Atom.parse("fraudDetected(A123)");
        String explanation = ui.explainWhy(fraudFact, 6);
        System.out.println(explanation);

        // Generate HTML report
        System.out.println("\nGenerating HTML report...");
        String html = ui.generateHTMLReport(fraudFact, 6);
        System.out.println("HTML report length: " + html.length() + " characters");
        System.out.println("(In production, save to file: fraud_report.html)");

        // Generate rule summary
        System.out.println("\n" + ui.generateRuleSummary());
    }

    /**
     * Demo 3: Kafka Integration (Mock)
     */
    private static void demonstrateKafkaIntegration() throws Exception {
        System.out.println("🌊 DEMO 3: Kafka Integration - Streaming from Topics\n");

        // Create Kafka reasoner
        KafkaReasoner kafkaReasoner = new KafkaReasoner.Builder()
            .topic("transactions")
            .parser(msg -> {
                // Parse JSON message to fact
                // Example: {"account": "A123", "amount": 10000, "time": 5}
                // In production, use real JSON parser (Jackson, Gson, etc.)
                if (msg.contains("A123")) {
                    return new TimedFact(
                        Atom.parse("transaction(A123,10000)"),
                        "kafka_txn",
                        List.of(new Interval(5, 5))
                    );
                }
                return null;
            })
            .maxTimesteps(100)
            .build();

        // Add fraud detection rules
        kafkaReasoner.addRule(new Rule(
            "fraudDetected(x) <-1 transaction(x,10000)",
            "kafka_fraud_rule"
        ));

        // Register callback
        kafkaReasoner.onNewFact("fraudDetected", fact -> {
            System.out.println("  🚨 Kafka Alert: Fraud detected for " + fact.getArgs().get(0));
        });

        // Start streaming (mock)
        kafkaReasoner.start();
        System.out.println("✓ Kafka consumer started");

        // Note: In production, Kafka messages would come from actual Kafka topics
        System.out.println("\nKafka reasoner ready to process messages from topic...");
        Thread.sleep(500);  // Simulate some runtime

        kafkaReasoner.stop();
        System.out.println("\n✓ Kafka consumer stopped");

        // Show statistics
        System.out.println("\nKafka Reasoner Statistics:");
        kafkaReasoner.getStatistics().forEach((key, value) ->
            System.out.println("  " + key + ": " + value)
        );
    }

    /**
     * Demo 4: Rule Validation - Detect Issues Before Reasoning
     */
    private static void demonstrateRuleValidation() {
        System.out.println("✅ DEMO 4: Rule Validation - Quality Checks\n");

        RuleValidator validator = new RuleValidator();

        // Add some good rules
        validator.addRule(new Rule(
            "safe(x) <- approved(x)",
            "safe_rule"
        ));

        // Add problematic rules

        // 1. Conflict: Same condition, different conclusions
        validator.addRule(new Rule(
            "unsafe(x) <- approved(x)",
            "unsafe_rule"
        ));

        // 2. Circular dependency
        validator.addRule(new Rule(
            "a(x) <- b(x)",
            "rule_a"
        ));
        validator.addRule(new Rule(
            "b(x) <- c(x)",
            "rule_b"
        ));
        validator.addRule(new Rule(
            "c(x) <- a(x)",
            "rule_c"
        ));

        // 3. Unsafe negation (unbound variable in negation)
        validator.addRule(new Rule(
            "risky(x) <- approved(x), not unknown(y)",
            "unsafe_negation_rule"
        ));

        // 4. Performance warning (no shared variables)
        validator.addRule(new Rule(
            "combined(x,y) <- data1(x), data2(y)",
            "cartesian_product_rule"
        ));

        // Add base facts
        validator.addBaseFact(new TimedFact(
            Atom.parse("approved(Alice)"),
            "base1",
            List.of(new Interval(0, 100))
        ));

        // Perform validation
        System.out.println("Validating rules...\n");
        RuleValidator.ValidationReport report = validator.validate();

        // Print report
        report.printReport();

        // Summary
        System.out.println("\n" + "=".repeat(70));
        System.out.println("Summary:");
        System.out.println("  Errors: " + report.getErrors().size());
        System.out.println("  Warnings: " + report.getWarnings().size());

        if (report.hasErrors()) {
            System.out.println("\n⚠️  FIX ERRORS BEFORE DEPLOYING TO PRODUCTION!");
        } else {
            System.out.println("\n✅ No critical errors - rules are safe to deploy");
        }
    }
}
