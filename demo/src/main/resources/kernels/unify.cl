/**
 * OpenCL Kernel for Parallel Variable Unification
 *
 * This kernel extracts variable bindings (substitutions) from matched facts.
 * Given a fact that matches a pattern, it builds a substitution mapping
 * variables to their concrete values.
 *
 * Example:
 *   Pattern: likes(X, Y)  →  [likes_id, 0, 0]  (0 = variable)
 *   Fact:    likes(alice, bob)  →  [likes_id, alice_id, bob_id]
 *   Result:  X=alice_id, Y=bob_id
 */

/**
 * Extracts variable bindings from a matched fact.
 *
 * @param fact The matched fact [pred, arg1, arg2, ...]
 * @param pattern The pattern [pred, var1, var2, ...] (0 = variable)
 * @param size Size of fact/pattern
 * @param varPositions Positions where variables occur (1-indexed)
 * @param numVars Number of variables
 * @param bindings Output: values to bind to variables
 * @return 1 if successful, 0 if failed
 */
int extract_bindings(__global const int* fact,
                     __global const int* pattern,
                     int size,
                     __global const int* varPositions,
                     int numVars,
                     __global int* bindings) {
    for (int i = 0; i < numVars; i++) {
        int pos = varPositions[i];
        if (pos < 0 || pos >= size) {
            return 0;  // Invalid position
        }
        bindings[i] = fact[pos];
    }
    return 1;
}

/**
 * Main unification kernel.
 *
 * For each matched fact, extract variable bindings and store them.
 *
 * @param facts Flattened array of all facts
 * @param factSizes Size of each fact
 * @param factOffsets Offset to start of each fact
 * @param matchedIndices Indices of facts that matched
 * @param numMatches Number of matched facts
 * @param pattern The pattern that was matched
 * @param patternSize Size of pattern
 * @param varPositions Positions where variables occur
 * @param numVars Number of variables in pattern
 * @param substitutions Output: [match0_var0, match0_var1, ..., match1_var0, ...]
 */
__kernel void unify(__global const int* facts,
                   __global const int* factSizes,
                   __global const int* factOffsets,
                   __global const int* matchedIndices,
                   const int numMatches,
                   __global const int* pattern,
                   const int patternSize,
                   __global const int* varPositions,
                   const int numVars,
                   __global int* substitutions) {
    int matchIdx = get_global_id(0);

    // Bounds check
    if (matchIdx >= numMatches) {
        return;
    }

    // Get the matched fact
    int factIdx = matchedIndices[matchIdx];
    int offset = factOffsets[factIdx];
    int size = factSizes[factIdx];
    __global const int* fact = &facts[offset];

    // Extract bindings for this match
    __global int* bindings = &substitutions[matchIdx * numVars];
    extract_bindings(fact, pattern, size, varPositions, numVars, bindings);
}

/**
 * Check if substitution is consistent (no conflicting bindings).
 *
 * @param bindings Variable bindings [var0, var1, ...]
 * @param numVars Number of variables
 * @param constraints Pairs of variables that must be equal: [var1, var2, var1, var2, ...]
 * @param numConstraints Number of constraint pairs
 * @return 1 if consistent, 0 if conflict
 */
int check_consistency(__global const int* bindings,
                     int numVars,
                     __global const int* constraints,
                     int numConstraints) {
    for (int i = 0; i < numConstraints; i++) {
        int var1 = constraints[i * 2];
        int var2 = constraints[i * 2 + 1];

        if (var1 < 0 || var1 >= numVars || var2 < 0 || var2 >= numVars) {
            return 0;  // Invalid constraint
        }

        if (bindings[var1] != bindings[var2]) {
            return 0;  // Conflict: same variable has different values
        }
    }
    return 1;
}

/**
 * Filter substitutions by consistency constraints.
 *
 * This is used when a rule has multiple body atoms with shared variables.
 * Example: rule "friend(X,Y) :- likes(X,Y), likes(Y,X)"
 * Variables X and Y must have consistent bindings across both atoms.
 *
 * @param substitutions Input substitutions
 * @param numMatches Number of matches
 * @param numVars Number of variables per match
 * @param constraints Constraint pairs
 * @param numConstraints Number of constraints
 * @param validFlags Output: 1 if valid, 0 if inconsistent
 */
__kernel void filter_substitutions(__global const int* substitutions,
                                  const int numMatches,
                                  const int numVars,
                                  __global const int* constraints,
                                  const int numConstraints,
                                  __global int* validFlags) {
    int matchIdx = get_global_id(0);

    if (matchIdx >= numMatches) {
        return;
    }

    __global const int* bindings = &substitutions[matchIdx * numVars];

    validFlags[matchIdx] = check_consistency(bindings, numVars, constraints, numConstraints);
}
