/**
 * OpenCL Kernel for Parallel Pattern Matching
 *
 * This kernel matches facts against rule body patterns in parallel.
 * Each work item processes one fact and checks if it matches a pattern.
 *
 * Memory Layout:
 *   facts:      [fact1_data..., fact2_data..., ...]
 *   factSizes:  [size1, size2, ...]
 *   pattern:    [pred_id, arg1_id, arg2_id, ...]  (variables = 0)
 *   matches:    [match_count, fact_idx1, fact_idx2, ...]
 */

/**
 * Checks if a fact matches a pattern.
 *
 * @param fact Pointer to start of fact data [pred, arg1, arg2, ...]
 * @param factSize Number of elements in fact
 * @param pattern Pointer to pattern [pred, arg1, arg2, ...] (0 = variable)
 * @param patternSize Number of elements in pattern
 * @return 1 if match, 0 otherwise
 */
int matches_pattern(__global const int* fact,
                    int factSize,
                    __global const int* pattern,
                    int patternSize) {
    // Facts and patterns must have same size
    if (factSize != patternSize) {
        return 0;
    }

    // Check predicate (must match exactly)
    if (fact[0] != pattern[0]) {
        return 0;
    }

    // Check arguments (0 in pattern = variable = matches anything)
    for (int i = 1; i < factSize; i++) {
        if (pattern[i] != 0 && pattern[i] != fact[i]) {
            return 0;  // Mismatch
        }
    }

    return 1;  // Match!
}

/**
 * Main pattern matching kernel.
 *
 * Each work item processes one fact and checks if it matches the pattern.
 * Matched fact indices are written to the matches buffer.
 *
 * @param facts Flattened array of all facts
 * @param factSizes Size of each fact
 * @param factOffsets Offset to start of each fact in facts array
 * @param numFacts Total number of facts
 * @param pattern Pattern to match against (0 = variable)
 * @param patternSize Size of pattern
 * @param matches Output buffer: [count, idx1, idx2, ...]
 * @param maxMatches Maximum number of matches to store
 */
__kernel void pattern_match(__global const int* facts,
                           __global const int* factSizes,
                           __global const int* factOffsets,
                           const int numFacts,
                           __global const int* pattern,
                           const int patternSize,
                           __global int* matches,
                           const int maxMatches) {
    int factIdx = get_global_id(0);

    // Bounds check
    if (factIdx >= numFacts) {
        return;
    }

    // Get fact data
    int offset = factOffsets[factIdx];
    int size = factSizes[factIdx];
    __global const int* fact = &facts[offset];

    // Check if fact matches pattern
    if (matches_pattern(fact, size, pattern, patternSize)) {
        // Atomically increment match count and get index
        int matchIdx = atomic_inc(&matches[0]);

        // Store fact index if there's room
        if (matchIdx < maxMatches) {
            matches[matchIdx + 1] = factIdx;
        }
    }
}

/**
 * Batch pattern matching kernel - matches against multiple patterns.
 *
 * @param facts Flattened array of all facts
 * @param factSizes Size of each fact
 * @param factOffsets Offset to start of each fact
 * @param numFacts Total number of facts
 * @param patterns Flattened array of patterns
 * @param patternSizes Size of each pattern
 * @param patternOffsets Offset to start of each pattern
 * @param numPatterns Number of patterns
 * @param matches Output buffer: [count, factIdx, patternIdx, factIdx, patternIdx, ...]
 * @param maxMatches Maximum number of matches to store
 */
__kernel void batch_pattern_match(__global const int* facts,
                                 __global const int* factSizes,
                                 __global const int* factOffsets,
                                 const int numFacts,
                                 __global const int* patterns,
                                 __global const int* patternSizes,
                                 __global const int* patternOffsets,
                                 const int numPatterns,
                                 __global int* matches,
                                 const int maxMatches) {
    int factIdx = get_global_id(0);
    int patternIdx = get_global_id(1);

    // Bounds check
    if (factIdx >= numFacts || patternIdx >= numPatterns) {
        return;
    }

    // Get fact data
    int factOffset = factOffsets[factIdx];
    int factSize = factSizes[factIdx];
    __global const int* fact = &facts[factOffset];

    // Get pattern data
    int patternOffset = patternOffsets[patternIdx];
    int patternSize = patternSizes[patternIdx];
    __global const int* pattern = &patterns[patternOffset];

    // Check if fact matches pattern
    if (matches_pattern(fact, factSize, pattern, patternSize)) {
        // Atomically increment match count and get index
        int matchIdx = atomic_inc(&matches[0]);

        // Store fact and pattern indices if there's room
        if (matchIdx < maxMatches) {
            matches[matchIdx * 2 + 1] = factIdx;
            matches[matchIdx * 2 + 2] = patternIdx;
        }
    }
}
