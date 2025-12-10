package com.example.distributed;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Statistics about a distributed worker's performance.
 */
public class WorkerStats implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String workerId;
    private final int totalFacts;
    private final int totalRules;
    private final int tasksCompleted;
    private final int tasksFailed;
    private final long totalExecutionTimeMs;
    private final long uptimeMs;
    private final Map<String, Object> customStats;

    public WorkerStats(String workerId,
                      int totalFacts,
                      int totalRules,
                      int tasksCompleted,
                      int tasksFailed,
                      long totalExecutionTimeMs,
                      long uptimeMs) {
        this.workerId = workerId;
        this.totalFacts = totalFacts;
        this.totalRules = totalRules;
        this.tasksCompleted = tasksCompleted;
        this.tasksFailed = tasksFailed;
        this.totalExecutionTimeMs = totalExecutionTimeMs;
        this.uptimeMs = uptimeMs;
        this.customStats = new HashMap<>();
    }

    public String getWorkerId() {
        return workerId;
    }

    public int getTotalFacts() {
        return totalFacts;
    }

    public int getTotalRules() {
        return totalRules;
    }

    public int getTasksCompleted() {
        return tasksCompleted;
    }

    public int getTasksFailed() {
        return tasksFailed;
    }

    public long getTotalExecutionTimeMs() {
        return totalExecutionTimeMs;
    }

    public long getUptimeMs() {
        return uptimeMs;
    }

    public double getAverageTaskTimeMs() {
        return tasksCompleted > 0 ? (double) totalExecutionTimeMs / tasksCompleted : 0.0;
    }

    public Map<String, Object> getCustomStats() {
        return new HashMap<>(customStats);
    }

    public void addCustomStat(String key, Object value) {
        customStats.put(key, value);
    }

    @Override
    public String toString() {
        return String.format("WorkerStats{id=%s, facts=%d, rules=%d, tasks=%d/%d, avgTime=%.2fms}",
                workerId, totalFacts, totalRules, tasksCompleted, tasksFailed, getAverageTaskTimeMs());
    }
}
