package com.example.gpu;

import com.example.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Phase 5 GPU optimizations:
 * - Memory transfer caching
 * - Work-group size auto-tuning
 */
class GpuPhase5OptimizationsTest {

    private GpuReasoningEngine gpu;

    @BeforeEach
    void setUp() {
        gpu = new GpuReasoningEngine();
    }

    @AfterEach
    void tearDown() {
        if (gpu != null) {
            gpu.cleanup();
        }
    }

    @Test
    void testMemoryCaching() {
        if (!gpu.isGpuAvailable()) {
            System.out.println("Skipping GPU test (no GPU available)");
            return;
        }

        GpuFactStore store = new GpuFactStore(gpu);

        try {
            List<Atom> facts = Arrays.asList(
                Atom.parse("likes(alice,bob)"),
                Atom.parse("likes(bob,charlie)"),
                Atom.parse("popular(bob)")
            );

            // First upload
            store.uploadFacts(facts);
            assertEquals(3, store.getFactCount());
            assertEquals(0, store.getCacheHits(), "No cache hits on first upload");

            // Second upload with same facts - should hit cache
            store.uploadFacts(facts);
            assertEquals(1, store.getCacheHits(), "Should have 1 cache hit");

            // Third upload with same facts - another cache hit
            store.uploadFacts(facts);
            assertEquals(2, store.getCacheHits(), "Should have 2 cache hits");

            // Different facts - cache miss
            List<Atom> newFacts = Arrays.asList(
                Atom.parse("new(fact)")
            );
            store.uploadFacts(newFacts);
            assertEquals(2, store.getCacheHits(), "Cache hits should not increase on new facts");

            System.out.println("Memory caching test passed!");
            System.out.println("  Cache hits: " + store.getCacheHits());
            System.out.println("  Uploads: " + store.getStats().uploadCount);

        } finally {
            store.cleanup();
        }
    }

    @Test
    void testCacheInvalidation() {
        if (!gpu.isGpuAvailable()) return;

        GpuFactStore store = new GpuFactStore(gpu);

        try {
            List<Atom> facts = Arrays.asList(Atom.parse("test(a)"));

            // Upload facts
            store.uploadFacts(facts);
            assertTrue(store.isGpuDataValid(), "GPU data should be valid after upload");

            // Invalidate cache
            store.invalidateCache();
            assertFalse(store.isGpuDataValid(), "GPU data should be invalid after invalidation");

            // Next upload should not hit cache
            store.uploadFacts(facts);
            assertEquals(0, store.getCacheHits(), "Should not hit cache after invalidation");

        } finally {
            store.cleanup();
        }
    }

    @Test
    void testCachingPerformanceBenefit() {
        if (!gpu.isGpuAvailable()) return;

        GpuFactStore store = new GpuFactStore(gpu);

        try {
            // Generate large fact set
            List<Atom> facts = new ArrayList<>();
            for (int i = 0; i < 10000; i++) {
                facts.add(Atom.parse("fact" + i + "(a" + i + ",b" + i + ")"));
            }

            // First upload - full transfer
            long start1 = System.nanoTime();
            store.uploadFacts(facts);
            long time1 = System.nanoTime() - start1;

            // Second upload - cached (should be much faster)
            long start2 = System.nanoTime();
            store.uploadFacts(facts);
            long time2 = System.nanoTime() - start2;

            System.out.println("\nCaching Performance Benefit:");
            System.out.println("  First upload:  " + String.format("%.2f", time1 / 1_000_000.0) + " ms");
            System.out.println("  Cached upload: " + String.format("%.2f", time2 / 1_000_000.0) + " ms");
            System.out.println("  Speedup:       " + String.format("%.0f", time1 / (double) time2) + "x");

            // Cached should be at least 10x faster
            assertTrue(time2 < time1 / 10,
                "Cached upload should be much faster than first upload");

        } finally {
            store.cleanup();
        }
    }

    @Test
    void testWorkGroupSizeTuning() {
        if (!gpu.isGpuAvailable()) return;

        GpuFactStore store = new GpuFactStore(gpu);

        try {
            // Upload facts
            List<Atom> facts = Arrays.asList(
                Atom.parse("likes(alice,bob)"),
                Atom.parse("likes(bob,charlie)"),
                Atom.parse("likes(charlie,alice)")
            );
            store.uploadFacts(facts);

            GpuPatternMatcher matcher = new GpuPatternMatcher(gpu, store);

            try {
                // First pattern match - triggers work-group size tuning
                List<Literal> pattern = Arrays.asList(Literal.parse("likes(X,Y)"));
                List<java.util.Map<String, String>> results = matcher.findSubstitutions(pattern);

                assertEquals(3, results.size(), "Should find 3 matches");

                // Second pattern match - uses tuned work-group size
                results = matcher.findSubstitutions(pattern);
                assertEquals(3, results.size(), "Should still find 3 matches with tuned size");

                System.out.println("\nWork-group size tuning test passed!");

            } finally {
                matcher.cleanup();
            }

        } finally {
            store.cleanup();
        }
    }

    @Test
    void testWorkGroupSizeLargeDataset() {
        if (!gpu.isGpuAvailable()) return;

        GpuFactStore store = new GpuFactStore(gpu);

        try {
            // Generate large dataset to test work-group optimization
            List<Atom> facts = new ArrayList<>();
            for (int i = 0; i < 5000; i++) {
                facts.add(Atom.parse("item(n" + i + ")"));
            }
            store.uploadFacts(facts);

            GpuPatternMatcher matcher = new GpuPatternMatcher(gpu, store);

            try {
                List<Literal> pattern = Arrays.asList(Literal.parse("item(X)"));

                // Benchmark with work-group tuning
                long start = System.nanoTime();
                List<java.util.Map<String, String>> results = matcher.findSubstitutions(pattern);
                long elapsed = System.nanoTime() - start;

                assertEquals(5000, results.size(), "Should match all 5000 items");

                System.out.println("\nWork-group optimization on large dataset:");
                System.out.println("  Facts: 5000");
                System.out.println("  Matches: " + results.size());
                System.out.println("  Time: " + String.format("%.2f", elapsed / 1_000_000.0) + " ms");

            } finally {
                matcher.cleanup();
            }

        } finally {
            store.cleanup();
        }
    }

    @Test
    void testCombinedOptimizations() {
        if (!gpu.isGpuAvailable()) return;

        System.out.println("\n" + "=".repeat(80));
        System.out.println("Phase 5 Combined Optimizations Test");
        System.out.println("=".repeat(80));

        GpuFactStore store = new GpuFactStore(gpu);

        try {
            List<Atom> facts = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                facts.add(Atom.parse("data(n" + i + ",m" + i + ")"));
            }

            // First run - no caching, work-group tuning
            long start1 = System.nanoTime();
            store.uploadFacts(facts);
            GpuPatternMatcher matcher1 = new GpuPatternMatcher(gpu, store);
            List<java.util.Map<String, String>> results1 = matcher1.findSubstitutions(
                Arrays.asList(Literal.parse("data(X,Y)")));
            long time1 = System.nanoTime() - start1;
            matcher1.cleanup();

            // Second run - with caching and tuned work-group size
            long start2 = System.nanoTime();
            store.uploadFacts(facts);  // Cached
            GpuPatternMatcher matcher2 = new GpuPatternMatcher(gpu, store);
            List<java.util.Map<String, String>> results2 = matcher2.findSubstitutions(
                Arrays.asList(Literal.parse("data(X,Y)")));
            long time2 = System.nanoTime() - start2;
            matcher2.cleanup();

            System.out.println("\nResults:");
            System.out.println("  First run:  " + String.format("%.2f", time1 / 1_000_000.0) + " ms");
            System.out.println("  Second run: " + String.format("%.2f", time2 / 1_000_000.0) + " ms");
            System.out.println("  Speedup:    " + String.format("%.1f", time1 / (double) time2) + "x");
            System.out.println("  Cache hits: " + store.getCacheHits());

            assertEquals(results1.size(), results2.size(), "Results should be identical");
            assertTrue(time2 < time1, "Second run should be faster (cached transfer)");

            System.out.println("\nPhase 5 optimizations working correctly!");

        } finally {
            store.cleanup();
        }

        System.out.println("=".repeat(80));
    }

    @Test
    void testCacheStatsReporting() {
        if (!gpu.isGpuAvailable()) return;

        GpuFactStore store = new GpuFactStore(gpu);

        try {
            List<Atom> facts = Arrays.asList(Atom.parse("test(a)"));

            store.uploadFacts(facts);
            store.uploadFacts(facts);  // Cache hit
            store.uploadFacts(facts);  // Cache hit

            GpuFactStore.StoreStats stats = store.getStats();

            assertEquals(3, stats.uploadCount, "Should count all upload attempts");
            assertEquals(2, stats.cacheHits, "Should have 2 cache hits");

            System.out.println("\nCache Statistics:");
            System.out.println("  " + stats);

        } finally {
            store.cleanup();
        }
    }
}
