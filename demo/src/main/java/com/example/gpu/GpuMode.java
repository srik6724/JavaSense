package com.example.gpu;

/**
 * GPU acceleration mode for reasoning.
 */
public enum GpuMode {
    /**
     * Never use GPU, always use CPU.
     */
    CPU_ONLY,

    /**
     * Always use GPU if available (fail if not).
     */
    GPU_ONLY,

    /**
     * Automatically decide based on problem size.
     * Uses GPU for large problems, CPU for small ones.
     */
    AUTO
}
