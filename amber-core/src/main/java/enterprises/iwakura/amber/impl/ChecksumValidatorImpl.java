package enterprises.iwakura.amber.impl;

import enterprises.iwakura.amber.ChecksumResult;
import enterprises.iwakura.amber.ChecksumType;
import enterprises.iwakura.amber.ChecksumValidator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Default implementation of {@link ChecksumValidator}. Uses {@link MessageDigest} to compute the checksum of a file.
 */
public class ChecksumValidatorImpl implements ChecksumValidator {

    @Override
    public ChecksumResult validate(ChecksumType checksumType, String checksum, Path file) throws IOException {
        try {
            String fileChecksum = calculateChecksum(checksumType, file);
            if (fileChecksum.equalsIgnoreCase(checksum)) {
                return ChecksumResult.MATCH;
            } else {
                return ChecksumResult.MISMATCH;
            }
        } catch (NoSuchAlgorithmException exception) {
            return ChecksumResult.UNSUPPORTED;
        }
    }

    /**
     * Calculate the checksum of a file using the specified checksum type.
     *
     * @param checksumType the type of checksum to calculate
     * @param file         the file to calculate the checksum for
     *
     * @return the calculated checksum as a hexadecimal string
     *
     * @throws IOException              if an I/O error occurs
     * @throws NoSuchAlgorithmException if the specified checksum type is not supported
     */
    protected String calculateChecksum(ChecksumType checksumType, Path file) throws IOException, NoSuchAlgorithmException {
        try (InputStream inputStream = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance(checksumType.name());
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        }
    }
}
