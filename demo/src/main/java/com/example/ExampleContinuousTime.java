package com.example;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Example: Continuous Time Reasoning
 *
 * <p>Demonstrates temporal reasoning with real timestamps (Instant) instead of
 * discrete timesteps. Perfect for IoT sensors, financial data, event logs, etc.</p>
 *
 * <h2>Scenario: IoT Temperature Monitoring</h2>
 * <ul>
 *   <li>Sensors report temperature readings with precise timestamps</li>
 *   <li>Alert if temperature stays high for more than 1 hour</li>
 *   <li>Critical alert if temperature exceeds threshold during business hours</li>
 * </ul>
 */
public class ExampleContinuousTime {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("JavaSense v1.2 - Continuous Time Reasoning Example");
        System.out.println("=".repeat(80));
        System.out.println();

        // Create continuous time reasoner
        ContinuousTimeReasoner reasoner = new ContinuousTimeReasoner();

        // === SCENARIO: IoT Temperature Monitoring ===
        System.out.println("Scenario: IoT Temperature Monitoring");
        System.out.println("-".repeat(80));
        System.out.println();

        // Define base time
        Instant baseTime = Instant.parse("2025-01-15T08:00:00Z");

        // Add temperature sensor readings
        System.out.println("Loading temperature sensor data...");

        // Sensor 1: Normal temperature initially
        reasoner.addFact(ContinuousTimeFact.during(
            Atom.parse("temperature(sensor1,72)"),
            "reading_001",
            baseTime,
            baseTime.plus(Duration.ofHours(2))
        ));

        // Sensor 1: High temperature for 3 hours (should trigger alert)
        reasoner.addFact(ContinuousTimeFact.during(
            Atom.parse("temperature(sensor1,95)"),
            "reading_002",
            baseTime.plus(Duration.ofHours(2)),
            baseTime.plus(Duration.ofHours(5))
        ));

        // Sensor 2: Consistently normal
        reasoner.addFact(ContinuousTimeFact.during(
            Atom.parse("temperature(sensor2,68)"),
            "reading_003",
            baseTime,
            baseTime.plus(Duration.ofHours(8))
        ));

        // Add threshold facts
        reasoner.addFact(ContinuousTimeFact.during(
            Atom.parse("highTempThreshold(90)"),
            "threshold_001",
            baseTime,
            baseTime.plus(Duration.ofHours(24))
        ));

        // Business hours: 9 AM - 5 PM
        reasoner.addFact(ContinuousTimeFact.during(
            Atom.parse("businessHours"),
            "business_hours",
            baseTime.plus(Duration.ofHours(1)),  // 9 AM
            baseTime.plus(Duration.ofHours(9))   // 5 PM
        ));

        System.out.println("✓ Loaded sensor data");
        System.out.println();

        // Add continuous time rules
        System.out.println("Adding temporal rules...");

        // Rule 1: Detect high temperature (instantaneous)
        reasoner.addRule(ContinuousTimeRule.instantaneous(
            "highTemp(sensor) <- temperature(sensor,temp), highTempThreshold(threshold)",
            "high_temp_detection"
        ));

        // Rule 2: Alert if high temperature sustained for 1+ hour
        reasoner.addRule(new ContinuousTimeRule(
            "tempAlert(sensor) <-1h highTemp(sensor)",
            "sustained_high_temp_alert",
            Duration.ofHours(1)
        ));

        // Rule 3: Critical alert during business hours
        reasoner.addRule(ContinuousTimeRule.instantaneous(
            "criticalAlert(sensor) <- tempAlert(sensor), businessHours",
            "critical_business_hours_alert"
        ));

        System.out.println("✓ Added 3 temporal rules");
        System.out.println();

        // Perform reasoning
        System.out.println("Performing continuous time reasoning...");
        TimeInterval analysisWindow = TimeInterval.between(
            baseTime,
            baseTime.plus(Duration.ofHours(8))
        );

        ContinuousTimeReasoner.ContinuousTimeInterpretation result = reasoner.reason(analysisWindow);
        System.out.println("✓ Reasoning complete");
        System.out.println();

        // Query results at specific times
        System.out.println("=== Query Results ===");
        System.out.println();

        // Check at 9 AM (1 hour after base)
        Instant time9AM = baseTime.plus(Duration.ofHours(1));
        System.out.println("At 9:00 AM (start of business hours):");
        System.out.println("  Time: " + time9AM);
        Set<Atom> factsAt9AM = result.getFactsAt(time9AM);
        System.out.println("  Facts: " + factsAt9AM.size());
        factsAt9AM.forEach(atom -> System.out.println("    • " + atom));
        System.out.println();

        // Check at 11 AM (3 hours after base - high temp started 1 hour ago)
        Instant time11AM = baseTime.plus(Duration.ofHours(3));
        System.out.println("At 11:00 AM (1 hour after high temp started):");
        System.out.println("  Time: " + time11AM);
        Set<Atom> factsAt11AM = result.getFactsAt(time11AM);
        System.out.println("  Facts: " + factsAt11AM.size());

        // Check for alerts
        boolean hasAlert = factsAt11AM.stream()
            .anyMatch(a -> a.getPredicate().equals("tempAlert"));
        boolean hasCriticalAlert = factsAt11AM.stream()
            .anyMatch(a -> a.getPredicate().equals("criticalAlert"));

        System.out.println("  🌡️ Temperature Alert: " + (hasAlert ? "YES" : "NO"));
        System.out.println("  🚨 Critical Alert: " + (hasCriticalAlert ? "YES" : "NO"));
        System.out.println();

        // Check at 2 PM (6 hours after base)
        Instant time2PM = baseTime.plus(Duration.ofHours(6));
        System.out.println("At 2:00 PM (4 hours of high temperature):");
        System.out.println("  Time: " + time2PM);
        Set<Atom> factsAt2PM = result.getFactsAt(time2PM);

        hasAlert = factsAt2PM.stream()
            .anyMatch(a -> a.getPredicate().equals("tempAlert"));
        hasCriticalAlert = factsAt2PM.stream()
            .anyMatch(a -> a.getPredicate().equals("criticalAlert"));

        System.out.println("  🌡️ Temperature Alert: " + (hasAlert ? "YES" : "NO"));
        System.out.println("  🚨 Critical Alert: " + (hasCriticalAlert ? "YES" : "NO"));
        System.out.println();

        // Timeline analysis
        System.out.println("=== Timeline Analysis ===");
        System.out.println();

        Atom tempAlertSensor1 = Atom.parse("tempAlert(sensor1)");
        List<Instant> alertTimes = result.whenHolds(tempAlertSensor1);

        if (!alertTimes.isEmpty()) {
            System.out.println("tempAlert(sensor1) detected at " + alertTimes.size() + " time points:");
            alertTimes.stream().limit(5).forEach(instant ->
                System.out.println("  • " + instant)
            );
            if (alertTimes.size() > 5) {
                System.out.println("  ... and " + (alertTimes.size() - 5) + " more");
            }
        } else {
            System.out.println("No temperature alerts detected for sensor1");
        }
        System.out.println();

        // Statistics
        System.out.println("=== Statistics ===");
        System.out.println();
        reasoner.getStatistics().forEach((key, value) ->
            System.out.println("  " + key + ": " + value)
        );
        System.out.println();

        System.out.println("Time range analyzed:");
        System.out.println("  From: " + analysisWindow.getStart());
        System.out.println("  To:   " + analysisWindow.getEnd());
        System.out.println("  Duration: " + Duration.between(
            analysisWindow.getStart(),
            analysisWindow.getEnd()
        ).toHours() + " hours");
        System.out.println();

        // Summary
        System.out.println("=== Summary ===");
        System.out.println();
        System.out.println("✓ Continuous time reasoning allows precise temporal analysis");
        System.out.println("✓ Rules can specify real-world durations (1h, 5m, etc.)");
        System.out.println("✓ Perfect for IoT sensors, financial data, event streams");
        System.out.println("✓ Query at any instant - not limited to discrete timesteps");
        System.out.println();

        System.out.println("=".repeat(80));
    }
}
