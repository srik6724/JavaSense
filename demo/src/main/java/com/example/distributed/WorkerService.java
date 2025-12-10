package com.example.distributed;

import com.example.*;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

/**
 * RMI interface for distributed worker nodes.
 *
 * <p>Workers receive facts, rules, and reasoning tasks from the master coordinator
 * and return derived facts back to the master.</p>
 */
public interface WorkerService extends Remote {

    /**
     * Adds a fact to this worker's local knowledge base.
     *
     * @param fact the fact to add
     * @throws RemoteException if communication fails
     */
    void addFact(TimedFact fact) throws RemoteException;

    /**
     * Adds a rule to this worker's local knowledge base.
     *
     * @param rule the rule to add
     * @throws RemoteException if communication fails
     */
    void addRule(Rule rule) throws RemoteException;

    /**
     * Performs reasoning for a specific timestep range.
     *
     * @param startTime starting timestep
     * @param endTime ending timestep
     * @return work result with derived facts
     * @throws RemoteException if communication fails
     */
    WorkResult reason(int startTime, int endTime) throws RemoteException;

    /**
     * Adds derived facts from other workers (for multi-round reasoning).
     *
     * @param facts derived facts from other workers
     * @throws RemoteException if communication fails
     */
    void addDerivedFacts(List<TimedFact> facts) throws RemoteException;

    /**
     * Clears all facts and rules from this worker.
     *
     * @throws RemoteException if communication fails
     */
    void reset() throws RemoteException;

    /**
     * Gets worker health status.
     *
     * @return true if worker is healthy
     * @throws RemoteException if communication fails
     */
    boolean isHealthy() throws RemoteException;

    /**
     * Gets worker statistics.
     *
     * @return statistics map
     * @throws RemoteException if communication fails
     */
    WorkerStats getStats() throws RemoteException;
}
