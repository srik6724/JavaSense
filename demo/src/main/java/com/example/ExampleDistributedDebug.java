package com.example;

import com.example.distributed.*;

import java.util.List;

/**
 * Debug version of ExampleDistributed to diagnose query issues.
 */
public class ExampleDistributedDebug {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("JavaSense v1.3 - Distributed Reasoning Debug");
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
                    .workerTimeout(60000)
                    .build();

            System.out.println("✓ Connected to 3 workers");
            System.out.println();

            // Load simple test data
            System.out.println("Loading test data...");

            // Add a disrupted supplier
            reasoner.addFact(new TimedFact(
                    Atom.parse("disrupted(SupplierA)"),
                    "disruption_A",
                    List.of(new Interval(0, 10))
            ));

            // Add supply relationship
            reasoner.addFact(new TimedFact(
                    Atom.parse("supplies(SupplierA,Engine)"),
                    "supply_A_Engine",
                    List.of(new Interval(0, 10))
            ));

            System.out.println("  • 1 disrupted supplier");
            System.out.println("  • 1 supply relationship");
            System.out.println("✓ Loaded test data");
            System.out.println();

            // Add risk propagation rule
            System.out.println("Adding risk propagation rule...");
            reasoner.addRule(new Rule(
                    "atRisk(part) <-1 disrupted(supplier), supplies(supplier,part)",
                    "supply_risk"
            ));
            System.out.println("✓ Added risk propagation rule");
            System.out.println();

            // Perform distributed reasoning
            System.out.println("Performing distributed reasoning...");
            DistributedReasoner.ReasoningResult result = reasoner.reason(10);

            System.out.println("✓ Distributed reasoning complete!");
            System.out.println();

            // Debug: Print all facts at each timestep
            System.out.println("=== Debug: Facts at Each Timestep ===");
            System.out.println();

            for (int t = 0; t <= 3; t++) {
                System.out.println("Timestep t=" + t + ":");
                int count = 0;
                for (Atom atom : result.getFactsAt(t)) {
                    System.out.println("  - " + atom);
                    count++;
                }
                System.out.println("  Total: " + count + " facts");
                System.out.println();
            }

            // Debug: Print worker results
            System.out.println("=== Debug: Worker Results ===");
            System.out.println();
            for (WorkResult workerResult : result.getWorkerResults()) {
                if (workerResult.isSuccess()) {
                    System.out.println("Worker " + workerResult.getWorkerId() + ":");
                    System.out.println("  Derived facts: " + workerResult.getDerivedFacts().size());
                    for (TimedFact fact : workerResult.getDerivedFacts()) {
                        System.out.println("    - " + fact.getAtom() + " @ " + fact.getIntervals());
                    }
                }
                System.out.println();
            }

            reasoner.shutdown();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=".repeat(80));
    }
}
