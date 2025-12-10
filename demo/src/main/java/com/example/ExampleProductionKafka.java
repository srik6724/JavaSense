package com.example;

import com.example.integration.KafkaReasoner;

/**
 * Production Kafka Integration Example
 *
 * <p>This example shows how to use JavaSense with a real Kafka cluster
 * for real-time fraud detection.</p>
 *
 * <h2>Prerequisites:</h2>
 * <ul>
 *   <li>Kafka running at localhost:9092</li>
 *   <li>Topic "transactions" created</li>
 *   <li>Topic "fraud-alerts" created (for output)</li>
 * </ul>
 *
 * <h2>To run:</h2>
 * <pre>
 * # Start Kafka (if not running)
 * # On macOS/Linux:
 * bin/kafka-server-start.sh config/server.properties
 *
 * # Create topics
 * bin/kafka-topics.sh --create --topic transactions --bootstrap-server localhost:9092
 * bin/kafka-topics.sh --create --topic fraud-alerts --bootstrap-server localhost:9092
 *
 * # Run this example
 * mvn exec:java -Dexec.mainClass="com.example.ExampleProductionKafka"
 *
 * # In another terminal, send test messages:
 * bin/kafka-console-producer.sh --topic transactions --bootstrap-server localhost:9092
 * > {"predicate": "transaction", "args": ["A123", "10000"], "id": "txn_001", "time": 5}
 * > {"predicate": "newAccount", "args": ["A123"], "id": "acct_001", "time": 0}
 * </pre>
 */
public class ExampleProductionKafka {

    public static void main(String[] args) throws Exception {
        System.out.println("=".repeat(80));
        System.out.println("JavaSense + Kafka - Production Fraud Detection");
        System.out.println("=".repeat(80));
        System.out.println();

        // Create Kafka reasoner
        KafkaReasoner reasoner = new KafkaReasoner.Builder()
            .bootstrapServers("localhost:9092")
            .groupId("javasense-fraud-detection")
            .topic("transactions")
            .parser(KafkaReasoner.defaultJSONParser())  // Use built-in JSON parser
            .maxTimesteps(100)
            .build();

        // Add fraud detection rules
        System.out.println("Adding fraud detection rules...");
        reasoner.addRule(new Rule(
            "largeTransaction(x) <-0 transaction(x,amt), highAmount(amt)",
            "large_transaction_rule"
        ));

        reasoner.addRule(new Rule(
            "fraudDetected(x) <-1 largeTransaction(x), newAccount(x)",
            "fraud_detection_rule"
        ));

        System.out.println("✓ Rules added\n");

        // Register fraud alert callback
        reasoner.onNewFact("fraudDetected", fact -> {
            String account = fact.getArgs().get(0);
            System.out.println("🚨 FRAUD ALERT: Account " + account + " flagged!");

            // In production: Send to fraud-alerts topic
            // KafkaProducer<String, String> producer = ...;
            // producer.send(new ProducerRecord<>("fraud-alerts", account));
        });

        // Start consuming
        System.out.println("Starting Kafka consumer...");
        System.out.println("Listening to topic: transactions");
        System.out.println("Waiting for messages...\n");
        reasoner.start();

        // Keep running until Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n\nShutting down...");
            reasoner.stop();
            System.out.println("\nStatistics:");
            reasoner.getStatistics().forEach((key, value) ->
                System.out.println("  " + key + ": " + value)
            );
            System.out.println("\nGoodbye!");
        }));

        // Block forever
        reasoner.awaitTermination();
    }
}
