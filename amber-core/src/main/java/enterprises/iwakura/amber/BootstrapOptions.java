package enterprises.iwakura.amber;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Options for Amber's bootstrap process. Holds various settings that control how dependencies are downloaded and validated.
 * <p>
 * There are some default options:
 * <ul>
 *     <li>Temporary Directory: System's default temporary directory (System property <code>java.io.tmpdir</code>)</li>
 *     <li>Validate Checksums: <code>true</code></li>
 *     <li>Fail on Invalid Checksum: <code>true</code></li>
 *     <li>Force Redownload: <code>false</code></li>
 *     <li>Fail on Missing Dependency: <code>true</code></li>
 *     <li>Exit Code After Download: <code>null</code> (no exit)</li>
 *     <li>Library Directory Override: <code>null</code> (use manifest's directory)</li>
 * </ul>
 * You may use the builder to easily create an instance with custom settings.
 */
@Data
@Builder
public class BootstrapOptions {

    /**
     * The temporary directory to use for downloading dependencies before moving them to the final location.
     */
    @Builder.Default
    private Path tempDirectory = Paths.get(System.getProperty("java.io.tmpdir"));

    /**
     * Whether to validate checksums of downloaded dependencies.
     */
    @Builder.Default
    private boolean validateChecksums = true;

    /**
     * Whether to fail the bootstrap process if a downloaded dependency has an invalid checksum.
     */
    @Builder.Default
    private boolean failOnInvalidChecksum = true;

    /**
     * Whether to force re-downloading dependencies even if they already exist in the target directory.
     */
    @Builder.Default
    private boolean forceRedownload = false;

    /**
     * Whether to fail the bootstrap process if a dependency cannot be found in any repository.
     */
    @Builder.Default
    private boolean failOnMissingDependency = true;

    /**
     * The exit code to use if the application should exit after downloading dependencies. If null, the application will not exit.
     */
    private Integer exitCodeAfterDownload;

    /**
     * An optional override for the library directory specified in the manifest. If set, this directory will be used instead of the one in the
     * manifest.
     */
    private Path libraryDirectoryOverride;

    /**
     * Get the preferred library directory, using the override if set, otherwise falling back to the manifest's directory.
     *
     * @param manifest The Amber manifest to get the directory from if no override is set.
     *
     * @return The preferred library directory.
     */
    public Path getPrefferedLibraryDirectory(AmberManifest manifest) {
        return libraryDirectoryOverride != null ? libraryDirectoryOverride : manifest.getDirectory();
    }
}
