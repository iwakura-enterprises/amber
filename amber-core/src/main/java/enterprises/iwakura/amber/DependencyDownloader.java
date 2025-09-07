package enterprises.iwakura.amber;

import enterprises.iwakura.amber.impl.MavenDependencyDownloader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Interface for downloading dependencies and their checksums from repositories.
 */
public interface DependencyDownloader {

    /**
     * A default User-Agent string for HTTP requests to identify the Amber library.
     */
    String USER_AGENT = String.format("Amber/%s", Version.VERSION);

    /**
     * Provides a map of default downloaders for each supported repository type.
     *
     * @return A map associating each {@link RepositoryType} with its corresponding {@link DependencyDownloader}.
     */
    static Map<RepositoryType, DependencyDownloader> defaultDownloaders() {
        Map<RepositoryType, DependencyDownloader> downloaders = new HashMap<>();
        downloaders.put(RepositoryType.MAVEN, new MavenDependencyDownloader());
        return downloaders;
    }

    /**
     * Downloads the jar file for the specified dependency from the given repository and saves it to the specified file path.
     *
     * @param dependency the dependency to download
     * @param repository the repository to download from
     * @param filePath   the path to save the downloaded jar file
     *
     * @return a {@link DownloadResult} indicating the success or failure of the download
     *
     * @throws IOException if an I/O error occurs during the download
     */
    DownloadResult downloadJar(Dependency dependency, Repository repository, Path filePath) throws IOException;

    /**
     * Downloads the checksum for the specified dependency from the given repository.
     *
     * @param dependency   the dependency to download the checksum for
     * @param repository   the repository to download from
     * @param checksumType the type of checksum to download (e.g., MD5, SHA-1, SHA-256)
     *
     * @return a {@link StringDownloadResult} containing the checksum string if successful, or an error message if failed
     *
     * @throws IOException if an I/O error occurs during the download
     */
    StringDownloadResult downloadChecksum(Dependency dependency, Repository repository, ChecksumType checksumType) throws IOException;
}
