package com.example.gpu;

import com.example.Atom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GpuFactStore.
 */
class GpuFactStoreTest {

    private GpuReasoningEngine gpu;
    private GpuFactStore store;

    @BeforeEach
    void setUp() {
        gpu = new GpuReasoningEngine();

        if (!gpu.isGpuAvailable()) {
            System.out.println("Skipping GpuFactStore tests (no GPU available)");
            store = null;
            return;
        }

        store = new GpuFactStore(gpu);
    }

    @AfterEach
    void tearDown() {
        if (store != null) {
            store.cleanup();
        }
        if (gpu != null) {
            gpu.cleanup();
        }
    }

    @Test
    void testCreateStoreWithoutGpu() {
        GpuReasoningEngine noGpu = new GpuReasoningEngine();

        if (noGpu.isGpuAvailable()) {
            System.out.println("GPU available, cannot test no-GPU case");
            return;
        }

        assertThrows(IllegalStateException.class, () -> {
            new GpuFactStore(noGpu);
        });
    }

    @Test
    void testCreateStoreWithNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            new GpuFactStore(null);
        });
    }

    @Test
    void testUploadFacts() {
        if (store == null) return;  // Skip if no GPU

        List<Atom> facts = Arrays.asList(
            Atom.parse("likes(alice,bob)"),
            Atom.parse("popular(bob)"),
            Atom.parse("friend(alice,charlie)")
        );

        store.uploadFacts(facts);

        assertEquals(3, store.getFactCount());
        assertNotNull(store.getEncodedFacts());
        assertNotNull(store.getFactSizes());
        assertNotNull(store.getFactBuffer());
        assertNotNull(store.getFactSizesBuffer());

        System.out.println("Uploaded facts: " + store);
    }

    @Test
    void testUploadEmptyFacts() {
        if (store == null) return;

        // Should not crash
        store.uploadFacts(Arrays.asList());
        assertEquals(0, store.getFactCount());
    }

    @Test
    void testUploadNullFacts() {
        if (store == null) return;

        // Should not crash
        store.uploadFacts(null);
        assertEquals(0, store.getFactCount());
    }

    @Test
    void testGetFactAt() {
        if (store == null) return;

        List<Atom> facts = Arrays.asList(
            Atom.parse("likes(alice,bob)"),
            Atom.parse("popular(bob)"),
            Atom.parse("friend(alice,charlie)")
        );

        store.uploadFacts(facts);

        // Retrieve facts by index
        assertEquals(facts.get(0), store.getFactAt(0));
        assertEquals(facts.get(1), store.getFactAt(1));
        assertEquals(facts.get(2), store.getFactAt(2));
    }

    @Test
    void testGetFactAtInvalidIndex() {
        if (store == null) return;

        List<Atom> facts = Arrays.asList(
            Atom.parse("likes(alice,bob)")
        );

        store.uploadFacts(facts);

        assertThrows(IndexOutOfBoundsException.class, () -> {
            store.getFactAt(-1);
        });

        assertThrows(IndexOutOfBoundsException.class, () -> {
            store.getFactAt(1);
        });
    }

    @Test
    void testGetAllFacts() {
        if (store == null) return;

        List<Atom> original = Arrays.asList(
            Atom.parse("likes(alice,bob)"),
            Atom.parse("popular(bob)"),
            Atom.parse("friend(alice,charlie)")
        );

        store.uploadFacts(original);

        List<Atom> retrieved = store.getAllFacts();
        assertEquals(original, retrieved);
    }

    @Test
    void testGetEncoder() {
        if (store == null) return;

        FactEncoder encoder = store.getEncoder();
        assertNotNull(encoder);

        // Encoder should be used by the store
        store.uploadFacts(Arrays.asList(Atom.parse("test(a,b)")));
        assertTrue(encoder.hasString("test"));
        assertTrue(encoder.hasString("a"));
        assertTrue(encoder.hasString("b"));
    }

    @Test
    void testEncodedFactsStructure() {
        if (store == null) return;

        store.uploadFacts(Arrays.asList(
            Atom.parse("likes(alice,bob)"),
            Atom.parse("popular(bob)")
        ));

        int[] encoded = store.getEncodedFacts();
        int[] sizes = store.getFactSizes();

        assertNotNull(encoded);
        assertNotNull(sizes);

        assertEquals(2, sizes.length);  // 2 facts
        assertEquals(3, sizes[0]);      // likes(alice,bob) = predicate + 2 args
        assertEquals(2, sizes[1]);      // popular(bob) = predicate + 1 arg

        assertEquals(5, encoded.length);  // 3 + 2 total integers

        System.out.println("Encoded: " + Arrays.toString(encoded));
        System.out.println("Sizes: " + Arrays.toString(sizes));
    }

    @Test
    void testGetStats() {
        if (store == null) return;

        store.uploadFacts(Arrays.asList(
            Atom.parse("likes(alice,bob)"),
            Atom.parse("popular(bob)")
        ));

        GpuFactStore.StoreStats stats = store.getStats();

        assertEquals(2, stats.factCount);
        assertEquals(5, stats.totalIntegers);  // 3 + 2
        assertTrue(stats.bytesTransferred > 0);
        assertEquals(1, stats.uploadCount);
        assertNotNull(stats.encoderStats);

        System.out.println("Store stats: " + stats);
    }

    @Test
    void testEstimateGpuMemory() {
        if (store == null) return;

        store.uploadFacts(Arrays.asList(
            Atom.parse("likes(alice,bob)"),
            Atom.parse("popular(bob)")
        ));

        long memory = store.estimateGpuMemory();
        assertTrue(memory > 0);

        // Should be at least: 5 ints (facts) + 2 ints (sizes) = 28 bytes
        assertTrue(memory >= 28);

        System.out.println("Estimated GPU memory: " + memory + " bytes");
    }

    @Test
    void testMultipleUploads() {
        if (store == null) return;

        // First upload
        store.uploadFacts(Arrays.asList(Atom.parse("test1(a)")));
        assertEquals(1, store.getFactCount());

        // Second upload (should replace first)
        store.uploadFacts(Arrays.asList(
            Atom.parse("test2(b)"),
            Atom.parse("test3(c)")
        ));
        assertEquals(2, store.getFactCount());

        GpuFactStore.StoreStats stats = store.getStats();
        assertEquals(2, stats.uploadCount);
    }

    @Test
    void testLargeFacts() {
        if (store == null) return;

        // Create 1000 facts
        List<Atom> facts = new java.util.ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            facts.add(Atom.parse("fact" + i + "(arg" + i + ")"));
        }

        long startTime = System.currentTimeMillis();
        store.uploadFacts(facts);
        long elapsed = System.currentTimeMillis() - startTime;

        assertEquals(1000, store.getFactCount());

        System.out.println("Uploaded 1000 facts in " + elapsed + "ms");
        System.out.println("GPU memory used: " + store.estimateGpuMemory() / 1024.0 + " KB");
    }

    @Test
    void testContainsParallelNotImplemented() {
        if (store == null) return;

        store.uploadFacts(Arrays.asList(Atom.parse("test(a)")));

        assertThrows(UnsupportedOperationException.class, () -> {
            store.containsParallel(Arrays.asList(Atom.parse("test(a)")));
        });
    }

    @Test
    void testCleanup() {
        if (store == null) return;

        store.uploadFacts(Arrays.asList(Atom.parse("test(a)")));

        // Should not throw
        assertDoesNotThrow(() -> store.cleanup());

        // Calling cleanup twice should be safe
        assertDoesNotThrow(() -> store.cleanup());
    }

    @Test
    void testToString() {
        if (store == null) return;

        store.uploadFacts(Arrays.asList(
            Atom.parse("likes(alice,bob)"),
            Atom.parse("popular(bob)")
        ));

        String str = store.toString();
        assertNotNull(str);
        assertTrue(str.contains("facts=2"));

        System.out.println("Store: " + str);
    }

    @Test
    void testMemoryTransferAccounting() {
        if (store == null) return;

        List<Atom> facts = Arrays.asList(
            Atom.parse("a(b)"),
            Atom.parse("c(d)")
        );

        store.uploadFacts(facts);

        GpuFactStore.StoreStats stats = store.getStats();
        long expected = store.getEncodedFacts().length * 4L +  // fact data
                       store.getFactSizes().length * 4L;       // size data

        assertEquals(expected, stats.bytesTransferred);
    }

    @Test
    void testEncoderConsistency() {
        if (store == null) return;

        // Upload facts
        store.uploadFacts(Arrays.asList(
            Atom.parse("likes(alice,bob)"),
            Atom.parse("likes(bob,charlie)")
        ));

        FactEncoder encoder = store.getEncoder();

        // "likes" and "bob" should be interned once
        int likesId = encoder.getId("likes");
        assertTrue(likesId > 0);

        int bobId = encoder.getId("bob");
        assertTrue(bobId > 0);

        // Check that "bob" appears in both facts with same ID
        int[] encoded = store.getEncodedFacts();
        int[] sizes = store.getFactSizes();

        // First fact: likes(alice,bob)
        int[] fact1 = Arrays.copyOfRange(encoded, 0, sizes[0]);
        assertEquals(likesId, fact1[0]);  // predicate
        assertEquals(bobId, fact1[2]);    // second arg

        // Second fact: likes(bob,charlie)
        int[] fact2 = Arrays.copyOfRange(encoded, sizes[0], sizes[0] + sizes[1]);
        assertEquals(likesId, fact2[0]);  // predicate
        assertEquals(bobId, fact2[1]);    // first arg
    }
}
