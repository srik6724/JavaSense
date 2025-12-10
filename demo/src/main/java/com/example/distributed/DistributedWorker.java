package com.example.distributed;

import com.example.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

/**
 * Distributed worker node that performs reasoning tasks.
 *
 * <p>Workers receive facts, rules, and reasoning tasks from the master coordinator
 * via Java RMI and return derived facts.</p>
 *
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * // Start worker on port 5001
 * DistributedWorker worker = new DistributedWorker("worker1", 5001);
 * worker.start();
 *
 * // Worker is now ready to receive tasks from master
 * // ... waits for work ...
 *
 * worker.shutdown();
 * }</pre>
 */
public class DistributedWorker extends UnicastRemoteObject implements WorkerService {
    private static final Logger logger = LoggerFactory.getLogger(DistributedWorker.class);

    private final String workerId;
    private final int port;
    private final OptimizedReasoner reasoner;

    // Track base facts to exclude them from derived facts
    private final Set<String> baseFacts = new HashSet<>();

    // Statistics
    private int factsAdded = 0;
    private int rulesAdded = 0;
    private int tasksCompleted = 0;
    private int tasksFailed = 0;
    private long totalExecutionTimeMs = 0;
    private final long startTimeMs;

    public DistributedWorker(String workerId, int port) throws RemoteException {
        super(port);
        this.workerId = workerId;
        this.port = port;
        this.reasoner = new OptimizedReasoner();
        this.startTimeMs = System.currentTimeMillis();

        logger.info("Worker {} created on port {}", workerId, port);
    }

    /**
     * Starts the worker and registers it with RMI registry.
     */
    public void start() throws Exception {
        Registry registry = LocateRegistry.createRegistry(port);
        registry.rebind("WorkerService", this);

        logger.info("Worker {} started and registered on port {}", workerId, port);
    }

    @Override
    public void addFact(TimedFact fact) throws RemoteException {
        try {
            reasoner.addFact(fact);
            factsAdded++;
            // Track base facts so we can exclude them from derived facts
            baseFacts.add(fact.getAtom().toString());
            logger.debug("Worker {} added fact: {}", workerId, fact);
        } catch (Exception e) {
            logger.error("Worker {} failed to add fact", workerId, e);
            throw new RemoteException("Failed to add fact", e);
        }
    }

    @Override
    public void addRule(Rule rule) throws RemoteException {
        try {
            reasoner.addRule(rule);
            rulesAdded++;
            logger.debug("Worker {} added rule: {}", workerId, rule);
        } catch (Exception e) {
            logger.error("Worker {} failed to add rule", workerId, e);
            throw new RemoteException("Failed to add rule", e);
        }
    }

    @Override
    public WorkResult reason(int startTime, int endTime) throws RemoteException {
        logger.info("Worker {} reasoning from t={} to t={}", workerId, startTime, endTime);

        long startMs = System.currentTimeMillis();

        try {
            // Perform reasoning
            ReasoningInterpretation result = reasoner.reason(endTime);

            // Extract derived facts in the time range
            List<TimedFact> derivedFacts = extractDerivedFacts(result, startTime, endTime);

            long executionTimeMs = System.currentTimeMillis() - startMs;

            // Update statistics
            tasksCompleted++;
            totalExecutionTimeMs += executionTimeMs;

            logger.info("Worker {} completed reasoning: derived {} facts in {}ms",
                    workerId, derivedFacts.size(), executionTimeMs);

            // Count total facts across all timesteps
            int totalFacts = 0;
            for (int t = 0; t <= endTime; t++) {
                totalFacts += result.getFactsAt(t).size();
            }

            return WorkResult.success(
                    workerId,
                    derivedFacts,
                    totalFacts,
                    rulesAdded,
                    executionTimeMs
            );

        } catch (Exception e) {
            tasksFailed++;
            logger.error("Worker {} failed reasoning task", workerId, e);
            return WorkResult.failure(workerId, e.getMessage());
        }
    }

    @Override
    public void addDerivedFacts(List<TimedFact> facts) throws RemoteException {
        try {
            for (TimedFact fact : facts) {
                reasoner.addFact(fact);
            }
            logger.debug("Worker {} added {} derived facts from other workers",
                    workerId, facts.size());
        } catch (Exception e) {
            logger.error("Worker {} failed to add derived facts", workerId, e);
            throw new RemoteException("Failed to add derived facts", e);
        }
    }

    @Override
    public void reset() throws RemoteException {
        try {
            // Reset counters and base facts
            factsAdded = 0;
            rulesAdded = 0;
            baseFacts.clear();
            logger.info("Worker {} reset (note: cannot clear OptimizedReasoner state)", workerId);
        } catch (Exception e) {
            logger.error("Worker {} failed to reset", workerId, e);
            throw new RemoteException("Failed to reset", e);
        }
    }

    @Override
    public boolean isHealthy() throws RemoteException {
        return true;  // If we can respond, we're healthy
    }

    @Override
    public WorkerStats getStats() throws RemoteException {
        long uptimeMs = System.currentTimeMillis() - startTimeMs;

        WorkerStats stats = new WorkerStats(
                workerId,
                factsAdded,
                rulesAdded,
                tasksCompleted,
                tasksFailed,
                totalExecutionTimeMs,
                uptimeMs
        );

        return stats;
    }

    /**
     * Shuts down the worker.
     */
    public void shutdown() {
        try {
            UnicastRemoteObject.unexportObject(this, true);
            logger.info("Worker {} shut down", workerId);
        } catch (Exception e) {
            logger.error("Error shutting down worker {}", workerId, e);
        }
    }

    // --- Helper Methods ---

    private List<TimedFact> extractDerivedFacts(ReasoningInterpretation result,
                                                 int startTime, int endTime) {
        List<TimedFact> facts = new ArrayList<>();
        Set<String> seenFactIds = new HashSet<>();

        logger.info("Worker {} extracting facts from t={} to t={}", workerId, startTime, endTime);
        logger.info("Worker {} has {} base facts to filter out", workerId, baseFacts.size());

        for (int t = startTime; t <= endTime; t++) {
            Set<Atom> atomsAtT = result.getFactsAt(t);
            logger.info("Worker {} found {} atoms at t={}", workerId, atomsAtT.size(), t);

            for (Atom atom : atomsAtT) {
                // Skip base facts - only return derived facts
                if (baseFacts.contains(atom.toString())) {
                    logger.debug("Worker {} skipping base fact: {}", workerId, atom);
                    continue;
                }

                logger.info("Worker {} found derived fact: {} at t={}", workerId, atom, t);

                // Create a unique ID for this fact
                String factId = workerId + "_" + atom.toString() + "_" + t;

                if (!seenFactIds.contains(factId)) {
                    TimedFact fact = new TimedFact(
                            atom,
                            factId,
                            List.of(new Interval(t, t))
                    );
                    facts.add(fact);
                    seenFactIds.add(factId);
                }
            }
        }

        logger.info("Worker {} extracted {} derived facts total", workerId, facts.size());
        return facts;
    }

    // --- Main Method for Standalone Execution ---

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java DistributedWorker <workerId> <port>");
            System.exit(1);
        }

        String workerId = args[0];
        int port = Integer.parseInt(args[1]);

        try {
            DistributedWorker worker = new DistributedWorker(workerId, port);
            worker.start();

            System.out.println("Worker " + workerId + " started on port " + port);
            System.out.println("Press Enter to shut down...");
            System.in.read();

            worker.shutdown();
        } catch (Exception e) {
            System.err.println("Failed to start worker: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
