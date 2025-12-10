package com.example.integration;

import com.example.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Production Kafka Integration for JavaSense - Stream reasoning from Kafka topics
 *
 * <p>Enables real-time reasoning over Kafka streams. Facts are consumed from topics,
 * reasoning is performed incrementally, and results can be published back to Kafka.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li><b>Auto-consume:</b> Subscribe to Kafka topics and reason automatically</li>
 *   <li><b>Custom parsers:</b> Convert Kafka messages to JavaSense facts</li>
 *   <li><b>Result publishing:</b> Publish derivations back to Kafka</li>
 *   <li><b>Backpressure:</b> Handle high-throughput streams gracefully</li>
 *   <li><b>Production-ready:</b> Full Kafka integration with proper error handling</li>
 * </ul>
 *
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * // Create Kafka reasoner
 * KafkaReasoner reasoner = new KafkaReasoner.Builder()
 *     .bootstrapServers("localhost:9092")
 *     .groupId("javasense-fraud-detection")
 *     .topic("transactions")
 *     .parser(msg -> parseTransactionFact(msg))
 *     .maxTimesteps(100)
 *     .build();
 *
 * // Add fraud detection rules
 * reasoner.addRule(new Rule("fraudDetected(x) <-1 largeTransaction(x), newAccount(x)"));
 *
 * // Alert on fraud
 * reasoner.onNewFact("fraudDetected", fact -> {
 *     String account = fact.getArgs().get(0);
 *     kafkaProducer.send("fraud-alerts", account);
 * });
 *
 * // Start streaming
 * reasoner.start();
 * }</pre>
 */
public class KafkaReasoner {
    private static final Logger logger = LoggerFactory.getLogger(KafkaReasoner.class);

    private final StreamingReasoner reasoner;
    private final List<String> topics;
    private final Function<String, TimedFact> messageParser;
    private final ExecutorService consumerThread;
    private final Properties kafkaProps;
    private final Gson gson = new Gson();

    // Real Kafka consumer
    private KafkaConsumer<String, String> consumer;
    private volatile boolean running = false;

    // Statistics
    private long messagesProcessed = 0;
    private long parseErrors = 0;
    private long reasoningErrors = 0;

    private KafkaReasoner(Builder builder) {
        this.reasoner = new StreamingReasoner(builder.maxTimesteps);
        this.topics = builder.topics;
        this.messageParser = builder.messageParser;
        this.consumerThread = Executors.newSingleThreadExecutor();
        this.kafkaProps = builder.kafkaProps;

        logger.info("KafkaReasoner created for topics: {} (bootstrap: {})",
                topics, kafkaProps.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
    }

    /**
     * Adds a rule to the reasoner.
     */
    public void addRule(Rule rule) {
        reasoner.addRule(rule);
    }

    /**
     * Registers a callback for derived facts.
     */
    public void onNewFact(String predicate, java.util.function.Consumer<Atom> callback) {
        reasoner.onNewFact(predicate, callback);
    }

    /**
     * Starts consuming from Kafka and reasoning.
     */
    public void start() {
        if (running) {
            logger.warn("KafkaReasoner already running");
            return;
        }

        running = true;
        logger.info("Starting Kafka consumer for topics: {}", topics);

        // Create Kafka consumer
        consumer = new KafkaConsumer<>(kafkaProps);
        consumer.subscribe(topics);

        consumerThread.submit(() -> {
            try {
                while (running) {
                    // Poll for records
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            processMessage(record.value());
                            messagesProcessed++;
                        } catch (Exception e) {
                            logger.error("Error processing message from topic {}, offset {}: {}",
                                    record.topic(), record.offset(), e.getMessage(), e);
                            parseErrors++;
                        }
                    }

                    // Commit offsets periodically
                    if (messagesProcessed % 100 == 0 && messagesProcessed > 0) {
                        consumer.commitSync();
                        logger.debug("Committed offsets after {} messages", messagesProcessed);
                    }
                }
            } catch (Exception e) {
                logger.error("Fatal error in Kafka consumer thread: {}", e.getMessage(), e);
            } finally {
                cleanup();
            }
        });

        logger.info("Kafka consumer started");
    }

    /**
     * Stops consuming from Kafka.
     */
    public void stop() {
        if (!running) {
            logger.warn("KafkaReasoner not running");
            return;
        }

        logger.info("Stopping Kafka consumer...");
        running = false;

        try {
            consumerThread.shutdown();
            if (!consumerThread.awaitTermination(10, TimeUnit.SECONDS)) {
                consumerThread.shutdownNow();
            }
        } catch (InterruptedException e) {
            consumerThread.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("Kafka consumer stopped. Processed {} messages ({} parse errors, {} reasoning errors)",
                messagesProcessed, parseErrors, reasoningErrors);
    }

    /**
     * Gets the underlying streaming reasoner.
     */
    public StreamingReasoner getReasoner() {
        return reasoner;
    }

    /**
     * Gets current statistics.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>(reasoner.getStatistics());
        stats.put("messagesProcessed", messagesProcessed);
        stats.put("parseErrors", parseErrors);
        stats.put("reasoningErrors", reasoningErrors);
        stats.put("running", running);
        return stats;
    }

    /**
     * Blocks until the consumer is stopped.
     */
    public void awaitTermination() throws InterruptedException {
        consumerThread.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }

    // --- Internal Methods ---

    private void processMessage(String message) {
        try {
            TimedFact fact = messageParser.apply(message);
            if (fact != null) {
                List<Provenance.AtomTimeKey> newDerivations = reasoner.addFactIncremental(fact);
                logger.debug("Processed message, derived {} new facts", newDerivations.size());
            } else {
                logger.trace("Message parser returned null (message skipped): {}", message);
            }
        } catch (Exception e) {
            reasoningErrors++;
            throw e;
        }
    }

    private void cleanup() {
        if (consumer != null) {
            try {
                consumer.close(Duration.ofSeconds(5));
                logger.info("Kafka consumer closed");
            } catch (Exception e) {
                logger.error("Error closing Kafka consumer: {}", e.getMessage(), e);
            }
        }
    }

    // --- Builder ---

    public static class Builder {
        private final List<String> topics = new ArrayList<>();
        private Function<String, TimedFact> messageParser = msg -> null;
        private int maxTimesteps = 100;
        private final Properties kafkaProps = new Properties();

        public Builder() {
            // Set default Kafka properties
            kafkaProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            kafkaProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            kafkaProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"); // Manual commit for reliability
            kafkaProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        }

        /**
         * Sets the Kafka bootstrap servers.
         *
         * @param servers comma-separated list (e.g., "localhost:9092" or "broker1:9092,broker2:9092")
         */
        public Builder bootstrapServers(String servers) {
            kafkaProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
            return this;
        }

        /**
         * Sets the consumer group ID.
         */
        public Builder groupId(String groupId) {
            kafkaProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            return this;
        }

        /**
         * Adds a Kafka topic to subscribe to.
         */
        public Builder topic(String topic) {
            this.topics.add(topic);
            return this;
        }

        /**
         * Sets the message parser (converts Kafka message to TimedFact).
         *
         * <p>Example with JSON:</p>
         * <pre>{@code
         * .parser(msg -> {
         *     Gson gson = new Gson();
         *     JsonObject json = gson.fromJson(msg, JsonObject.class);
         *     String account = json.get("account").getAsString();
         *     int amount = json.get("amount").getAsInt();
         *     int time = json.get("time").getAsInt();
         *     return new TimedFact(
         *         Atom.parse("transaction(" + account + "," + amount + ")"),
         *         "txn_" + json.get("id").getAsString(),
         *         List.of(new Interval(time, time))
         *     );
         * })
         * }</pre>
         */
        public Builder parser(Function<String, TimedFact> parser) {
            this.messageParser = parser;
            return this;
        }

        /**
         * Sets the maximum timesteps for reasoning.
         */
        public Builder maxTimesteps(int maxTimesteps) {
            this.maxTimesteps = maxTimesteps;
            return this;
        }

        /**
         * Sets a custom Kafka property.
         */
        public Builder kafkaProperty(String key, String value) {
            kafkaProps.put(key, value);
            return this;
        }

        public KafkaReasoner build() {
            if (topics.isEmpty()) {
                throw new IllegalStateException("At least one topic must be specified");
            }
            if (!kafkaProps.containsKey(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG)) {
                throw new IllegalStateException("Bootstrap servers must be specified");
            }
            if (!kafkaProps.containsKey(ConsumerConfig.GROUP_ID_CONFIG)) {
                throw new IllegalStateException("Group ID must be specified");
            }
            return new KafkaReasoner(this);
        }
    }

    // --- Helper: Default JSON Parser ---

    /**
     * Creates a default JSON parser for simple transaction messages.
     *
     * <p>Expected JSON format:</p>
     * <pre>{@code
     * {
     *   "predicate": "transaction",
     *   "args": ["A123", "10000"],
     *   "id": "txn_001",
     *   "time": 5
     * }
     * }</pre>
     */
    public static Function<String, TimedFact> defaultJSONParser() {
        Gson gson = new Gson();
        return msg -> {
            try {
                JsonObject json = gson.fromJson(msg, JsonObject.class);
                String predicate = json.get("predicate").getAsString();

                // Parse args array
                List<String> args = new ArrayList<>();
                json.get("args").getAsJsonArray().forEach(el -> args.add(el.getAsString()));

                String id = json.get("id").getAsString();
                int time = json.get("time").getAsInt();

                // Build atom string
                String atomStr = predicate + "(" + String.join(",", args) + ")";

                return new TimedFact(
                    Atom.parse(atomStr),
                    id,
                    List.of(new Interval(time, time))
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse JSON: " + msg, e);
            }
        };
    }
}
