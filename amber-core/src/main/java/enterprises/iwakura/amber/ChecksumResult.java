package enterprises.iwakura.amber;

/**
 * Represents the result of a checksum verification operation.
 */
public enum ChecksumResult {
    /**
     * The checksum matches the expected value.
     */
    MATCH,

    /**
     * The checksum does not match the expected value.
     */
    MISMATCH,

    /**
     * The checksum file was not found.
     */
    NOT_FOUND,

    /**
     * The checksum algorithm is unsupported by the JVM/system.
     */
    UNSUPPORTED;
}
