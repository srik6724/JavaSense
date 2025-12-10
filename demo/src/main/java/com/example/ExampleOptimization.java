package com.example;

import java.util.List;

/**
 * Example: Constraint Optimization Reasoning
 *
 * <p>Demonstrates finding optimal solutions that maximize/minimize objectives
 * while satisfying hard and soft constraints.</p>
 *
 * <h2>Scenario: Supplier Selection Optimization</h2>
 * <ul>
 *   <li>Multiple suppliers with different costs and quality ratings</li>
 *   <li>Hard constraint: Quality must be above 0.7</li>
 *   <li>Soft constraint: Prefer suppliers with fast delivery</li>
 *   <li>Objective: Minimize total cost</li>
 * </ul>
 */
public class ExampleOptimization {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("JavaSense v1.2 - Constraint Optimization Example");
        System.out.println("=".repeat(80));
        System.out.println();

        // === SCENARIO: Supplier Selection Optimization ===
        System.out.println("Scenario: Supplier Selection Optimization");
        System.out.println("-".repeat(80));
        System.out.println();

        OptimizationReasoner reasoner = new OptimizationReasoner();

        // Add supplier facts: supplier(name, cost, quality, deliveryDays)
        System.out.println("Loading supplier data...");

        // Supplier A: Low cost, medium quality, slow delivery
        reasoner.addFact(new TimedFact(
            Atom.parse("supplier(SupplierA,100,0.75,10)"),
            "supplier_a",
            List.of(new Interval(0, 100))
        ));

        // Supplier B: Medium cost, high quality, fast delivery
        reasoner.addFact(new TimedFact(
            Atom.parse("supplier(SupplierB,150,0.95,3)"),
            "supplier_b",
            List.of(new Interval(0, 100))
        ));

        // Supplier C: High cost, very high quality, very fast delivery
        reasoner.addFact(new TimedFact(
            Atom.parse("supplier(SupplierC,200,0.98,1)"),
            "supplier_c",
            List.of(new Interval(0, 100))
        ));

        // Supplier D: Very low cost, low quality (below threshold!)
        reasoner.addFact(new TimedFact(
            Atom.parse("supplier(SupplierD,50,0.60,7)"),
            "supplier_d",
            List.of(new Interval(0, 100))
        ));

        // Supplier E: Low cost, acceptable quality, medium delivery
        reasoner.addFact(new TimedFact(
            Atom.parse("supplier(SupplierE,120,0.80,5)"),
            "supplier_e",
            List.of(new Interval(0, 100))
        ));

        System.out.println("✓ Loaded 5 suppliers");
        System.out.println();

        // Print supplier details
        System.out.println("Supplier Details:");
        System.out.println("  SupplierA: Cost=$100, Quality=0.75, Delivery=10 days");
        System.out.println("  SupplierB: Cost=$150, Quality=0.95, Delivery=3 days");
        System.out.println("  SupplierC: Cost=$200, Quality=0.98, Delivery=1 day");
        System.out.println("  SupplierD: Cost=$50,  Quality=0.60, Delivery=7 days ⚠️ Low quality!");
        System.out.println("  SupplierE: Cost=$120, Quality=0.80, Delivery=5 days");
        System.out.println();

        // Add rules
        System.out.println("Adding optimization rules...");

        reasoner.addRule(new Rule(
            "selected(name,cost,quality,delivery) <-0 supplier(name,cost,quality,delivery)",
            "selection_rule"
        ));

        System.out.println("✓ Added selection rules");
        System.out.println();

        // Add hard constraint: Quality must be > 0.7
        System.out.println("Adding constraints...");
        System.out.println("  Hard Constraint: Quality > 0.7 (eliminates SupplierD)");

        reasoner.addHardConstraint("selected", atom -> {
            // Extract quality (3rd argument)
            if (atom.getArgs().size() >= 3) {
                try {
                    double quality = Double.parseDouble(atom.getArgs().get(2));
                    return quality > 0.7;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return false;
        });

        // Add soft constraint: Prefer fast delivery (< 5 days)
        System.out.println("  Soft Constraint: Delivery < 5 days (penalty=$20 if violated)");

        reasoner.addSoftConstraint("selected", 20.0, atom -> {
            // Extract delivery days (4th argument)
            if (atom.getArgs().size() >= 4) {
                try {
                    double deliveryDays = Double.parseDouble(atom.getArgs().get(3));
                    return deliveryDays < 5;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return false;
        });

        System.out.println("✓ Added constraints");
        System.out.println();

        // Set objective: Minimize cost (2nd argument)
        System.out.println("Setting objective: MINIMIZE cost");
        reasoner.setObjectiveFunction(OptimizationReasoner.ObjectiveType.MINIMIZE, atom -> {
            if (atom.getPredicate().equals("selected") && atom.getArgs().size() >= 2) {
                try {
                    return Double.parseDouble(atom.getArgs().get(1));
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            }
            return 0.0;
        });
        System.out.println();

        // Perform optimization
        System.out.println("Performing optimization...");
        OptimizationReasoner.OptimizationResult result = reasoner.optimize(10);
        System.out.println("✓ Optimization complete");
        System.out.println();

        // Display results
        System.out.println("=== Optimization Results ===");
        System.out.println();

        if (!result.hasFeasibleSolution()) {
            System.out.println("❌ No feasible solution found!");
            return;
        }

        OptimizationReasoner.Solution best = result.getBestSolution();
        System.out.println("✓ Best Solution Found:");
        System.out.println("  Objective Value (Cost + Penalties): $" + String.format("%.2f", best.getScore()));
        System.out.println("  Timestep: " + best.getTimestep());
        System.out.println("  Total Facts: " + best.getFacts().size());
        System.out.println();

        // Extract selected supplier
        List<Atom> selectedSuppliers = best.getFactsWithPredicate("selected");
        if (!selectedSuppliers.isEmpty()) {
            System.out.println("Selected Supplier(s):");
            for (Atom supplier : selectedSuppliers) {
                List<String> supplierArgs = supplier.getArgs();
                if (supplierArgs.size() >= 4) {
                    String name = supplierArgs.get(0);
                    String cost = supplierArgs.get(1);
                    String quality = supplierArgs.get(2);
                    String delivery = supplierArgs.get(3);

                    System.out.println("  🏆 " + name);
                    System.out.println("      Cost: $" + cost);
                    System.out.println("      Quality: " + quality);
                    System.out.println("      Delivery: " + delivery + " days");

                    // Check soft constraint
                    double deliveryDays = Double.parseDouble(delivery);
                    if (deliveryDays >= 5) {
                        System.out.println("      ⚠️ Penalty: Delivery time >= 5 days (+$20)");
                    } else {
                        System.out.println("      ✓ Fast delivery!");
                    }
                }
            }
        }
        System.out.println();

        // Show all feasible solutions
        System.out.println("=== All Feasible Solutions ===");
        System.out.println();

        List<OptimizationReasoner.Solution> allSolutions = result.getAllSolutions();
        System.out.println("Found " + allSolutions.size() + " feasible solutions:");
        System.out.println();

        int rank = 1;
        for (OptimizationReasoner.Solution solution : result.getTopK(5)) {
            List<Atom> suppliers = solution.getFactsWithPredicate("selected");
            if (!suppliers.isEmpty()) {
                Atom supplier = suppliers.get(0);
                List<String> supplierArgs = supplier.getArgs();
                if (supplierArgs.size() >= 4) {
                    String name = supplierArgs.get(0);
                    double cost = Double.parseDouble(supplierArgs.get(1));
                    double quality = Double.parseDouble(supplierArgs.get(2));
                    int delivery = Integer.parseInt(supplierArgs.get(3));

                    String badge = (rank == 1) ? "🥇" : (rank == 2) ? "🥈" : (rank == 3) ? "🥉" : "  ";

                    System.out.printf("%s %d. %-12s  Cost=$%-6.0f  Quality=%.2f  Delivery=%d days  Score=$%.2f%n",
                        badge, rank, name, cost, quality, delivery, solution.getScore());

                    rank++;
                }
            }
        }
        System.out.println();

        // Analysis
        System.out.println("=== Analysis ===");
        System.out.println();
        System.out.println("Hard Constraint Filtering:");
        System.out.println("  ❌ SupplierD eliminated (quality 0.60 < 0.7 threshold)");
        System.out.println("  ✓ Remaining suppliers all meet quality requirement");
        System.out.println();

        System.out.println("Soft Constraint Impact:");
        System.out.println("  • Suppliers with delivery >= 5 days get $20 penalty");
        System.out.println("  • This makes SupplierA (10 days) less attractive");
        System.out.println("  • SupplierB and SupplierC benefit from fast delivery");
        System.out.println();

        System.out.println("Optimization Decision:");
        System.out.println("  • SupplierA: $100 + $20 penalty = $120 total");
        System.out.println("  • SupplierE: $120 + $20 penalty = $140 total");
        System.out.println("  • SupplierB: $150 + $0 penalty = $150 total (fast delivery!)");
        System.out.println("  • SupplierC: $200 + $0 penalty = $200 total (fastest!)");
        System.out.println();

        if (best.getScore() <= 120) {
            System.out.println("✓ Optimal Choice: SupplierA (despite slow delivery, lowest total cost)");
        } else if (best.getScore() <= 150) {
            System.out.println("✓ Optimal Choice: SupplierB (best balance of cost, quality, speed)");
        }
        System.out.println();

        // Summary
        System.out.println("=== Summary ===");
        System.out.println();
        System.out.println("✓ Constraint optimization finds best solution from multiple candidates");
        System.out.println("✓ Hard constraints eliminate infeasible options");
        System.out.println("✓ Soft constraints add penalties to guide optimization");
        System.out.println("✓ Perfect for resource allocation, scheduling, configuration");
        System.out.println();

        System.out.println("=".repeat(80));
    }
}
