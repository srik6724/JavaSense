package com.example;

import com.example.gpu.GpuMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for OptimizedReasoner with GPU support.
 */
class OptimizedReasonerGpuTest {

    private OptimizedReasoner reasoner;

    @BeforeEach
    void setUp() {
        reasoner = new OptimizedReasoner();
    }

    @AfterEach
    void tearDown() {
        if (reasoner != null) {
            reasoner.cleanup();
        }
    }

    @Test
    void testDefaultGpuMode() {
        // Default should be CPU_ONLY
        assertEquals(GpuMode.CPU_ONLY, reasoner.getGpuMode());
        assertFalse(reasoner.willUseGpu(100));
    }

    @Test
    void testSetGpuModeAuto() {
        reasoner.setGpuMode(GpuMode.AUTO);
        assertEquals(GpuMode.AUTO, reasoner.getGpuMode());

        System.out.println("GPU Info: " + reasoner.getGpuInfo());

        // Small problem shouldn't use GPU
        assertFalse(reasoner.willUseGpu(10));

        // Large problem might use GPU (if available)
        boolean willUse = reasoner.willUseGpu(10000);
        System.out.println("Will use GPU for large problem: " + willUse);
    }

    @Test
    void testSetGpuModeGpuOnly() {
        try {
            reasoner.setGpuMode(GpuMode.GPU_ONLY);

            // If we get here, GPU is available
            assertEquals(GpuMode.GPU_ONLY, reasoner.getGpuMode());
            System.out.println("GPU_ONLY mode enabled: " + reasoner.getGpuInfo());

        } catch (IllegalStateException e) {
            // No GPU available, this is OK
            System.out.println("No GPU available for GPU_ONLY mode (expected on CI)");
            assertTrue(e.getMessage().contains("no GPU available"));
        }
    }

    @Test
    void testCustomGpuThresholds() {
        reasoner.setGpuMode(GpuMode.AUTO);
        reasoner.setGpuThresholds(10, 1, 1000);

        System.out.println("GPU Info: " + reasoner.getGpuInfo());
    }

    @Test
    void testReasoningWithCpuOnly() {
        // Add simple facts and rules
        reasoner.addFact(new TimedFact(
            Atom.parse("likes(alice,bob)"),
            "f1",
            List.of(new Interval(0, 10))
        ));

        reasoner.addRule(new Rule(
            "popular(X) <-0 likes(Y,X)",
            "rule1"
        ));

        // Reason with CPU_ONLY
        reasoner.setGpuMode(GpuMode.CPU_ONLY);
        ReasoningInterpretation result = reasoner.reason(10);

        // Should derive popular(bob)
        assertTrue(result.getFactsAt(0).contains(Atom.parse("popular(bob)")));
    }

    @Test
    void testReasoningWithAutoMode() {
        // Add simple facts and rules
        reasoner.addFact(new TimedFact(
            Atom.parse("likes(alice,bob)"),
            "f1",
            List.of(new Interval(0, 10))
        ));

        reasoner.addRule(new Rule(
            "popular(X) <-0 likes(Y,X)",
            "rule1"
        ));

        // Reason with AUTO mode (will use CPU for small problem)
        reasoner.setGpuMode(GpuMode.AUTO);
        ReasoningInterpretation result = reasoner.reason(10);

        // Should still derive popular(bob) using CPU
        assertTrue(result.getFactsAt(0).contains(Atom.parse("popular(bob)")));
    }

    @Test
    void testWillUseGpuDecision() {
        reasoner.setGpuMode(GpuMode.CPU_ONLY);
        assertFalse(reasoner.willUseGpu(10000));

        reasoner.setGpuMode(GpuMode.AUTO);
        // Decision depends on GPU availability and problem size
        boolean decision = reasoner.willUseGpu(10000);
        System.out.println("AUTO mode will use GPU for 10K timesteps: " + decision);
    }

    @Test
    void testCleanup() {
        reasoner.setGpuMode(GpuMode.AUTO);

        // Should not throw exception
        assertDoesNotThrow(() -> reasoner.cleanup());

        // Calling cleanup twice should be safe
        assertDoesNotThrow(() -> reasoner.cleanup());
    }

    @Test
    void testMultipleReasonersWithGpu() {
        OptimizedReasoner reasoner1 = new OptimizedReasoner();
        OptimizedReasoner reasoner2 = new OptimizedReasoner();

        try {
            reasoner1.setGpuMode(GpuMode.AUTO);
            reasoner2.setGpuMode(GpuMode.AUTO);

            // Both should work independently
            assertNotNull(reasoner1.getGpuInfo());
            assertNotNull(reasoner2.getGpuInfo());

        } finally {
            reasoner1.cleanup();
            reasoner2.cleanup();
        }
    }
}
