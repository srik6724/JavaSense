package com.example.gpu;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GPU Reasoning Engine.
 */
class GpuReasoningEngineTest {

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
    void testGpuInitialization() {
        assertNotNull(gpu);
        assertNotNull(gpu.getGpuInfo());

        // GPU may or may not be available depending on hardware
        // Just check that initialization doesn't crash
        System.out.println("GPU Info: " + gpu.getGpuInfo());
    }

    @Test
    void testGpuAvailability() {
        boolean available = gpu.isGpuAvailable();

        if (available) {
            System.out.println("GPU detected: " + gpu.getGpuInfo());
            assertNotNull(gpu.getContext());
            assertNotNull(gpu.getCommandQueue());
            assertNotNull(gpu.getDevice());
        } else {
            System.out.println("No GPU detected (this is OK for CI environments)");
        }
    }

    @Test
    void testShouldUseGpuWithSmallProblem() {
        // Small problem should not use GPU (overhead too high)
        assertFalse(gpu.shouldUseGpu(10, 5, 10));
        assertFalse(gpu.shouldUseGpu(100, 10, 10));
    }

    @Test
    void testShouldUseGpuWithLargeProblem() {
        if (!gpu.isGpuAvailable()) {
            System.out.println("Skipping GPU decision test (no GPU available)");
            return;
        }

        // Large problem should use GPU
        assertTrue(gpu.shouldUseGpu(10000, 100, 100));
        assertTrue(gpu.shouldUseGpu(100000, 500, 500));
    }

    @Test
    void testCustomThresholds() {
        // Set low thresholds
        gpu.setMinFactsForGpu(10);
        gpu.setMinRulesForGpu(1);
        gpu.setMinComplexityForGpu(1000);

        if (gpu.isGpuAvailable()) {
            assertTrue(gpu.shouldUseGpu(100, 10, 10));
        }
    }

    @Test
    void testGpuStats() {
        GpuReasoningEngine.GpuStats stats = gpu.getStats();

        assertNotNull(stats);
        assertNotNull(stats.gpuInfo);
        assertTrue(stats.minFactsForGpu > 0);
        assertTrue(stats.minRulesForGpu > 0);
        assertTrue(stats.minComplexityForGpu > 0);

        System.out.println("GPU Stats: " + stats);
    }

    @Test
    void testCleanup() {
        // Should not throw exception
        assertDoesNotThrow(() -> gpu.cleanup());

        // Calling cleanup twice should be safe
        assertDoesNotThrow(() -> gpu.cleanup());
    }

    @Test
    void testFindSubstitutionsNotYetImplemented() {
        // Phase 1: GPU pattern matching not yet implemented
        assertThrows(UnsupportedOperationException.class, () -> {
            gpu.findSubstitutionsGpu(null, null, 0);
        });
    }

    @Test
    void testMultipleEngineInstances() {
        // Should be able to create multiple engines
        GpuReasoningEngine gpu2 = new GpuReasoningEngine();

        assertNotNull(gpu2);
        assertEquals(gpu.isGpuAvailable(), gpu2.isGpuAvailable());

        gpu2.cleanup();
    }
}
