package enterprises.iwakura.amber;

/**
 * Enum representing different types of checksums.
 */
public enum ChecksumType {
    SHA512,
    SHA256,
    SHA1,
    MD5;

    /**
     * Gets the file extension associated with the checksum type.
     *
     * @return The file extension (e.g., "sha256" for SHA256).
     */
    public String getFileExtension() {
        return this.name().toLowerCase();
    }
}
