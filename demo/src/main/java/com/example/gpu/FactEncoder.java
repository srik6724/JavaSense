package com.example.gpu;

import com.example.Atom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Encodes Atoms into GPU-friendly integer arrays for efficient processing.
 *
 * <p>GPUs work best with primitive arrays rather than complex objects. This encoder
 * converts Atoms like "likes(alice,bob)" into integer arrays like [1, 2, 3] by
 * using string interning for predicates and arguments.</p>
 *
 * <h2>Encoding Format:</h2>
 * <pre>
 * Atom: likes(alice,bob)
 * →
 * int[]: [predicate_id, arg1_id, arg2_id]
 * →
 * int[]: [1, 2, 3]
 * </pre>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Bidirectional encoding/decoding</li>
 *   <li>String interning for memory efficiency</li>
 *   <li>Support for variable-length argument lists</li>
 *   <li>Thread-safe operations</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * FactEncoder encoder = new FactEncoder();
 *
 * // Encode atom to integers
 * Atom atom = Atom.parse("likes(alice,bob)");
 * int[] encoded = encoder.encode(atom);  // [1, 2, 3]
 *
 * // Decode back to atom
 * Atom decoded = encoder.decode(encoded);  // likes(alice,bob)
 * }</pre>
 */
public class FactEncoder {
    private static final Logger logger = LoggerFactory.getLogger(FactEncoder.class);

    // String interning: bidirectional mapping between strings and IDs
    private final Map<String, Integer> stringToId = new HashMap<>();
    private final Map<Integer, String> idToString = new HashMap<>();
    private int nextId = 1;  // 0 reserved for null/unknown

    // Statistics
    private int atomsEncoded = 0;
    private int atomsDecoded = 0;

    /**
     * Encodes an Atom into an integer array.
     *
     * <p>Format: [predicate_id, arg1_id, arg2_id, ...]</p>
     *
     * @param atom the atom to encode
     * @return integer array representation
     */
    public synchronized int[] encode(Atom atom) {
        if (atom == null) {
            throw new IllegalArgumentException("Cannot encode null atom");
        }

        // Get or create ID for predicate
        int predicateId = getOrCreateId(atom.getPredicate());

        // Get or create IDs for arguments
        List<String> args = atom.getArgs();
        int[] encoded = new int[1 + args.size()];
        encoded[0] = predicateId;

        for (int i = 0; i < args.size(); i++) {
            encoded[i + 1] = getOrCreateId(args.get(i));
        }

        atomsEncoded++;
        logger.trace("Encoded {} -> {}", atom, Arrays.toString(encoded));

        return encoded;
    }

    /**
     * Encodes multiple atoms into a flattened integer array.
     *
     * <p>Format: [size1, pred1, arg1, arg2, ..., size2, pred2, arg1, ...]</p>
     * <p>Each atom is prefixed by its size (number of integers including predicate).</p>
     *
     * @param atoms list of atoms to encode
     * @return flattened array with all atoms
     */
    public synchronized int[] encodeAll(List<Atom> atoms) {
        if (atoms == null || atoms.isEmpty()) {
            return new int[0];
        }

        // Calculate total size
        int totalSize = 0;
        for (Atom atom : atoms) {
            totalSize += 1 + (1 + atom.getArgs().size());  // size prefix + predicate + args
        }

        int[] result = new int[totalSize];
        int offset = 0;

        for (Atom atom : atoms) {
            int[] encoded = encode(atom);
            result[offset++] = encoded.length;  // Size prefix
            System.arraycopy(encoded, 0, result, offset, encoded.length);
            offset += encoded.length;
        }

        logger.debug("Encoded {} atoms into {} integers", atoms.size(), totalSize);

        return result;
    }

    /**
     * Decodes an integer array back to an Atom.
     *
     * @param encoded integer array (format: [predicate_id, arg1_id, arg2_id, ...])
     * @return decoded atom
     */
    public synchronized Atom decode(int[] encoded) {
        if (encoded == null || encoded.length == 0) {
            throw new IllegalArgumentException("Cannot decode null or empty array");
        }

        // Decode predicate
        String predicate = idToString.get(encoded[0]);
        if (predicate == null) {
            throw new IllegalStateException("Unknown predicate ID: " + encoded[0]);
        }

        // Decode arguments
        List<String> args = new ArrayList<>();
        for (int i = 1; i < encoded.length; i++) {
            String arg = idToString.get(encoded[i]);
            if (arg == null) {
                throw new IllegalStateException("Unknown argument ID: " + encoded[i]);
            }
            args.add(arg);
        }

        Atom atom = new Atom(predicate, args);
        atomsDecoded++;
        logger.trace("Decoded {} -> {}", Arrays.toString(encoded), atom);

        return atom;
    }

    /**
     * Decodes a flattened array of atoms.
     *
     * @param encoded flattened array (format: [size1, pred1, arg1, ..., size2, pred2, ...])
     * @return list of decoded atoms
     */
    public synchronized List<Atom> decodeAll(int[] encoded) {
        if (encoded == null || encoded.length == 0) {
            return new ArrayList<>();
        }

        List<Atom> atoms = new ArrayList<>();
        int offset = 0;

        while (offset < encoded.length) {
            int size = encoded[offset++];
            int[] atomData = new int[size];
            System.arraycopy(encoded, offset, atomData, 0, size);
            atoms.add(decode(atomData));
            offset += size;
        }

        logger.debug("Decoded {} atoms from {} integers", atoms.size(), encoded.length);

        return atoms;
    }

    /**
     * Gets or creates an ID for a string (predicate or argument).
     */
    private int getOrCreateId(String str) {
        return stringToId.computeIfAbsent(str, k -> {
            int id = nextId++;
            idToString.put(id, k);
            logger.trace("Interned '{}' -> {}", k, id);
            return id;
        });
    }

    /**
     * Gets the ID for a string if it exists, otherwise returns 0.
     */
    public synchronized int getId(String str) {
        return stringToId.getOrDefault(str, 0);
    }

    /**
     * Gets the string for an ID if it exists, otherwise returns null.
     */
    public synchronized String getString(int id) {
        return idToString.get(id);
    }

    /**
     * Checks if a string has been interned.
     */
    public synchronized boolean hasString(String str) {
        return stringToId.containsKey(str);
    }

    /**
     * Checks if an ID exists.
     */
    public synchronized boolean hasId(int id) {
        return idToString.containsKey(id);
    }

    /**
     * Gets the number of unique strings interned.
     */
    public synchronized int getStringCount() {
        return stringToId.size();
    }

    /**
     * Gets the next ID that will be assigned.
     */
    public synchronized int getNextId() {
        return nextId;
    }

    /**
     * Resets the encoder, clearing all string interning.
     */
    public synchronized void reset() {
        stringToId.clear();
        idToString.clear();
        nextId = 1;
        atomsEncoded = 0;
        atomsDecoded = 0;
        logger.debug("Encoder reset");
    }

    /**
     * Gets encoding statistics.
     */
    public synchronized EncoderStats getStats() {
        return new EncoderStats(
            stringToId.size(),
            atomsEncoded,
            atomsDecoded,
            nextId - 1
        );
    }

    /**
     * Estimates memory usage of the encoder.
     *
     * @return estimated bytes used
     */
    public synchronized long estimateMemoryUsage() {
        // Rough estimate: 2 maps with strings + integers
        long stringBytes = 0;
        for (String str : stringToId.keySet()) {
            stringBytes += str.length() * 2;  // UTF-16
        }

        long mapOverhead = stringToId.size() * 64;  // Map entry overhead
        return stringBytes + mapOverhead * 2;  // Two maps
    }

    /**
     * Gets all interned strings (for debugging).
     */
    public synchronized Map<String, Integer> getAllStrings() {
        return new HashMap<>(stringToId);
    }

    @Override
    public synchronized String toString() {
        return String.format("FactEncoder{strings=%d, encoded=%d, decoded=%d, memory=%.1f KB}",
            stringToId.size(),
            atomsEncoded,
            atomsDecoded,
            estimateMemoryUsage() / 1024.0
        );
    }

    /**
     * Statistics about encoding operations.
     */
    public static class EncoderStats {
        public final int uniqueStrings;
        public final int atomsEncoded;
        public final int atomsDecoded;
        public final int maxIdUsed;

        public EncoderStats(int uniqueStrings, int atomsEncoded, int atomsDecoded, int maxIdUsed) {
            this.uniqueStrings = uniqueStrings;
            this.atomsEncoded = atomsEncoded;
            this.atomsDecoded = atomsDecoded;
            this.maxIdUsed = maxIdUsed;
        }

        @Override
        public String toString() {
            return String.format("EncoderStats{strings=%d, encoded=%d, decoded=%d, maxId=%d}",
                uniqueStrings, atomsEncoded, atomsDecoded, maxIdUsed);
        }
    }
}
