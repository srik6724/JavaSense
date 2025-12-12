package com.example.gpu;

import com.example.Atom;
import com.example.Literal;
import org.jocl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.jocl.CL.*;

/**
 * GPU-accelerated reasoning engine using OpenCL via JOCL.
 *
 * <p>This engine provides GPU-accelerated implementations of the most expensive
 * reasoning operations: pattern matching, variable unification, and set operations.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Automatic GPU detection and initialization</li>
 *   <li>Automatic fallback to CPU if GPU unavailable</li>
 *   <li>Smart decision logic (only use GPU for large problems)</li>
 *   <li>Memory management and cleanup</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * GpuReasoningEngine gpu = new GpuReasoningEngine();
 *
 * if (gpu.isGpuAvailable()) {
 *     System.out.println("GPU: " + gpu.getGpuInfo());
 *
 *     if (gpu.shouldUseGpu(numFacts, numRules, timesteps)) {
 *         // Use GPU-accelerated reasoning
 *         List<Substitution> results = gpu.findSubstitutionsGpu(...);
 *     }
 * }
 *
 * gpu.cleanup();
 * }</pre>
 */
public class GpuReasoningEngine {
    private static final Logger logger = LoggerFactory.getLogger(GpuReasoningEngine.class);

    // OpenCL objects
    private cl_context context;
    private cl_command_queue commandQueue;
    private cl_device_id device;
    private cl_platform_id platform;

    // GPU availability
    private boolean gpuAvailable = false;
    private String gpuInfo = "No GPU detected";

    // Configuration thresholds
    private int minFactsForGpu = 1000;
    private int minRulesForGpu = 10;
    private int minComplexityForGpu = 100_000;  // facts × rules × timesteps

    /**
     * Initializes the GPU reasoning engine and detects available GPUs.
     */
    public GpuReasoningEngine() {
        try {
            initializeGpu();
        } catch (Exception e) {
            logger.warn("GPU initialization failed: {}", e.getMessage());
            logger.debug("GPU initialization error details", e);
            gpuAvailable = false;
        }
    }

    /**
     * Initializes OpenCL context and detects GPU devices.
     */
    private void initializeGpu() {
        // Enable exceptions for OpenCL errors
        CL.setExceptionsEnabled(true);

        // Get platform
        int[] numPlatforms = new int[1];
        clGetPlatformIDs(0, null, numPlatforms);

        if (numPlatforms[0] == 0) {
            logger.info("No OpenCL platforms found");
            return;
        }

        cl_platform_id[] platforms = new cl_platform_id[numPlatforms[0]];
        clGetPlatformIDs(platforms.length, platforms, null);
        platform = platforms[0];

        logger.debug("Found {} OpenCL platform(s)", numPlatforms[0]);

        // Get GPU devices
        int[] numDevices = new int[1];
        clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, 0, null, numDevices);

        if (numDevices[0] == 0) {
            // Try CPU as fallback
            logger.debug("No GPU devices found, trying CPU devices");
            clGetDeviceIDs(platform, CL_DEVICE_TYPE_CPU, 0, null, numDevices);

            if (numDevices[0] == 0) {
                logger.info("No OpenCL devices found");
                return;
            }
        }

        cl_device_id[] devices = new cl_device_id[numDevices[0]];
        clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, devices.length, devices, null);
        device = devices[0];

        // Get device info
        gpuInfo = getDeviceInfo(device);
        logger.info("GPU detected: {}", gpuInfo);

        // Create context
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        context = clCreateContext(
            contextProperties, 1, new cl_device_id[]{device},
            null, null, null
        );

        // Create command queue
        cl_queue_properties properties = new cl_queue_properties();
        commandQueue = clCreateCommandQueueWithProperties(context, device, properties, null);

        gpuAvailable = true;
        logger.info("GPU reasoning engine initialized successfully");
    }

    /**
     * Gets detailed information about the GPU device.
     */
    private String getDeviceInfo(cl_device_id device) {
        // Get device name
        long[] size = new long[1];
        clGetDeviceInfo(device, CL_DEVICE_NAME, 0, null, size);
        byte[] buffer = new byte[(int) size[0]];
        clGetDeviceInfo(device, CL_DEVICE_NAME, buffer.length, Pointer.to(buffer), null);
        String deviceName = new String(buffer, 0, buffer.length - 1);

        // Get compute units
        int[] computeUnits = new int[1];
        clGetDeviceInfo(device, CL_DEVICE_MAX_COMPUTE_UNITS, Sizeof.cl_int,
            Pointer.to(computeUnits), null);

        // Get global memory size
        long[] globalMemSize = new long[1];
        clGetDeviceInfo(device, CL_DEVICE_GLOBAL_MEM_SIZE, Sizeof.cl_long,
            Pointer.to(globalMemSize), null);

        return String.format("%s (%d compute units, %.1f GB memory)",
            deviceName.trim(),
            computeUnits[0],
            globalMemSize[0] / (1024.0 * 1024.0 * 1024.0)
        );
    }

    /**
     * Checks if GPU is available and initialized.
     */
    public boolean isGpuAvailable() {
        return gpuAvailable;
    }

    /**
     * Gets GPU device information.
     */
    public String getGpuInfo() {
        return gpuInfo;
    }

    /**
     * Decides whether to use GPU based on problem size.
     *
     * <p>GPU has overhead from data transfer, so only worth it for large problems.</p>
     *
     * @param numFacts number of facts
     * @param numRules number of rules
     * @param timesteps number of timesteps
     * @return true if GPU should be used
     */
    public boolean shouldUseGpu(int numFacts, int numRules, int timesteps) {
        if (!gpuAvailable) {
            return false;
        }

        // Check minimum thresholds
        if (numFacts < minFactsForGpu || numRules < minRulesForGpu) {
            logger.debug("Problem too small for GPU (facts={}, rules={})", numFacts, numRules);
            return false;
        }

        // Check complexity threshold
        long complexity = (long) numFacts * numRules * timesteps;
        if (complexity < minComplexityForGpu) {
            logger.debug("Problem complexity too low for GPU ({})", complexity);
            return false;
        }

        logger.info("Using GPU for reasoning (facts={}, rules={}, timesteps={}, complexity={})",
            numFacts, numRules, timesteps, complexity);
        return true;
    }

    /**
     * Sets minimum number of facts required to use GPU.
     */
    public void setMinFactsForGpu(int minFacts) {
        this.minFactsForGpu = minFacts;
    }

    /**
     * Sets minimum number of rules required to use GPU.
     */
    public void setMinRulesForGpu(int minRules) {
        this.minRulesForGpu = minRules;
    }

    /**
     * Sets minimum complexity threshold for GPU usage.
     */
    public void setMinComplexityForGpu(int minComplexity) {
        this.minComplexityForGpu = minComplexity;
    }

    /**
     * Gets the OpenCL context (for advanced usage).
     */
    public cl_context getContext() {
        return context;
    }

    /**
     * Gets the OpenCL command queue (for advanced usage).
     */
    public cl_command_queue getCommandQueue() {
        return commandQueue;
    }

    /**
     * Gets the OpenCL device (for advanced usage).
     */
    public cl_device_id getDevice() {
        return device;
    }

    /**
     * GPU-accelerated pattern matching (Phase 3).
     *
     * <p>This method uploads facts to GPU, performs pattern matching, and
     * returns variable substitutions.</p>
     *
     * @param bodyLiterals pattern to match
     * @param facts facts to match against
     * @param timestep current timestep (unused in Phase 3)
     * @return list of variable substitutions
     */
    public List<Map<String, String>> findSubstitutionsGpu(
            List<Literal> bodyLiterals,
            List<Atom> facts,
            int timestep) {

        if (!gpuAvailable) {
            throw new IllegalStateException("GPU not available");
        }

        // Create temporary fact store
        GpuFactStore store = new GpuFactStore(this);
        try {
            // Upload facts
            store.uploadFacts(facts);

            // Create pattern matcher
            GpuPatternMatcher matcher = new GpuPatternMatcher(this, store);
            try {
                // Find substitutions
                return matcher.findSubstitutions(bodyLiterals);
            } finally {
                matcher.cleanup();
            }
        } finally {
            store.cleanup();
        }
    }

    /**
     * Cleans up GPU resources.
     * Should be called when done using the GPU engine.
     */
    public void cleanup() {
        if (commandQueue != null) {
            clReleaseCommandQueue(commandQueue);
            commandQueue = null;
        }

        if (context != null) {
            clReleaseContext(context);
            context = null;
        }

        logger.debug("GPU resources cleaned up");
    }

    /**
     * Gets statistics about GPU usage.
     */
    public GpuStats getStats() {
        return new GpuStats(
            gpuAvailable,
            gpuInfo,
            minFactsForGpu,
            minRulesForGpu,
            minComplexityForGpu
        );
    }

    /**
     * Statistics about GPU configuration and usage.
     */
    public static class GpuStats {
        public final boolean gpuAvailable;
        public final String gpuInfo;
        public final int minFactsForGpu;
        public final int minRulesForGpu;
        public final int minComplexityForGpu;

        public GpuStats(boolean gpuAvailable, String gpuInfo,
                       int minFactsForGpu, int minRulesForGpu, int minComplexityForGpu) {
            this.gpuAvailable = gpuAvailable;
            this.gpuInfo = gpuInfo;
            this.minFactsForGpu = minFactsForGpu;
            this.minRulesForGpu = minRulesForGpu;
            this.minComplexityForGpu = minComplexityForGpu;
        }

        @Override
        public String toString() {
            return "GpuStats{" +
                    "available=" + gpuAvailable +
                    ", info='" + gpuInfo + '\'' +
                    ", minFacts=" + minFactsForGpu +
                    ", minRules=" + minRulesForGpu +
                    ", minComplexity=" + minComplexityForGpu +
                    '}';
        }
    }
}
