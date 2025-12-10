package com.example.distributed;

import com.example.TimedFact;

import java.io.Serializable;
import java.util.*;

/**
 * Result of reasoning performed by a distributed worker.
 *
 * <p>Contains derived facts, statistics, and metadata about the work performed.</p>
 */
public class WorkResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String workerId;
    private final List<TimedFact> derivedFacts;
    private final int factsProcessed;
    private final int rulesApplied;
    private final long executionTimeMs;
    private final boolean success;
    private final String errorMessage;

    public WorkResult(String workerId,
                     List<TimedFact> derivedFacts,
                     int factsProcessed,
                     int rulesApplied,
                     long executionTimeMs,
                     boolean success,
                     String errorMessage) {
        this.workerId = workerId;
        this.derivedFacts = new ArrayList<>(derivedFacts);
        this.factsProcessed = factsProcessed;
        this.rulesApplied = rulesApplied;
        this.executionTimeMs = executionTimeMs;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public static WorkResult success(String workerId,
                                     List<TimedFact> derivedFacts,
                                     int factsProcessed,
                                     int rulesApplied,
                                     long executionTimeMs) {
        return new WorkResult(workerId, derivedFacts, factsProcessed, rulesApplied,
                             executionTimeMs, true, null);
    }

    public static WorkResult failure(String workerId, String errorMessage) {
        return new WorkResult(workerId, Collections.emptyList(), 0, 0, 0, false, errorMessage);
    }

    public String getWorkerId() {
        return workerId;
    }

    public List<TimedFact> getDerivedFacts() {
        return Collections.unmodifiableList(derivedFacts);
    }

    public int getFactsProcessed() {
        return factsProcessed;
    }

    public int getRulesApplied() {
        return rulesApplied;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        if (success) {
            return String.format("WorkResult{worker=%s, derived=%d, processed=%d, time=%dms}",
                    workerId, derivedFacts.size(), factsProcessed, executionTimeMs);
        } else {
            return String.format("WorkResult{worker=%s, FAILED: %s}", workerId, errorMessage);
        }
    }
}
