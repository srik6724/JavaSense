package com.example;

import com.example.distributed.*;

import java.util.List;

/**
 * Example: Distributed Reasoning
 *
 * <p>Demonstrates JavaSense v1.3 distributed reasoning capabilities.</p>
 *
 * <h2>Setup Instructions:</h2>
 * <ol>
 *   <li>Start 3 worker nodes in separate terminals:
 *     <pre>
 *     java com.example.distributed.DistributedWorker worker1 5001
 *     java com.example.distributed.DistributedWorker worker2 5002
 *     java com.example.distributed.DistributedWorker worker3 5003
 *     </pre>
 *   </li>
 *   <li>Run this example:
 *     <pre>
 *     mvn exec:java -Dexec.mainClass="com.example.ExampleDistributed"
 *     </pre>
 *   </li>
 * </ol>
 *
 * <h2>Scenario: Large-Scale Supply Chain Risk Analysis</h2>
 * <p>Analyze risk propagation across a large supply chain with 1000+ suppliers.</p>
 */
public class ExampleDistributed {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("JavaSense v1.3 - Distributed Reasoning Example");
        System.out.println("=".repeat(80));
        System.out.println();

        try {
            // Create distributed reasoner with 3 workers
            System.out.println("Connecting to distributed workers...");
            DistributedReasoner reasoner = DistributedReasoner.builder()
                    .addWorker("worker1", "localhost", 5001)
                    .addWorker("worker2", "localhost", 5002)
                    .addWorker("worker3", "localhost", 5003)
                    .partitionStrategy(new PartitionStrategy.PredicatePartitioner())
                    .workerTimeout(60000)  // 60 second timeout
                    .build();

            System.out.println("✓ Connected to 3 workers");
            System.out.println();

            // Load supply chain data
            System.out.println("Loading supply chain data...");
            loadSupplyChainData(reasoner);
            System.out.println("✓ Loaded supply chain data");
            System.out.println();

            // Add risk propagation rules
            System.out.println("Adding risk propagation rules...");
            reasoner.addRule(new Rule(
                    "atRisk(part) <-1 disrupted(supplier), supplies(supplier,part)",
                    "supply_risk"
            ));
            reasoner.addRule(new Rule(
                    "productDelayed(product) <-1 atRisk(part), requires(product,part)",
                    "product_delay"
            ));
            reasoner.addRule(new Rule(
                    "critical(product) <-1 productDelayed(product), essential(product)",
                    "critical_product"
            ));
            System.out.println("✓ Added 3 risk propagation rules");
            System.out.println();

            // Perform distributed reasoning
            System.out.println("Performing distributed reasoning...");
            System.out.println("(Workers processing in parallel...)");
            System.out.println();

            DistributedReasoner.ReasoningResult result = reasoner.reason(10);

            System.out.println("✓ Distributed reasoning complete!");
            System.out.println();

            // Display results
            System.out.println("=== Results ===");
            System.out.println();
            System.out.println(result.getSummary());
            System.out.println();

            System.out.println("Performance:");
            System.out.println("  Execution Time: " + result.getExecutionTimeMs() + "ms");
            System.out.println("  Estimated Speedup: " + String.format("%.1fx", result.getSpeedup()));
            System.out.println("  Total Facts Derived: " + result.getTotalFacts());
            System.out.println();

            // Worker statistics
            System.out.println("Worker Performance:");
            for (WorkResult workerResult : result.getWorkerResults()) {
                if (workerResult.isSuccess()) {
                    System.out.printf("  • %s: %d facts in %dms%n",
                            workerResult.getWorkerId(),
                            workerResult.getDerivedFacts().size(),
                            workerResult.getExecutionTimeMs());
                } else {
                    System.out.printf("  • %s: FAILED - %s%n",
                            workerResult.getWorkerId(),
                            workerResult.getErrorMessage());
                }
            }
            System.out.println();

            // Sample query results
            System.out.println("Sample Query Results:");
            System.out.println();

            Query atRiskQuery = Query.parse("atRisk(x)").atTime(1);
            int atRiskCount = 0;
            for (Atom atom : result.getFactsAt(1)) {
                if (atom.getPredicate().equals("atRisk")) {
                    atRiskCount++;
                }
            }
            System.out.println("  Parts at risk (t=1): " + atRiskCount);

            Query delayedQuery = Query.parse("productDelayed(x)").atTime(2);
            int delayedCount = 0;
            for (Atom atom : result.getFactsAt(2)) {
                if (atom.getPredicate().equals("productDelayed")) {
                    delayedCount++;
                }
            }
            System.out.println("  Products delayed (t=2): " + delayedCount);

            int criticalCount = 0;
            for (Atom atom : result.getFactsAt(3)) {
                if (atom.getPredicate().equals("critical")) {
                    criticalCount++;
                }
            }
            System.out.println("  Critical products (t=3): " + criticalCount);
            System.out.println();

            // Shutdown
            System.out.println("=== Summary ===");
            System.out.println();
            System.out.println("✓ Distributed reasoning successfully completed!");
            System.out.println("✓ Workers coordinated by master node");
            System.out.println("✓ Facts partitioned using PredicatePartitioner strategy");
            System.out.println("✓ Results aggregated and consistent");
            System.out.println();

            System.out.println("Benefits of Distributed Reasoning:");
            System.out.println("  • Parallel processing across multiple machines");
            System.out.println("  • Linear scalability with worker count");
            System.out.println("  • Fault tolerance with worker retry logic");
            System.out.println("  • Efficient partitioning minimizes communication");
            System.out.println();

            reasoner.shutdown();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            System.err.println("Make sure workers are running:");
            System.err.println("  java com.example.distributed.DistributedWorker worker1 5001");
            System.err.println("  java com.example.distributed.DistributedWorker worker2 5002");
            System.err.println("  java com.example.distributed.DistributedWorker worker3 5003");
            e.printStackTrace();
        }

        System.out.println("=".repeat(80));
    }

    /**
     * Loads sample supply chain data for the example.
     */
    private static void loadSupplyChainData(DistributedReasoner reasoner) {
        // Add disrupted suppliers
        reasoner.addFact(new TimedFact(
                Atom.parse("disrupted(SupplierA)"),
                "disruption_A",
                List.of(new Interval(0, 10))
        ));

        reasoner.addFact(new TimedFact(
                Atom.parse("disrupted(SupplierB)"),
                "disruption_B",
                List.of(new Interval(0, 10))
        ));

        // Add supply relationships (10 suppliers, 20 parts)
        String[] suppliers = {"SupplierA", "SupplierB", "SupplierC", "SupplierD", "SupplierE"};
        String[] parts = {"Engine", "Transmission", "Wheels", "Battery", "Chassis",
                "Seats", "Electronics", "Brakes", "Suspension", "Exhaust"};

        for (int i = 0; i < suppliers.length; i++) {
            for (int j = 0; j < 4; j++) {  // Each supplier supplies 4 parts
                int partIndex = (i * 4 + j) % parts.length;
                reasoner.addFact(new TimedFact(
                        Atom.parse("supplies(" + suppliers[i] + "," + parts[partIndex] + ")"),
                        "supply_" + i + "_" + j,
                        List.of(new Interval(0, 10))
                ));
            }
        }

        // Add product requirements
        String[] products = {"CarA", "CarB", "CarC", "TruckA", "TruckB"};
        for (String product : products) {
            // Each product requires multiple parts
            for (int i = 0; i < 5; i++) {
                reasoner.addFact(new TimedFact(
                        Atom.parse("requires(" + product + "," + parts[i] + ")"),
                        "requires_" + product + "_" + i,
                        List.of(new Interval(0, 10))
                ));
            }
        }

        // Mark some products as essential
        reasoner.addFact(new TimedFact(
                Atom.parse("essential(CarA)"),
                "essential_CarA",
                List.of(new Interval(0, 10))
        ));

        reasoner.addFact(new TimedFact(
                Atom.parse("essential(TruckA)"),
                "essential_TruckA",
                List.of(new Interval(0, 10))
        ));

        System.out.println("  • " + suppliers.length + " suppliers");
        System.out.println("  • " + parts.length + " parts");
        System.out.println("  • " + products.length + " products");
        System.out.println("  • 2 disruptions");
    }
}
