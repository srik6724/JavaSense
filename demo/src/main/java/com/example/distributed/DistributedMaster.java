package com.example.distributed;

import com.example.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.*;

/**
 * Master coordinator for distributed reasoning.
 *
 * <p>The master manages worker nodes, distributes facts/rules, coordinates reasoning,
 * and aggregates results from all workers.</p>
 *
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * // Create master
 * DistributedMaster master = new DistributedMaster();
 *
 * // Register workers
 * master.addWorker("worker1", "localhost", 5001);
 * master.addWorker("worker2", "localhost", 5002);
 *
 * // Set partitioning strategy
 * master.setPartitionStrategy(new PartitionStrategy.PredicatePartitioner());
 *
 * // Add facts and rules
 * master.addFact(fact);
 * master.addRule(rule);
 *
 * // Perform distributed reasoning
 * DistributedInterpretation result = master.reason(100);
 *
 * // Query results
 * Set<Atom> factsAtTime = result.getFactsAt(10);
 * }</pre>
 */
public class DistributedMaster {
    private static final Logger logger = LoggerFactory.getLogger(DistributedMaster.class);

    private final Map<String, WorkerConnection> workers = new ConcurrentHashMap<>();
    private final List<TimedFact> facts = new ArrayList<>();
    private final List<Rule> rules = new ArrayList<>();

    private PartitionStrategy partitionStrategy = new PartitionStrategy.PredicatePartitioner();
    private int workerTimeoutMs = 30000;  // 30 seconds
    private int maxRetries = 3;

    // Statistics
    private long totalReasoningTimeMs = 0;
    private int tasksDistributed = 0;

    /**
     * Adds a worker node to the cluster.
     *
     * @param workerId unique worker identifier
     * @param host worker host
     * @param port worker RMI port
     */
    public void addWorker(String workerId, String host, int port) {
        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            WorkerService worker = (WorkerService) registry.lookup("WorkerService");

            // Test connection
            if (worker.isHealthy()) {
                workers.put(workerId, new WorkerConnection(workerId, worker, host, port));
                logger.info("Added worker: {} at {}:{}", workerId, host, port);
            } else {
                logger.warn("Worker {} at {}:{} is not healthy", workerId, host, port);
            }
        } catch (Exception e) {
            logger.error("Failed to connect to worker {} at {}:{}", workerId, host, port, e);
            throw new RuntimeException("Failed to add worker: " + workerId, e);
        }
    }

    /**
     * Removes a worker from the cluster.
     */
    public void removeWorker(String workerId) {
        workers.remove(workerId);
        logger.info("Removed worker: {}", workerId);
    }

    /**
     * Sets the partitioning strategy.
     */
    public void setPartitionStrategy(PartitionStrategy strategy) {
        this.partitionStrategy = strategy;
        logger.info("Set partition strategy: {}", strategy.getName());
    }

    /**
     * Sets worker timeout in milliseconds.
     */
    public void setWorkerTimeout(int timeoutMs) {
        this.workerTimeoutMs = timeoutMs;
    }

    /**
     * Adds a fact (will be distributed during reasoning).
     */
    public void addFact(TimedFact fact) {
        facts.add(fact);
    }

    /**
     * Adds a rule (will be distributed to all workers).
     */
    public void addRule(Rule rule) {
        rules.add(rule);
    }

    /**
     * Performs distributed reasoning.
     *
     * @param timesteps maximum timestep
     * @return aggregated results from all workers
     */
    public DistributedInterpretation reason(int timesteps) {
        logger.info("Starting distributed reasoning with {} workers for {} timesteps",
                workers.size(), timesteps);

        if (workers.isEmpty()) {
            throw new IllegalStateException("No workers available");
        }

        long startMs = System.currentTimeMillis();

        try {
            // Step 1: Partition facts and rules
            List<String> workerIds = new ArrayList<>(workers.keySet());
            Map<String, List<TimedFact>> factPartitions = partitionStrategy.partitionFacts(facts, workerIds);
            Map<String, List<Rule>> rulePartitions = partitionStrategy.partitionRules(rules, workerIds);

            logger.info("Partitioned {} facts and {} rules across {} workers",
                    facts.size(), rules.size(), workers.size());

            // Step 2: Distribute facts and rules to workers
            distributeData(factPartitions, rulePartitions);

            // Step 3: Execute reasoning on all workers in parallel
            List<WorkResult> results = executeReasoning(timesteps);

            // Step 4: Aggregate results
            DistributedInterpretation interpretation = aggregateResults(results, timesteps);

            long executionTimeMs = System.currentTimeMillis() - startMs;
            totalReasoningTimeMs += executionTimeMs;
            tasksDistributed++;

            logger.info("Distributed reasoning completed in {}ms", executionTimeMs);
            return interpretation;

        } catch (Exception e) {
            logger.error("Distributed reasoning failed", e);
            throw new RuntimeException("Distributed reasoning failed", e);
        }
    }

    /**
     * Gets statistics about the master and workers.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("workers", workers.size());
        stats.put("facts", facts.size());
        stats.put("rules", rules.size());
        stats.put("tasksDistributed", tasksDistributed);
        stats.put("totalReasoningTimeMs", totalReasoningTimeMs);
        stats.put("averageReasoningTimeMs",
                tasksDistributed > 0 ? totalReasoningTimeMs / tasksDistributed : 0);

        // Add worker stats
        Map<String, WorkerStats> workerStats = new HashMap<>();
        for (Map.Entry<String, WorkerConnection> entry : workers.entrySet()) {
            try {
                workerStats.put(entry.getKey(), entry.getValue().worker.getStats());
            } catch (Exception e) {
                logger.warn("Failed to get stats from worker {}", entry.getKey());
            }
        }
        stats.put("workerStats", workerStats);

        return stats;
    }

    /**
     * Shuts down all workers.
     */
    public void shutdown() {
        logger.info("Shutting down distributed master");
        workers.clear();
    }

    // --- Internal Methods ---

    private void distributeData(Map<String, List<TimedFact>> factPartitions,
                                Map<String, List<Rule>> rulePartitions) {
        ExecutorService executor = Executors.newFixedThreadPool(workers.size());
        List<Future<?>> futures = new ArrayList<>();

        for (Map.Entry<String, WorkerConnection> entry : workers.entrySet()) {
            String workerId = entry.getKey();
            WorkerConnection connection = entry.getValue();

            Future<?> future = executor.submit(() -> {
                try {
                    // Send rules (all rules to all workers)
                    for (Rule rule : rulePartitions.get(workerId)) {
                        connection.worker.addRule(rule);
                    }

                    // BUG FIX: Send ALL base facts to ALL workers
                    // Multi-predicate rules require facts from multiple predicates to evaluate
                    // Example: "atRisk(X) <-1 disrupted(Y), supplies(Y,X)" needs both predicates
                    for (TimedFact fact : facts) {
                        connection.worker.addFact(fact);
                    }

                    logger.debug("Distributed data to worker {}: {} facts (all base facts), {} rules",
                            workerId, facts.size(),
                            rulePartitions.get(workerId).size());

                } catch (Exception e) {
                    logger.error("Failed to distribute data to worker {}", workerId, e);
                    throw new RuntimeException("Failed to distribute data", e);
                }
            });

            futures.add(future);
        }

        // Wait for all distributions to complete
        for (Future<?> future : futures) {
            try {
                future.get(workerTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                logger.error("Distribution timeout or failure", e);
            }
        }

        executor.shutdown();
    }

    private List<WorkResult> executeReasoning(int timesteps) {
        ExecutorService executor = Executors.newFixedThreadPool(workers.size());
        List<Future<WorkResult>> futures = new ArrayList<>();

        // Submit reasoning tasks to all workers
        for (Map.Entry<String, WorkerConnection> entry : workers.entrySet()) {
            String workerId = entry.getKey();
            WorkerConnection connection = entry.getValue();

            Future<WorkResult> future = executor.submit(() -> {
                return executeWithRetry(connection, 0, timesteps);
            });

            futures.add(future);
        }

        // Collect results
        List<WorkResult> results = new ArrayList<>();
        for (Future<WorkResult> future : futures) {
            try {
                WorkResult result = future.get(workerTimeoutMs, TimeUnit.MILLISECONDS);
                results.add(result);

                if (result.isSuccess()) {
                    logger.info("Worker {} completed: {}", result.getWorkerId(), result);
                } else {
                    logger.warn("Worker {} failed: {}", result.getWorkerId(), result.getErrorMessage());
                }
            } catch (TimeoutException e) {
                logger.error("Worker timeout", e);
            } catch (Exception e) {
                logger.error("Worker execution failed", e);
            }
        }

        executor.shutdown();
        return results;
    }

    private WorkResult executeWithRetry(WorkerConnection connection, int startTime, int endTime) {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return connection.worker.reason(startTime, endTime);
            } catch (Exception e) {
                logger.warn("Worker {} attempt {} failed: {}", connection.workerId, attempt + 1, e.getMessage());
                if (attempt == maxRetries - 1) {
                    return WorkResult.failure(connection.workerId,
                            "Max retries exceeded: " + e.getMessage());
                }
                // Wait before retry
                try {
                    Thread.sleep(1000 * (attempt + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return WorkResult.failure(connection.workerId, "Unexpected retry loop exit");
    }

    private DistributedInterpretation aggregateResults(List<WorkResult> results, int timesteps) {
        logger.info("Aggregating results from {} workers", results.size());

        // Collect all derived facts
        List<TimedFact> allDerivedFacts = new ArrayList<>();
        for (WorkResult result : results) {
            if (result.isSuccess()) {
                allDerivedFacts.addAll(result.getDerivedFacts());
            }
        }

        // Build interpretation from derived facts
        List<Set<Atom>> factsAtTime = new ArrayList<>();
        for (int t = 0; t <= timesteps; t++) {
            factsAtTime.add(new HashSet<>());
        }

        // Add base facts
        for (TimedFact fact : facts) {
            for (int t = 0; t <= timesteps; t++) {
                if (fact.isTrueAt(t)) {
                    factsAtTime.get(t).add(fact.getAtom());
                }
            }
        }

        // Add derived facts
        for (TimedFact fact : allDerivedFacts) {
            for (int t = 0; t <= timesteps; t++) {
                if (fact.isTrueAt(t)) {
                    factsAtTime.get(t).add(fact.getAtom());
                }
            }
        }

        logger.info("Aggregation complete: {} total facts across {} timesteps",
                allDerivedFacts.size(), timesteps + 1);

        return new DistributedInterpretation(factsAtTime, results);
    }

    // --- Helper Classes ---

    private static class WorkerConnection {
        final String workerId;
        final WorkerService worker;
        final String host;
        final int port;

        WorkerConnection(String workerId, WorkerService worker, String host, int port) {
            this.workerId = workerId;
            this.worker = worker;
            this.host = host;
            this.port = port;
        }
    }

    /**
     * Result of distributed reasoning.
     */
    public static class DistributedInterpretation {
        private final List<Set<Atom>> factsAtTime;
        private final List<WorkResult> workerResults;

        public DistributedInterpretation(List<Set<Atom>> factsAtTime, List<WorkResult> workerResults) {
            this.factsAtTime = factsAtTime;
            this.workerResults = workerResults;
        }

        public Set<Atom> getFactsAt(int time) {
            if (time < 0 || time >= factsAtTime.size()) {
                return Collections.emptySet();
            }
            return Collections.unmodifiableSet(factsAtTime.get(time));
        }

        public int getMaxTime() {
            return factsAtTime.size() - 1;
        }

        public List<WorkResult> getWorkerResults() {
            return Collections.unmodifiableList(workerResults);
        }

        public int getTotalFacts() {
            return factsAtTime.stream().mapToInt(Set::size).sum();
        }
    }
}
