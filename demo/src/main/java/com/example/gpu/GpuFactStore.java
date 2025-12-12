package com.example.gpu;

import com.example.Atom;
import org.jocl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.jocl.CL.*;

/**
 * Manages facts in GPU memory for efficient parallel processing.
 *
 * <p>This store handles:</p>
 * <ul>
 *   <li>Encoding atoms to integer arrays</li>
 *   <li>Transferring data to GPU memory</li>
 *   <li>Managing GPU buffers and memory</li>
 *   <li>Parallel containment checks on GPU</li>
 * </ul>
 *
 * <h2>Memory Layout:</h2>
 * <pre>
 * CPU Memory:        GPU Memory:
 * likes(alice,bob)   Buffer: [size, pred_id, arg1_id, arg2_id]
 * likes(bob,charlie) Buffer: [size, pred_id, arg1_id, arg2_id]
 * popular(bob)       Buffer: [size, pred_id, arg1_id]
 * </pre>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * GpuReasoningEngine gpu = new GpuReasoningEngine();
 * GpuFactStore store = new GpuFactStore(gpu);
 *
 * // Add facts
 * List<Atom> facts = Arrays.asList(
 *     Atom.parse("likes(alice,bob)"),
 *     Atom.parse("popular(bob)")
 * );
 * store.uploadFacts(facts);
 *
 * // Check containment on GPU (Phase 3 feature)
 * boolean[] results = store.containsParallel(checkAtoms);
 *
 * store.cleanup();
 * }</pre>
 */
public class GpuFactStore {
    private static final Logger logger = LoggerFactory.getLogger(GpuFactStore.class);

    private final GpuReasoningEngine gpuEngine;
    private final FactEncoder encoder;

    // GPU buffers
    private cl_mem factBuffer;          // Stores encoded facts
    private cl_mem factSizesBuffer;     // Stores size of each fact
    private cl_mem indexBuffer;         // Hash table for fast lookup

    // CPU-side data
    private int[] encodedFacts;
    private int[] factSizes;
    private int factCount = 0;

    // Phase 5: Caching optimization
    private List<Atom> cachedFacts = null;  // Cache to avoid redundant uploads
    private boolean gpuDataValid = false;   // Track if GPU data is up-to-date

    // Statistics
    private long bytesTransferred = 0;
    private int uploadCount = 0;
    private int cacheHits = 0;

    /**
     * Creates a new GPU fact store.
     *
     * @param gpuEngine the GPU engine to use
     */
    public GpuFactStore(GpuReasoningEngine gpuEngine) {
        if (gpuEngine == null) {
            throw new IllegalArgumentException("GPU engine cannot be null");
        }
        if (!gpuEngine.isGpuAvailable()) {
            throw new IllegalStateException("GPU not available");
        }

        this.gpuEngine = gpuEngine;
        this.encoder = new FactEncoder();

        logger.debug("GPU fact store created");
    }

    /**
     * Uploads facts to GPU memory.
     *
     * <p>This encodes atoms to integers and transfers them to GPU memory.</p>
     * <p><b>Phase 5 Optimization:</b> Caches facts to avoid redundant uploads.</p>
     *
     * @param facts list of atoms to upload
     */
    public void uploadFacts(List<Atom> facts) {
        if (facts == null || facts.isEmpty()) {
            logger.warn("No facts to upload");
            return;
        }

        // Phase 5: Check cache to avoid redundant uploads
        if (gpuDataValid && cachedFacts != null && cachedFacts.equals(facts)) {
            cacheHits++;
            logger.debug("Cache hit! Reusing GPU data for {} facts (total hits: {})",
                facts.size(), cacheHits);
            return;
        }

        long startTime = System.currentTimeMillis();

        // Encode facts
        logger.debug("Encoding {} facts...", facts.size());
        List<int[]> encodedList = new ArrayList<>();
        List<Integer> sizeList = new ArrayList<>();

        for (Atom atom : facts) {
            int[] encoded = encoder.encode(atom);
            encodedList.add(encoded);
            sizeList.add(encoded.length);
        }

        // Flatten encoded facts
        int totalInts = encodedList.stream().mapToInt(arr -> arr.length).sum();
        encodedFacts = new int[totalInts];
        factSizes = new int[sizeList.size()];

        int offset = 0;
        for (int i = 0; i < encodedList.size(); i++) {
            int[] encoded = encodedList.get(i);
            System.arraycopy(encoded, 0, encodedFacts, offset, encoded.length);
            factSizes[i] = encoded.length;
            offset += encoded.length;
        }

        factCount = facts.size();

        logger.debug("Encoded {} facts into {} integers ({} bytes)",
            factCount, totalInts, totalInts * 4);

        // Transfer to GPU
        transferToGpu();

        // Phase 5: Update cache
        cachedFacts = new ArrayList<>(facts);  // Copy to avoid external modifications
        gpuDataValid = true;

        long elapsed = System.currentTimeMillis() - startTime;
        uploadCount++;

        logger.info("Uploaded {} facts to GPU in {}ms (transfer: {} KB, cache updated)",
            factCount, elapsed, bytesTransferred / 1024);
    }

    /**
     * Transfers encoded facts to GPU memory.
     */
    private void transferToGpu() {
        cl_context context = gpuEngine.getContext();

        // Cleanup old buffers
        if (factBuffer != null) {
            clReleaseMemObject(factBuffer);
        }
        if (factSizesBuffer != null) {
            clReleaseMemObject(factSizesBuffer);
        }

        // Create GPU buffers
        long factBufferSize = (long) encodedFacts.length * Sizeof.cl_int;
        long sizesBufferSize = (long) factSizes.length * Sizeof.cl_int;

        factBuffer = clCreateBuffer(context,
            CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
            factBufferSize,
            Pointer.to(encodedFacts),
            null);

        factSizesBuffer = clCreateBuffer(context,
            CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
            sizesBufferSize,
            Pointer.to(factSizes),
            null);

        bytesTransferred += factBufferSize + sizesBufferSize;

        logger.debug("Transferred {} bytes to GPU memory", factBufferSize + sizesBufferSize);
    }

    /**
     * Performs parallel containment check on GPU.
     *
     * <p><b>Note:</b> This is a Phase 3 feature - kernel implementation required.</p>
     *
     * @param atoms atoms to check
     * @return boolean array indicating which atoms are present
     */
    public boolean[] containsParallel(List<Atom> atoms) {
        throw new UnsupportedOperationException(
            "GPU containment check not yet implemented (Phase 3 - requires OpenCL kernel)"
        );
    }

    /**
     * Gets the number of facts stored.
     */
    public int getFactCount() {
        return factCount;
    }

    /**
     * Gets the encoder used by this store.
     */
    public FactEncoder getEncoder() {
        return encoder;
    }

    /**
     * Gets encoded facts (CPU-side copy).
     */
    public int[] getEncodedFacts() {
        return encodedFacts;
    }

    /**
     * Gets fact sizes (CPU-side copy).
     */
    public int[] getFactSizes() {
        return factSizes;
    }

    /**
     * Gets the GPU fact buffer.
     */
    public cl_mem getFactBuffer() {
        return factBuffer;
    }

    /**
     * Gets the GPU fact sizes buffer.
     */
    public cl_mem getFactSizesBuffer() {
        return factSizesBuffer;
    }

    /**
     * Decodes a fact by index.
     *
     * @param index fact index (0-based)
     * @return decoded atom
     */
    public Atom getFactAt(int index) {
        if (index < 0 || index >= factCount) {
            throw new IndexOutOfBoundsException("Fact index: " + index);
        }

        // Find offset
        int offset = 0;
        for (int i = 0; i < index; i++) {
            offset += factSizes[i];
        }

        // Extract fact data
        int size = factSizes[index];
        int[] factData = new int[size];
        System.arraycopy(encodedFacts, offset, factData, 0, size);

        return encoder.decode(factData);
    }

    /**
     * Gets all facts as atoms.
     */
    public List<Atom> getAllFacts() {
        List<Atom> facts = new ArrayList<>();
        for (int i = 0; i < factCount; i++) {
            facts.add(getFactAt(i));
        }
        return facts;
    }

    /**
     * Gets statistics about the store.
     */
    public StoreStats getStats() {
        return new StoreStats(
            factCount,
            encodedFacts != null ? encodedFacts.length : 0,
            bytesTransferred,
            uploadCount,
            cacheHits,  // Phase 5: Include cache hits
            encoder.getStats()
        );
    }

    /**
     * Invalidates the GPU cache, forcing next upload to retransfer data.
     * <p><b>Phase 5:</b> Use this when facts have been modified externally.</p>
     */
    public void invalidateCache() {
        gpuDataValid = false;
        cachedFacts = null;
        logger.debug("GPU cache invalidated");
    }

    /**
     * Checks if GPU data is currently valid (cached).
     * <p><b>Phase 5:</b> Returns true if cached data can be reused.</p>
     */
    public boolean isGpuDataValid() {
        return gpuDataValid;
    }

    /**
     * Gets the number of cache hits.
     * <p><b>Phase 5:</b> Number of times redundant uploads were avoided.</p>
     */
    public int getCacheHits() {
        return cacheHits;
    }

    /**
     * Estimates GPU memory usage.
     *
     * @return estimated bytes used on GPU
     */
    public long estimateGpuMemory() {
        long factMemory = encodedFacts != null ? encodedFacts.length * 4L : 0;
        long sizesMemory = factSizes != null ? factSizes.length * 4L : 0;
        return factMemory + sizesMemory;
    }

    /**
     * Cleans up GPU resources.
     */
    public void cleanup() {
        if (factBuffer != null) {
            clReleaseMemObject(factBuffer);
            factBuffer = null;
        }

        if (factSizesBuffer != null) {
            clReleaseMemObject(factSizesBuffer);
            factSizesBuffer = null;
        }

        if (indexBuffer != null) {
            clReleaseMemObject(indexBuffer);
            indexBuffer = null;
        }

        // Phase 5: Invalidate cache on cleanup
        invalidateCache();

        logger.debug("GPU fact store cleaned up");
    }

    @Override
    public String toString() {
        return String.format("GpuFactStore{facts=%d, ints=%d, gpuMem=%.1f KB, uploads=%d}",
            factCount,
            encodedFacts != null ? encodedFacts.length : 0,
            estimateGpuMemory() / 1024.0,
            uploadCount
        );
    }

    /**
     * Statistics about the fact store.
     */
    public static class StoreStats {
        public final int factCount;
        public final int totalIntegers;
        public final long bytesTransferred;
        public final int uploadCount;
        public final int cacheHits;  // Phase 5: Cache statistics
        public final FactEncoder.EncoderStats encoderStats;

        public StoreStats(int factCount, int totalIntegers, long bytesTransferred,
                         int uploadCount, int cacheHits, FactEncoder.EncoderStats encoderStats) {
            this.factCount = factCount;
            this.cacheHits = cacheHits;
            this.totalIntegers = totalIntegers;
            this.bytesTransferred = bytesTransferred;
            this.uploadCount = uploadCount;
            this.encoderStats = encoderStats;
        }

        @Override
        public String toString() {
            return String.format("StoreStats{facts=%d, ints=%d, transferred=%.1f KB, uploads=%d, cacheHits=%d, %s}",
                factCount, totalIntegers, bytesTransferred / 1024.0, uploadCount, cacheHits, encoderStats);
        }
    }
}
