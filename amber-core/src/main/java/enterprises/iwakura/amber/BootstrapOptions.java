package enterprises.iwakura.amber;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

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
     * An optional message to print to the console before exiting after downloading dependencies. If null, no message will be printed.
     * {@link #exitCodeAfterDownload} must be set for this to have any effect.
     */
    private String exitMessageAfterDownload;

    /**
     * An optional exit callback function that will be called with the list of all dependency paths before exiting (this includes even
     * those that were downloaded in the past bootstraps. The supplied list is the same one as the one returned by the {@link Amber#bootstrap()}
     * methods). If set, {@link #exitCodeAfterDownload} and {@link #exitMessageAfterDownload} will be
     * ignored. A non-null return value from this function will be used as the exit code. If the return value will be null, the application will
     * <b>not</b> exit. This function will not be invoked if no dependencies were downloaded.
     */
    private Function<List<Path>, Integer> exitCallback;

    /**
     * An optional consumer that will receive progress hints during the bootstrap process.
     * This includes updates for existing and currently downloading dependencies. Be aware that this consumer may be invoked from multiple threads
     * (see {@link #downloaderThreadCount} for more information). Any exceptions thrown by this consumer will be caught and logged,
     * but will <b>not</b> affect the bootstrap process.
     */
    private Consumer<ProgressHintContext> progressHintConsumer;

    /**
     * An optional override for the library directory specified in the manifest. If set, this directory will be used instead of the one in the
     * manifest.
     */
    private Path libraryDirectoryOverride;

    /**
     * The number of threads to use for downloading dependencies. Defaults to twice the number of available processors. A higher number may
     * speed up the bootstrapping process significantly, especially when downloading many small dependencies.
     */
    @Builder.Default
    private int downloaderThreadCount = Runtime.getRuntime().availableProcessors() * 2;

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

    /**
     * Invokes the progress hint consumer with the given context, if it is set.
     *
     * @param context The progress hint context to pass to the consumer.
     */
    public void invokeProgressHintConsumer(ProgressHintContext context, Logger logger) {
        if (progressHintConsumer != null) {
            try {
                progressHintConsumer.accept(context);
            } catch (Exception exception) {
                logger.error("An exception occurred while invoking the progress hint consumer!", exception);
            }
        }
    }
}
