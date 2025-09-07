package enterprises.iwakura.amber.impl;

import enterprises.iwakura.amber.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Implementation of {@link DependencyDownloader} that downloads dependencies from any Maven repositories. Supports version overrides via
 * <code>maven-metadata.xml</code>.
 */
public class MavenDependencyDownloader implements DependencyDownloader {

    @Override
    public DownloadResult downloadJar(
            Dependency dependency,
            Repository repository,
            Path filePath
    ) throws IOException {
        Files.createDirectories(filePath.getParent());

        StringDownloadResult versionOverrideResult = downloadVersionOverrideFromMavenMetadata(dependency, repository);
        if (!versionOverrideResult.isSuccess()) {
            return versionOverrideResult;
        }

        HttpURLConnection connection = createConnection(repository.getJarDownloadPath(dependency, versionOverrideResult.getContent() != null ? versionOverrideResult.getContent() : dependency.getVersion()));

        try {
            connection.connect();

            // Check for HTTP 2xx response code
            if (connection.getResponseCode() / 100 != 2) {
                return DownloadResult.error(String.format("HTTP %d: %s", connection.getResponseCode(), connection.getResponseMessage()));
            }

            // Download to temporary file
            try (InputStream inputStream = connection.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            // Successfully downloaded
            return DownloadResult.success();
        } catch (IOException exception) {
            Files.deleteIfExists(filePath);
            return DownloadResult.error(String.format("Failed to download dependency %s: %s", dependency, exception));
        } finally {
            connection.disconnect();
        }
    }

    @Override
    public StringDownloadResult downloadChecksum(Dependency dependency, Repository repository, ChecksumType checksumType) throws IOException {
        StringDownloadResult versionOverrideResult = downloadVersionOverrideFromMavenMetadata(dependency, repository);
        if (!versionOverrideResult.isSuccess()) {
            return versionOverrideResult;
        }

        HttpURLConnection connection = createConnection(repository.getChecksumDownloadPath(dependency, versionOverrideResult.getContent() != null ? versionOverrideResult.getContent() : dependency.getVersion(), checksumType));

        try {
            connection.connect();

            // Check for HTTP 2xx response code
            if (connection.getResponseCode() / 100 != 2) {
                return StringDownloadResult.error(String.format("HTTP %d: %s", connection.getResponseCode(), connection.getResponseMessage()));
            }

            // Read checksum from response
            try (InputStream inputStream = connection.getInputStream()) {
                try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                    byte[] data = new byte[8192];
                    int nRead;
                    while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    buffer.flush();
                    return StringDownloadResult.success(buffer.toString().trim());
                }
            }
        } catch (IOException exception) {
            throw new IOException("Failed to download checksum for dependency: " + dependency, exception);
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Tries to download and parse <code>maven-metadata.xml</code> to find a version override for the given dependency.
     *
     * @param dependency the dependency to check for version override
     * @param repository the repository to download from
     *
     * @return a {@link StringDownloadResult} containing the version override if found, null if not found, or an error message if failed
     */
    protected StringDownloadResult downloadVersionOverrideFromMavenMetadata(Dependency dependency, Repository repository) {
        HttpURLConnection connection = null;
        try {
            connection = createConnection(repository.getDownloadPathDirectory(dependency) + "maven-metadata.xml");
            connection.connect();

            // Check for HTTP 2xx response code
            if (connection.getResponseCode() / 100 != 2) {
                return StringDownloadResult.success(null); // No version override found
            }

            // Read version from response
            try (InputStream inputStream = connection.getInputStream()) {
                try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                    byte[] data = new byte[8192];
                    int nRead;
                    while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    buffer.flush();
                    String metadataXml = buffer.toString().trim();
                    String version = MavenMetadataParser.parseLatestVersion(metadataXml);
                    if (version == null || version.isEmpty()) {
                        return StringDownloadResult.success(null); // No version override found
                    }
                    return StringDownloadResult.success(version);
                }
            }
        } catch (IOException exception) {
            return StringDownloadResult.error("Failed to download maven-metadata.xml for dependency: " + dependency + " due to " + exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Prepares an HTTP GET connection with the default User-Agent.
     *
     * @param url the URL to connect to
     *
     * @return the prepared HttpURLConnection
     *
     * @throws IOException if an I/O error occurs
     */
    protected HttpURLConnection createConnection(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setInstanceFollowRedirects(true);
        return connection;
    }

    /**
     * Simple XML "parser" to extract the latest version from <code>maven-metadata.xml</code>.
     */
    private static class MavenMetadataParser {

        /**
         * Finds the latest version for jar extension in the given <code>maven-metadata.xml</code> content.
         *
         * @param metadataXml the content of <code>maven-metadata.xml</code>
         *
         * @return the latest version if found, otherwise null
         */
        public static String parseLatestVersion(String metadataXml) {
            // Simple extraction for <extension>jar</extension> and its <value>
            int extIdx = metadataXml.indexOf("<extension>jar</extension>");
            if (extIdx == -1) {
                return null;
            }
            int valueStart = metadataXml.indexOf("<value>", extIdx);
            int valueEnd = metadataXml.indexOf("</value>", valueStart);
            if (valueStart == -1 || valueEnd == -1) {
                return null;
            }
            return metadataXml.substring(valueStart + 7, valueEnd);
        }
    }
}
