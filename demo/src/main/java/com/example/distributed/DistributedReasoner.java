package com.example.distributed;

import com.example.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * High-level API for distributed reasoning with JavaSense.
 *
 * <p>Provides a simple builder-based API for configuring and executing
 * distributed reasoning across multiple worker nodes.</p>
 *
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * // Create distributed reasoner with 3 workers
 * DistributedReasoner reasoner = DistributedReasoner.builder()
 *     .addWorker("worker1", "localhost", 5001)
 *     .addWorker("worker2", "localhost", 5002)
 *     .addWorker("worker3", "localhost", 5003)
 *     .partitionStrategy(new PartitionStrategy.PredicatePartitioner())
 *     .build();
 *
 * // Add facts and rules (automatically distributed)
 * reasoner.addFact(fact);
 * reasoner.addRule(rule);
 *
 * // Perform distributed reasoning
 * ReasoningResult result = reasoner.reason(100);
 *
 * // Query results
 * Set<Atom> facts = result.getFactsAt(10);
 * System.out.println("Speedup: " + result.getSpeedup() + "x");
 * }</pre>
 */
public class DistributedReasoner {
    private static final Logger logger = LoggerFactory.getLogger(DistributedReasoner.class);

    private final DistributedMaster master;

    private DistributedReasoner(Builder builder) {
        this.master = new DistributedMaster();

        // Configure master
        if (builder.partitionStrategy != null) {
            master.setPartitionStrategy(builder.partitionStrategy);
        }
        if (builder.workerTimeoutMs > 0) {
            master.setWorkerTimeout(builder.workerTimeoutMs);
        }

        // Add workers
        for (WorkerConfig config : builder.workers) {
            master.addWorker(config.id, config.host, config.port);
        }

        logger.info("DistributedReasoner initialized with {} workers", builder.workers.size());
    }

    /**
     * Creates a new builder for configuring distributed reasoning.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Adds a fact (will be distributed across workers).
     */
    public void addFact(TimedFact fact) {
        master.addFact(fact);
    }

    /**
     * Adds a rule (will be replicated to all workers).
     */
    public void addRule(Rule rule) {
        master.addRule(rule);
    }

    /**
     * Performs distributed reasoning.
     *
     * @param timesteps maximum timestep
     * @return reasoning result with performance metrics
     */
    public ReasoningResult reason(int timesteps) {
        long startMs = System.currentTimeMillis();

        DistributedMaster.DistributedInterpretation result = master.reason(timesteps);

        long executionTimeMs = System.currentTimeMillis() - startMs;

        return new ReasoningResult(result, executionTimeMs, master.getStatistics());
    }

    /**
     * Gets master and worker statistics.
     */
    public Map<String, Object> getStatistics() {
        return master.getStatistics();
    }

    /**
     * Shuts down the distributed reasoner.
     */
    public void shutdown() {
        master.shutdown();
    }

    // --- Builder ---

    public static class Builder {
        private final List<WorkerConfig> workers = new ArrayList<>();
        private PartitionStrategy partitionStrategy;
        private int workerTimeoutMs = 30000;

        /**
         * Adds a worker node.
         */
        public Builder addWorker(String workerId, String host, int port) {
            workers.add(new WorkerConfig(workerId, host, port));
            return this;
        }

        /**
         * Sets the partitioning strategy.
         */
        public Builder partitionStrategy(PartitionStrategy strategy) {
            this.partitionStrategy = strategy;
            return this;
        }

        /**
         * Sets worker timeout in milliseconds.
         */
        public Builder workerTimeout(int timeoutMs) {
            this.workerTimeoutMs = timeoutMs;
            return this;
        }

        /**
         * Builds the distributed reasoner.
         */
        public DistributedReasoner build() {
            if (workers.isEmpty()) {
                throw new IllegalStateException("At least one worker must be configured");
            }
            return new DistributedReasoner(this);
        }
    }

    private static class WorkerConfig {
        final String id;
        final String host;
        final int port;

        WorkerConfig(String id, String host, int port) {
            this.id = id;
            this.host = host;
            this.port = port;
        }
    }

    // --- Result ---

    /**
     * Result of distributed reasoning with performance metrics.
     */
    public static class ReasoningResult {
        private final DistributedMaster.DistributedInterpretation interpretation;
        private final long executionTimeMs;
        private final Map<String, Object> statistics;

        public ReasoningResult(DistributedMaster.DistributedInterpretation interpretation,
                              long executionTimeMs,
                              Map<String, Object> statistics) {
            this.interpretation = interpretation;
            this.executionTimeMs = executionTimeMs;
            this.statistics = statistics;
        }

        /**
         * Gets facts at a specific timestep.
         */
        public Set<Atom> getFactsAt(int time) {
            return interpretation.getFactsAt(time);
        }

        /**
         * Gets maximum timestep.
         */
        public int getMaxTime() {
            return interpretation.getMaxTime();
        }

        /**
         * Gets total execution time in milliseconds.
         */
        public long getExecutionTimeMs() {
            return executionTimeMs;
        }

        /**
         * Gets total number of facts derived.
         */
        public int getTotalFacts() {
            return interpretation.getTotalFacts();
        }

        /**
         * Gets worker results.
         */
        public List<WorkResult> getWorkerResults() {
            return interpretation.getWorkerResults();
        }

        /**
         * Gets detailed statistics.
         */
        public Map<String, Object> getStatistics() {
            return statistics;
        }

        /**
         * Estimates speedup compared to single-node reasoning.
         *
         * <p>This is an estimate based on worker count and overhead.</p>
         */
        public double getSpeedup() {
            int workerCount = interpretation.getWorkerResults().size();
            // Estimate: assumes ideal parallelism with 20% overhead
            return workerCount * 0.8;
        }

        /**
         * Gets a summary string of the reasoning performance.
         */
        public String getSummary() {
            return String.format(
                    "DistributedReasoning{workers=%d, facts=%d, time=%dms, speedup=%.1fx}",
                    interpretation.getWorkerResults().size(),
                    getTotalFacts(),
                    executionTimeMs,
                    getSpeedup()
            );
        }
    }
}
