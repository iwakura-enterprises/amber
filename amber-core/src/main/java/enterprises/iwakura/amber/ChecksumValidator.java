package enterprises.iwakura.amber;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Validates the checksum of a file against a given checksum and type.
 */
public interface ChecksumValidator {

    /**
     * Validates the checksum of the given file. On unsupported algorithms, this method returns
     * a {@link ChecksumResult#UNSUPPORTED}
     *
     * @param checksumType the type of checksum (e.g., MD5, SHA-1, SHA-256)
     * @param checksum     the expected checksum value
     * @param file         the file to validate
     *
     * @return the result of the checksum validation
     *
     * @throws IOException if an I/O error occurs
     */
    ChecksumResult validate(ChecksumType checksumType, String checksum, Path file) throws IOException;
}
