package enterprises.iwakura.amber;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Represents a repository from which dependencies can be downloaded.
 */
@Data
@RequiredArgsConstructor
public class Repository {

    /**
     * The type of the repository (e.g., MAVEN).
     */
    private final RepositoryType type;

    /**
     * The base URL of the repository (e.g., <code>https://repo1.maven.org/maven2/</code>).
     */
    private final String url;

    /**
     * Generates the download path directory for a given dependency. The path is constructed based on the repository URL, group ID, name, and version
     * of the dependency. If you need the precise JAR file path, use {@link #getJarDownloadPath(Dependency, String)}.
     *
     * @param dependency the dependency for which to generate the download path
     *
     * @return the download path directory as a String
     */
    public String getDownloadPathDirectory(Dependency dependency) {
        boolean urlEndsWithSlash = url.endsWith("/");
        return String.format("%1$s%2$s%3$s/%4$s/%5$s/",
                url,
                urlEndsWithSlash ? "" : "/",
                dependency.getGroupIdAsPath(),
                dependency.getName(),
                dependency.getVersion()
        );
    }

    /**
     * Generates the full download path for the JAR file of a given dependency, including the filename. This method allows for an optional version
     * override, which can be useful for handling snapshot versions or other special cases. Uses {@link #getDownloadPathDirectory(Dependency)} to
     * construct the directory part of the path.
     *
     * @param dependency      the dependency for which to generate the JAR download path
     * @param versionOverride an optional version to override the dependency's version; if null, the dependency's version is used
     *
     * @return the full JAR download path as a String
     */
    public String getJarDownloadPath(Dependency dependency, String versionOverride) {
        return String.format("%s%s-%s.jar",
                getDownloadPathDirectory(dependency),
                dependency.getName(),
                versionOverride != null ? versionOverride : dependency.getVersion()
        );
    }

    /**
     * Generates the full download path for the checksum file of a given dependency's JAR file. This method uses
     * {@link #getJarDownloadPath(Dependency, String)} and appends the appropriate checksum file extension based on the specified checksum type.
     *
     * @param dependency      the dependency for which to generate the checksum download path
     * @param versionOverride an optional version to override the dependency's version; if null, the dependency's version is used
     * @param checksumType    the type of checksum (e.g., MD5, SHA-1, SHA-256)
     *
     * @return the full checksum download path as a String
     */
    public String getChecksumDownloadPath(Dependency dependency, String versionOverride, ChecksumType checksumType) {
        return getJarDownloadPath(dependency, versionOverride) + "." + checksumType.getFileExtension();
    }
}
