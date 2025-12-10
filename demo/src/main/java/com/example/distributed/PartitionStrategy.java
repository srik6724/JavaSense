package com.example.distributed;

import com.example.Rule;
import com.example.TimedFact;

import java.util.List;
import java.util.Map;

/**
 * Strategy for partitioning facts and rules across distributed workers.
 *
 * <p>Different partitioning strategies optimize for different workloads:</p>
 * <ul>
 *   <li><b>Predicate-based:</b> Keep related facts together (default)</li>
 *   <li><b>Temporal:</b> Partition by time range</li>
 *   <li><b>Hash-based:</b> Even distribution</li>
 * </ul>
 */
public interface PartitionStrategy {

    /**
     * Partitions facts across workers.
     *
     * @param facts list of facts to partition
     * @param workerIds list of available worker IDs
     * @return map of worker ID -> facts assigned to that worker
     */
    Map<String, List<TimedFact>> partitionFacts(List<TimedFact> facts, List<String> workerIds);

    /**
     * Partitions rules across workers.
     *
     * <p>By default, rules are replicated to all workers.
     * Subclasses can override for rule-specific partitioning.</p>
     *
     * @param rules list of rules to partition
     * @param workerIds list of available worker IDs
     * @return map of worker ID -> rules assigned to that worker
     */
    Map<String, List<Rule>> partitionRules(List<Rule> rules, List<String> workerIds);

    /**
     * Gets the strategy name.
     */
    String getName();

    /**
     * Predicate-based partitioning strategy.
     *
     * <p>Groups facts by predicate and assigns each predicate group to a worker.
     * Minimizes communication by keeping related facts together.</p>
     */
    class PredicatePartitioner implements PartitionStrategy {
        @Override
        public Map<String, List<TimedFact>> partitionFacts(List<TimedFact> facts, List<String> workerIds) {
            Map<String, List<TimedFact>> partitions = new java.util.HashMap<>();

            // Initialize empty lists for each worker
            for (String workerId : workerIds) {
                partitions.put(workerId, new java.util.ArrayList<>());
            }

            // Group facts by predicate
            Map<String, List<TimedFact>> byPredicate = new java.util.HashMap<>();
            for (TimedFact fact : facts) {
                String predicate = fact.getAtom().getPredicate();
                byPredicate.computeIfAbsent(predicate, k -> new java.util.ArrayList<>()).add(fact);
            }

            // Assign each predicate group to a worker (round-robin)
            int workerIndex = 0;
            for (List<TimedFact> predicateFacts : byPredicate.values()) {
                String workerId = workerIds.get(workerIndex % workerIds.size());
                partitions.get(workerId).addAll(predicateFacts);
                workerIndex++;
            }

            return partitions;
        }

        @Override
        public Map<String, List<Rule>> partitionRules(List<Rule> rules, List<String> workerIds) {
            // Replicate all rules to all workers
            Map<String, List<Rule>> partitions = new java.util.HashMap<>();
            for (String workerId : workerIds) {
                partitions.put(workerId, new java.util.ArrayList<>(rules));
            }
            return partitions;
        }

        @Override
        public String getName() {
            return "PredicatePartitioner";
        }
    }

    /**
     * Hash-based partitioning strategy.
     *
     * <p>Distributes facts evenly across workers using hash of fact ID.
     * Good load balancing but may increase communication.</p>
     */
    class HashPartitioner implements PartitionStrategy {
        @Override
        public Map<String, List<TimedFact>> partitionFacts(List<TimedFact> facts, List<String> workerIds) {
            Map<String, List<TimedFact>> partitions = new java.util.HashMap<>();

            // Initialize empty lists
            for (String workerId : workerIds) {
                partitions.put(workerId, new java.util.ArrayList<>());
            }

            // Assign facts by hash
            for (TimedFact fact : facts) {
                int hash = Math.abs(fact.getName().hashCode());
                int workerIndex = hash % workerIds.size();
                String workerId = workerIds.get(workerIndex);
                partitions.get(workerId).add(fact);
            }

            return partitions;
        }

        @Override
        public Map<String, List<Rule>> partitionRules(List<Rule> rules, List<String> workerIds) {
            // Replicate all rules to all workers
            Map<String, List<Rule>> partitions = new java.util.HashMap<>();
            for (String workerId : workerIds) {
                partitions.put(workerId, new java.util.ArrayList<>(rules));
            }
            return partitions;
        }

        @Override
        public String getName() {
            return "HashPartitioner";
        }
    }

    /**
     * Round-robin partitioning strategy.
     *
     * <p>Distributes facts one-by-one in round-robin fashion.
     * Simple and provides good load balancing.</p>
     */
    class RoundRobinPartitioner implements PartitionStrategy {
        @Override
        public Map<String, List<TimedFact>> partitionFacts(List<TimedFact> facts, List<String> workerIds) {
            Map<String, List<TimedFact>> partitions = new java.util.HashMap<>();

            // Initialize empty lists
            for (String workerId : workerIds) {
                partitions.put(workerId, new java.util.ArrayList<>());
            }

            // Assign facts in round-robin
            for (int i = 0; i < facts.size(); i++) {
                String workerId = workerIds.get(i % workerIds.size());
                partitions.get(workerId).add(facts.get(i));
            }

            return partitions;
        }

        @Override
        public Map<String, List<Rule>> partitionRules(List<Rule> rules, List<String> workerIds) {
            // Replicate all rules to all workers
            Map<String, List<Rule>> partitions = new java.util.HashMap<>();
            for (String workerId : workerIds) {
                partitions.put(workerId, new java.util.ArrayList<>(rules));
            }
            return partitions;
        }

        @Override
        public String getName() {
            return "RoundRobinPartitioner";
        }
    }
}
