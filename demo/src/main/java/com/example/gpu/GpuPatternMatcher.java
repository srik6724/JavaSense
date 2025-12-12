package com.example.gpu;

import com.example.Atom;
import com.example.Literal;
import org.jocl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

import static org.jocl.CL.*;

/**
 * GPU-accelerated pattern matching using OpenCL kernels.
 *
 * <p>This class provides host-side wrappers for the OpenCL pattern matching
 * and unification kernels. It handles:</p>
 * <ul>
 *   <li>Loading and compiling OpenCL kernels</li>
 *   <li>Encoding patterns with variables</li>
 *   <li>Executing parallel pattern matching on GPU</li>
 *   <li>Extracting variable substitutions</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * GpuReasoningEngine gpu = new GpuReasoningEngine();
 * GpuFactStore store = new GpuFactStore(gpu);
 * GpuPatternMatcher matcher = new GpuPatternMatcher(gpu, store);
 *
 * // Upload facts
 * store.uploadFacts(facts);
 *
 * // Match pattern
 * List<Literal> pattern = Arrays.asList(Literal.parse("likes(X,Y)"));
 * List<Map<String, String>> substitutions = matcher.findSubstitutions(pattern);
 *
 * matcher.cleanup();
 * }</pre>
 */
public class GpuPatternMatcher {
    private static final Logger logger = LoggerFactory.getLogger(GpuPatternMatcher.class);

    private final GpuReasoningEngine gpuEngine;
    private final GpuFactStore factStore;

    // OpenCL kernels
    private cl_program program;
    private cl_kernel patternMatchKernel;
    private cl_kernel unifyKernel;

    // Phase 5: Work-group size tuning
    private long optimalWorkGroupSize = 0;  // 0 = auto-detect
    private boolean workGroupSizeTuned = false;

    // Statistics
    private int patternsMatched = 0;
    private long totalMatchTimeNs = 0;

    /**
     * Creates a new GPU pattern matcher.
     *
     * @param gpuEngine the GPU engine
     * @param factStore the fact store with uploaded facts
     */
    public GpuPatternMatcher(GpuReasoningEngine gpuEngine, GpuFactStore factStore) {
        if (gpuEngine == null || !gpuEngine.isGpuAvailable()) {
            throw new IllegalStateException("GPU not available");
        }
        if (factStore == null) {
            throw new IllegalArgumentException("Fact store cannot be null");
        }

        this.gpuEngine = gpuEngine;
        this.factStore = factStore;

        loadKernels();
    }

    /**
     * Loads and compiles OpenCL kernels.
     */
    private void loadKernels() {
        try {
            // Load kernel source code
            String patternMatchSource = loadKernelSource("/kernels/pattern_match.cl");
            String unifySource = loadKernelSource("/kernels/unify.cl");
            String combinedSource = patternMatchSource + "\n" + unifySource;

            // Create program
            program = clCreateProgramWithSource(
                gpuEngine.getContext(),
                1,
                new String[]{combinedSource},
                null,
                null
            );

            // Build program
            int ret = clBuildProgram(program, 0, null, null, null, null);

            if (ret != CL_SUCCESS) {
                // Get build log
                long[] logSize = new long[1];
                clGetProgramBuildInfo(program, gpuEngine.getDevice(),
                    CL_PROGRAM_BUILD_LOG, 0, null, logSize);

                byte[] log = new byte[(int) logSize[0]];
                clGetProgramBuildInfo(program, gpuEngine.getDevice(),
                    CL_PROGRAM_BUILD_LOG, log.length, Pointer.to(log), null);

                String logStr = new String(log, 0, log.length - 1);
                throw new RuntimeException("Failed to build OpenCL program:\n" + logStr);
            }

            // Create kernels
            patternMatchKernel = clCreateKernel(program, "pattern_match", null);
            unifyKernel = clCreateKernel(program, "unify", null);

            logger.info("GPU pattern matching kernels loaded successfully");

        } catch (Exception e) {
            logger.error("Failed to load GPU kernels", e);
            throw new RuntimeException("Failed to load GPU kernels", e);
        }
    }

    /**
     * Loads kernel source code from resources.
     */
    private String loadKernelSource(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            return reader.lines().collect(Collectors.joining("\n"));

        } catch (Exception e) {
            throw new RuntimeException("Failed to load kernel: " + resourcePath, e);
        }
    }

    /**
     * Finds all substitutions that match a pattern against stored facts.
     *
     * <p>This is the main entry point for GPU pattern matching.</p>
     *
     * <p><b>Advanced Features (Phase 7):</b></p>
     * <ul>
     *   <li>Multi-literal patterns: {@code a(X,Y), b(Y,Z)}</li>
     *   <li>Negation: {@code not blocked(X)}</li>
     * </ul>
     *
     * @param pattern list of literals
     * @return list of substitutions (variable → value mappings)
     */
    public List<Map<String, String>> findSubstitutions(List<Literal> pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return new ArrayList<>();
        }

        // Handle single literal (fast path)
        if (pattern.size() == 1) {
            Literal literal = pattern.get(0);
            if (literal.isNegated()) {
                return findSubstitutionsWithNegation(pattern);
            }
            return findSubstitutionsForAtom(literal.getAtom());
        }

        // Handle multi-literal pattern with GPU-accelerated joins
        return findSubstitutionsMultiLiteral(pattern);
    }

    /**
     * Finds substitutions for a single atom pattern.
     */
    private List<Map<String, String>> findSubstitutionsForAtom(Atom atomPattern) {
        long startTime = System.nanoTime();

        // Encode pattern (variables → 0)
        PatternEncoding encoding = encodePattern(atomPattern);

        // Execute GPU pattern matching
        int[] matchedIndices = executePatternMatch(encoding.encodedPattern);

        // Extract substitutions
        List<Map<String, String>> substitutions = extractSubstitutions(
            matchedIndices,
            encoding
        );

        long elapsed = System.nanoTime() - startTime;
        totalMatchTimeNs += elapsed;
        patternsMatched++;

        logger.debug("GPU pattern match: {} matches in {:.2f}ms",
            substitutions.size(), elapsed / 1_000_000.0);

        return substitutions;
    }

    /**
     * Encodes a pattern with variables.
     */
    private PatternEncoding encodePattern(Atom pattern) {
        FactEncoder encoder = factStore.getEncoder();

        String predicate = pattern.getPredicate();
        List<String> args = pattern.getArgs();

        // Build encoded pattern and track variables
        List<Integer> encoded = new ArrayList<>();
        Map<String, Integer> varToPosition = new HashMap<>();
        List<String> varNames = new ArrayList<>();

        // Encode predicate
        encoded.add(encoder.getId(predicate));
        if (encoded.get(0) == 0) {
            throw new IllegalArgumentException("Unknown predicate: " + predicate);
        }

        // Encode arguments (variables → 0)
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);

            if (isVariable(arg)) {
                // Variable: encode as 0
                encoded.add(0);
                if (!varToPosition.containsKey(arg)) {
                    varToPosition.put(arg, i + 1);  // 1-indexed (0 is predicate)
                    varNames.add(arg);
                }
            } else {
                // Constant: encode normally
                int id = encoder.getId(arg);
                if (id == 0) {
                    throw new IllegalArgumentException("Unknown constant: " + arg);
                }
                encoded.add(id);
            }
        }

        // Convert to int array
        int[] encodedArray = encoded.stream().mapToInt(Integer::intValue).toArray();

        // Get variable positions
        int[] varPositions = varNames.stream()
            .mapToInt(varToPosition::get)
            .toArray();

        return new PatternEncoding(encodedArray, varNames, varPositions);
    }

    /**
     * Checks if a string is a variable (starts with uppercase).
     */
    private boolean isVariable(String str) {
        return str != null && !str.isEmpty() && Character.isUpperCase(str.charAt(0));
    }

    /**
     * Executes GPU pattern matching kernel.
     *
     * @param pattern encoded pattern
     * @return indices of matched facts
     */
    private int[] executePatternMatch(int[] pattern) {
        cl_context context = gpuEngine.getContext();
        cl_command_queue queue = gpuEngine.getCommandQueue();

        int numFacts = factStore.getFactCount();
        int maxMatches = numFacts;  // Worst case: all facts match

        // Create buffers
        cl_mem patternBuffer = clCreateBuffer(context,
            CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
            (long) pattern.length * Sizeof.cl_int,
            Pointer.to(pattern),
            null);

        // Result buffer: [count, idx1, idx2, ...]
        int[] matchesArray = new int[maxMatches + 1];
        matchesArray[0] = 0;  // Initialize count to 0

        cl_mem matchesBuffer = clCreateBuffer(context,
            CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR,
            (long) matchesArray.length * Sizeof.cl_int,
            Pointer.to(matchesArray),
            null);

        // Calculate fact offsets
        int[] offsets = calculateFactOffsets(factStore.getFactSizes());
        cl_mem offsetsBuffer = clCreateBuffer(context,
            CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
            (long) offsets.length * Sizeof.cl_int,
            Pointer.to(offsets),
            null);

        try {
            // Set kernel arguments
            clSetKernelArg(patternMatchKernel, 0, Sizeof.cl_mem, Pointer.to(factStore.getFactBuffer()));
            clSetKernelArg(patternMatchKernel, 1, Sizeof.cl_mem, Pointer.to(factStore.getFactSizesBuffer()));
            clSetKernelArg(patternMatchKernel, 2, Sizeof.cl_mem, Pointer.to(offsetsBuffer));
            clSetKernelArg(patternMatchKernel, 3, Sizeof.cl_int, Pointer.to(new int[]{numFacts}));
            clSetKernelArg(patternMatchKernel, 4, Sizeof.cl_mem, Pointer.to(patternBuffer));
            clSetKernelArg(patternMatchKernel, 5, Sizeof.cl_int, Pointer.to(new int[]{pattern.length}));
            clSetKernelArg(patternMatchKernel, 6, Sizeof.cl_mem, Pointer.to(matchesBuffer));
            clSetKernelArg(patternMatchKernel, 7, Sizeof.cl_int, Pointer.to(new int[]{maxMatches}));

            // Phase 5: Auto-tune work-group size on first run
            if (!workGroupSizeTuned) {
                optimalWorkGroupSize = autoTuneWorkGroupSize(numFacts);
                workGroupSizeTuned = true;
            }

            // Execute kernel with optimal work-group size
            long[] globalWorkSize = new long[]{numFacts};
            long[] localWorkSize = optimalWorkGroupSize > 0 ? new long[]{optimalWorkGroupSize} : null;

            clEnqueueNDRangeKernel(queue, patternMatchKernel, 1, null,
                globalWorkSize, localWorkSize, 0, null, null);

            // Read results
            clEnqueueReadBuffer(queue, matchesBuffer, CL_TRUE, 0,
                (long) matchesArray.length * Sizeof.cl_int,
                Pointer.to(matchesArray), 0, null, null);

            // Extract matched indices
            int count = matchesArray[0];
            int[] result = new int[count];
            System.arraycopy(matchesArray, 1, result, 0, count);

            return result;

        } finally {
            clReleaseMemObject(patternBuffer);
            clReleaseMemObject(matchesBuffer);
            clReleaseMemObject(offsetsBuffer);
        }
    }

    /**
     * Calculates offsets for each fact in the flattened array.
     */
    private int[] calculateFactOffsets(int[] sizes) {
        int[] offsets = new int[sizes.length];
        int offset = 0;
        for (int i = 0; i < sizes.length; i++) {
            offsets[i] = offset;
            offset += sizes[i];
        }
        return offsets;
    }

    /**
     * Extracts substitutions from matched facts.
     */
    private List<Map<String, String>> extractSubstitutions(
            int[] matchedIndices,
            PatternEncoding encoding) {

        if (matchedIndices.length == 0 || encoding.varNames.isEmpty()) {
            // No matches or no variables
            return matchedIndices.length == 0 ? new ArrayList<>() :
                   Collections.nCopies(matchedIndices.length, new HashMap<>());
        }

        // For each matched fact, extract variable bindings
        List<Map<String, String>> substitutions = new ArrayList<>();

        for (int factIdx : matchedIndices) {
            Atom fact = factStore.getFactAt(factIdx);
            Map<String, String> substitution = new HashMap<>();

            // Extract bindings for each variable
            for (int i = 0; i < encoding.varNames.size(); i++) {
                String varName = encoding.varNames.get(i);
                int position = encoding.varPositions[i];
                String value = fact.getArgs().get(position - 1);  // -1 because position is 1-indexed
                substitution.put(varName, value);
            }

            substitutions.add(substitution);
        }

        return substitutions;
    }

    /**
     * Handles multi-literal patterns using GPU-accelerated iterative joins.
     *
     * <p>Algorithm:</p>
     * <ol>
     *   <li>Process first positive literal on GPU → get initial substitutions</li>
     *   <li>For each remaining literal:</li>
     *   <ul>
     *     <li>If positive: join with current substitutions (GPU-accelerated)</li>
     *     <li>If negative: filter out substitutions that match (GPU-accelerated)</li>
     *   </ul>
     * </ol>
     *
     * @param pattern multi-literal pattern
     * @return list of substitutions
     */
    private List<Map<String, String>> findSubstitutionsMultiLiteral(List<Literal> pattern) {
        logger.debug("GPU multi-literal pattern matching: {} literals", pattern.size());

        // Start with first positive literal
        List<Map<String, String>> currentSubs = new ArrayList<>();
        int startIdx = 0;

        // Find first positive literal to start
        for (int i = 0; i < pattern.size(); i++) {
            if (pattern.get(i).isPositive()) {
                currentSubs = findSubstitutionsForAtom(pattern.get(i).getAtom());
                startIdx = i + 1;
                break;
            }
        }

        // If no positive literal found, return empty (all negations)
        if (currentSubs.isEmpty() && startIdx == 0) {
            logger.warn("Pattern has no positive literals - cannot process on GPU");
            return new ArrayList<>();
        }

        // Process remaining literals
        for (int i = startIdx; i < pattern.size(); i++) {
            Literal literal = pattern.get(i);

            if (literal.isPositive()) {
                // Join with positive literal
                currentSubs = joinWithLiteral(currentSubs, literal.getAtom());
            } else {
                // Filter with negative literal
                currentSubs = filterWithNegation(currentSubs, literal.getAtom());
            }

            // Early termination if no substitutions left
            if (currentSubs.isEmpty()) {
                break;
            }
        }

        return currentSubs;
    }

    /**
     * Joins current substitutions with a positive literal (GPU-accelerated).
     *
     * <p>For each substitution, applies it to the pattern and checks if
     * the grounded pattern matches any facts on GPU.</p>
     *
     * @param currentSubs current substitutions
     * @param pattern atom pattern to join with
     * @return joined substitutions
     */
    private List<Map<String, String>> joinWithLiteral(
            List<Map<String, String>> currentSubs,
            Atom pattern) {

        List<Map<String, String>> results = new ArrayList<>();

        for (Map<String, String> sub : currentSubs) {
            // Apply current substitution to pattern
            Atom groundedPattern = applySubstitution(pattern, sub);

            // Find matches for this grounded pattern
            List<Map<String, String>> newSubs = findSubstitutionsForAtom(groundedPattern);

            // Merge with current substitution
            for (Map<String, String> newSub : newSubs) {
                Map<String, String> merged = new HashMap<>(sub);
                merged.putAll(newSub);
                results.add(merged);
            }
        }

        return results;
    }

    /**
     * Filters substitutions by removing those that match a negative literal.
     *
     * <p>Uses GPU to check if grounded pattern matches any facts.</p>
     *
     * @param currentSubs current substitutions
     * @param pattern atom pattern to check negation
     * @return filtered substitutions
     */
    private List<Map<String, String>> filterWithNegation(
            List<Map<String, String>> currentSubs,
            Atom pattern) {

        List<Map<String, String>> results = new ArrayList<>();

        for (Map<String, String> sub : currentSubs) {
            // Apply current substitution to pattern
            Atom groundedPattern = applySubstitution(pattern, sub);

            // Check if this pattern matches any facts (GPU)
            List<Map<String, String>> matches = findSubstitutionsForAtom(groundedPattern);

            // Keep substitution only if NO matches (negation as failure)
            if (matches.isEmpty()) {
                results.add(sub);
            }
        }

        return results;
    }

    /**
     * Handles patterns with negation using GPU-accelerated negation-as-failure.
     *
     * @param pattern pattern with negation
     * @return list of substitutions
     */
    private List<Map<String, String>> findSubstitutionsWithNegation(List<Literal> pattern) {
        // For single negative literal, we need domain to generate candidates
        // This is tricky - for now, return empty (negation alone has no bindings)
        logger.warn("Single negative literal not yet supported - use with positive literals");
        return new ArrayList<>();
    }

    /**
     * Applies a substitution to an atom pattern.
     *
     * @param pattern atom pattern
     * @param substitution variable substitution
     * @return grounded atom
     */
    private Atom applySubstitution(Atom pattern, Map<String, String> substitution) {
        String predicate = pattern.getPredicate();
        List<String> args = pattern.getArgs();

        List<String> groundedArgs = new ArrayList<>();
        for (String arg : args) {
            if (isVariable(arg) && substitution.containsKey(arg)) {
                groundedArgs.add(substitution.get(arg));
            } else {
                groundedArgs.add(arg);
            }
        }

        return new Atom(predicate, groundedArgs);
    }

    /**
     * Auto-tunes work-group size for optimal performance.
     * <p><b>Phase 5:</b> Queries GPU capabilities and selects optimal work-group size.</p>
     *
     * @param numFacts number of facts to process
     * @return optimal work-group size (or 0 for auto)
     */
    private long autoTuneWorkGroupSize(int numFacts) {
        try {
            // Query maximum work-group size for this kernel
            long[] maxWorkGroupSize = new long[1];
            clGetKernelWorkGroupInfo(patternMatchKernel, gpuEngine.getDevice(),
                CL_KERNEL_WORK_GROUP_SIZE, Sizeof.size_t,
                Pointer.to(maxWorkGroupSize), null);

            // Common good sizes (powers of 2): 32, 64, 128, 256
            long[] candidateSizes = {32, 64, 128, 256};

            // Choose largest that fits
            long optimalSize = 0;
            for (long size : candidateSizes) {
                if (size <= maxWorkGroupSize[0] && numFacts >= size) {
                    optimalSize = size;
                }
            }

            if (optimalSize > 0) {
                logger.debug("Auto-tuned work-group size: {} (max: {})",
                    optimalSize, maxWorkGroupSize[0]);
            } else {
                logger.debug("Using automatic work-group size (max: {})",
                    maxWorkGroupSize[0]);
            }

            return optimalSize;

        } catch (Exception e) {
            logger.warn("Work-group size auto-tuning failed, using automatic: {}",
                e.getMessage());
            return 0;  // Let OpenCL choose
        }
    }

    /**
     * Gets statistics about pattern matching.
     */
    public MatcherStats getStats() {
        return new MatcherStats(
            patternsMatched,
            totalMatchTimeNs,
            patternsMatched > 0 ? totalMatchTimeNs / patternsMatched : 0
        );
    }

    /**
     * Cleans up GPU resources.
     */
    public void cleanup() {
        if (patternMatchKernel != null) {
            clReleaseKernel(patternMatchKernel);
            patternMatchKernel = null;
        }

        if (unifyKernel != null) {
            clReleaseKernel(unifyKernel);
            unifyKernel = null;
        }

        if (program != null) {
            clReleaseProgram(program);
            program = null;
        }

        logger.debug("GPU pattern matcher cleaned up");
    }

    /**
     * Encoding of a pattern with variable information.
     */
    private static class PatternEncoding {
        final int[] encodedPattern;
        final List<String> varNames;
        final int[] varPositions;

        PatternEncoding(int[] encodedPattern, List<String> varNames, int[] varPositions) {
            this.encodedPattern = encodedPattern;
            this.varNames = varNames;
            this.varPositions = varPositions;
        }
    }

    /**
     * Statistics about pattern matching operations.
     */
    public static class MatcherStats {
        public final int patternsMatched;
        public final long totalTimeNs;
        public final long avgTimeNs;

        public MatcherStats(int patternsMatched, long totalTimeNs, long avgTimeNs) {
            this.patternsMatched = patternsMatched;
            this.totalTimeNs = totalTimeNs;
            this.avgTimeNs = avgTimeNs;
        }

        @Override
        public String toString() {
            return String.format("MatcherStats{patterns=%d, avgTime=%.2fms}",
                patternsMatched, avgTimeNs / 1_000_000.0);
        }
    }
}
