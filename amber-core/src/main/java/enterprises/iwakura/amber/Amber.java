package enterprises.iwakura.amber;

import enterprises.iwakura.amber.impl.ChecksumValidatorImpl;
import enterprises.iwakura.amber.impl.ClassLoaderManifestLoader;
import enterprises.iwakura.amber.impl.ConsoleLogger;
import enterprises.iwakura.amber.impl.JarFileManifestLoader;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The entrypoint for Amber's bootstrapping functionality.
 * <p>
 * You may create an instance of this class using one of the static factory methods or its constructor.
 * </p>
 */
@Getter
@Setter
@RequiredArgsConstructor
public class Amber {

    /**
     * The manifest loader to load Amber manifests.
     */
    protected final ManifestLoader manifestLoader;

    /**
     * The map of repository types to their corresponding dependency downloaders.
     */
    protected final Map<RepositoryType, DependencyDownloader> downloaders;

    /**
     * The checksum validator to validate downloaded dependencies.
     */
    protected final ChecksumValidator checksumValidator;

    /**
     * The logger to log messages during the bootstrapping process.
     */
    protected final Logger logger;

    /**
     * Indicates whether any dependencies were downloaded during the bootstrapping process.
     */
    protected boolean downloadedSomething = false;

    /**
     * Create an Amber instance that loads manifests from the current thread's context class loader. Uses {@link ConsoleLogger} with debug messages
     * disabled.
     *
     * @return An Amber instance.
     */
    public static Amber classLoader() {
        return classLoader(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Create an Amber instance that loads manifests from the specified class loader. Uses {@link ConsoleLogger} with debug messages disabled.
     *
     * @param classLoader The class loader to load manifests from.
     *
     * @return An Amber instance.
     */
    public static Amber classLoader(ClassLoader classLoader) {
        return classLoader(classLoader, new ConsoleLogger(false));
    }

    /**
     * Create an Amber instance that loads manifests from the specified class loader and uses the specified logger.
     *
     * @param classLoader The class loader to load manifests from.
     * @param logger      The logger to use for logging messages.
     *
     * @return An Amber instance.
     */
    public static Amber classLoader(ClassLoader classLoader, Logger logger) {
        return new Amber(new ClassLoaderManifestLoader(classLoader), DependencyDownloader.defaultDownloaders(), new ChecksumValidatorImpl(), logger);
    }

    /**
     * Create an Amber instance that loads manifests from the specified list of jar file paths. Uses {@link ConsoleLogger} with debug messages
     * disabled.
     *
     * @param jarFilePaths The list of jar file paths to load manifests from.
     *
     * @return An Amber instance.
     */
    public static Amber jarFiles(List<Path> jarFilePaths) {
        return jarFiles(jarFilePaths, new ConsoleLogger(false));
    }

    /**
     * Create an Amber instance that loads manifests from the specified list of jar file paths and uses the specified logger.
     *
     * @param jarFilePaths The list of jar file paths to load manifests from.
     * @param logger       The logger to use for logging messages.
     *
     * @return An Amber instance.
     */
    public static Amber jarFiles(List<Path> jarFilePaths, Logger logger) {
        return new Amber(new JarFileManifestLoader(jarFilePaths), DependencyDownloader.defaultDownloaders(), new ChecksumValidatorImpl(), logger);
    }

    /**
     * Bootstraps dependencies as per the loaded Amber manifests with default options.
     *
     * @return A list of jar files that were required by loaded manifests. This includes dependencies that were already present
     * in the library directories.
     *
     * @throws IOException If an I/O error occurs during bootstrapping.
     */
    public List<Path> bootstrap() throws IOException {
        return bootstrap(BootstrapOptions.builder().build());
    }

    /**
     * Bootstraps dependencies as per the loaded Amber manifests with the specified options.
     *
     * @param options The bootstrap options to use.
     *
     * @return A list of jar files that were required by loaded manifests. This includes dependencies that were already present
     * in the library directories.
     *
     * @throws IOException If an I/O error occurs during bootstrapping.
     */
    public List<Path> bootstrap(BootstrapOptions options) throws IOException {
        logger.info("Bootstrapping...");
        long startTime = System.nanoTime();

        logger.debug("Loading manifests...");
        List<AmberManifest> manifests = manifestLoader.loadManifest();
        if (manifests.isEmpty()) {
            logger.info("No manifests found. Nothing to bootstrap.");
            return Collections.emptyList();
        }
        logger.debug(String.format("Loaded %d manifests.", manifests.size()));

        List<Path> allDependencies = new ArrayList<>();

        logger.debug(String.format("Processing %d manifests...", manifests.size()));
        for (AmberManifest manifest : manifests) {
            allDependencies.addAll(processManifest(manifest, options));
        }

        logger.info(String.format("Bootstrapping completed (took %d ms)", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)));

        if (downloadedSomething && options.getExitCodeAfterDownload() != null) {
            logger.info("Exiting with code " + options.getExitCodeAfterDownload() + " as per configuration.");
            if (options.getExitMessageAfterDownload() != null) {
                logger.info(options.getExitMessageAfterDownload());
            }
            System.exit(options.getExitCodeAfterDownload());
            return null;
        }

        // Reset for potential re-use
        downloadedSomething = false;

        return allDependencies;
    }

    /**
     * Processes a single Amber manifest to download its dependencies as per the specified options.
     *
     * @param manifest the Amber manifest to process
     * @param options  the bootstrap options to use
     *
     * @return a list of paths to the downloaded (or already existing) dependencies
     *
     * @throws IOException if an I/O error occurs during processing
     */
    protected List<Path> processManifest(AmberManifest manifest, BootstrapOptions options) throws IOException {
        List<Path> dependencyPaths = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(options.getDownloaderThreadCount());
        AtomicReference<Exception> lastException = new AtomicReference<>();

        logger.info(String.format("Bootstrapping %d dependencies from %d repositories into %s",
                manifest.getDependencies().size(),
                manifest.getRepositories().size(),
                options.getPrefferedLibraryDirectory(manifest).toAbsolutePath()
        ));

        for (Dependency dependency : manifest.getDependencies()) {
            executorService.execute(() -> {
                if (lastException.get() != null) {
                    logger.debug("Skipping download of " + dependency + " due to previous error.");
                    return; // Skip further processing if an exception has already occurred
                }

                try {
                    long startTime = System.nanoTime();

                    if (isDependencyDownloaded(dependency, manifest, options)) {
                        logger.debug("Dependency exists: " + dependency);
                        dependencyPaths.add(options.getPrefferedLibraryDirectory(manifest).resolve(dependency.getFileName()));
                        return;
                    }

                    Path libraryDir = options.getPrefferedLibraryDirectory(manifest);
                    Path tempJarPath = options.getTempDirectory().resolve(dependency.getFileName() + UUID.randomUUID() + ".part");
                    Path jarPath = libraryDir.resolve(dependency.getFileName());
                    Files.createDirectories(jarPath.getParent());
                    Files.createDirectories(tempJarPath.getParent());

                    Map<Repository, DownloadResult> dependencyDownloadResults = new HashMap<>();
                    StringDownloadResult checksumDownloadResult = null;
                    ChecksumResult checksumResult = ChecksumResult.NOT_FOUND;

                    logger.debug(String.format("Downloading dependency %s from %d repositories...", dependency, manifest.getRepositories().size()));

                    repository_loop:
                    for (Repository repository : manifest.getRepositories()) {
                        DependencyDownloader downloader = downloaders.get(repository.getType());

                        if (downloader == null) {
                            logger.error("No downloader found for repository type: " + repository.getType(), null);
                            continue;
                        }

                        logger.debug(String.format("Attempting to download %s from %s (may not specify the exact version)", dependency, repository.getJarDownloadPath(dependency, null)));
                        DownloadResult result = downloader.downloadJar(dependency, repository, tempJarPath);
                        dependencyDownloadResults.put(repository, result);

                        if (!result.isSuccess()) {
                            logger.debug("Download failed: " + result.getErrorMessage());
                            continue;
                        }

                        if (options.isValidateChecksums()) {
                            logger.debug("Validating checksums for " + dependency);
                            for (ChecksumType checksumType : ChecksumType.values()) {
                                StringDownloadResult tempResult = downloader.downloadChecksum(dependency, repository, checksumType);

                                if (tempResult.isSuccess()) {
                                    checksumResult = checksumValidator.validate(checksumType, tempResult.getContent(), tempJarPath);
                                    logger.debug("Checksum " + checksumType + " validation result: " + checksumResult);

                                    if (checksumResult == ChecksumResult.UNSUPPORTED) {
                                        logger.error("Unsupported algorithm for checksum type: " + checksumType, null);
                                        // Continue on unsupported checksum type
                                    } else {
                                        checksumDownloadResult = tempResult;
                                        break repository_loop; // Break on first (in)valid checksum that is supported
                                    }
                                } else {
                                    logger.debug("Checksum download failed for type " + checksumType + ": " + tempResult.getErrorMessage());
                                    // Assign the last error if no checksum was found yet
                                    checksumDownloadResult = tempResult;
                                }
                            }
                        } else {
                            logger.debug("Skipping checksum validation for " + dependency + " as per configuration.");
                            checksumResult = ChecksumResult.MATCH; // Skip checksum validation
                        }
                    }

                    if (dependencyDownloadResults.values().stream().noneMatch(DownloadResult::isSuccess)) {
                        logger.error(String.format("Could not find %s in repositories: ", dependency), null);
                        manifest.getRepositories().forEach(repository -> {
                            String errorMessage = Optional.ofNullable(dependencyDownloadResults.get(repository))
                                    .map(DownloadResult::getErrorMessage)
                                    .orElse("N/A");
                            logger.error(String.format(" - %s: %s", repository.getUrl(), errorMessage), null);
                        });

                        if (options.isFailOnMissingDependency()) {
                            throw new IOException("Failed to download dependency: " + dependency);
                        }
                    }

                    if (checksumResult != ChecksumResult.MATCH) {
                        if (checksumDownloadResult != null) {
                            logger.error(String.format("Checksum validation failed for %s: %s with error %s", dependency, checksumResult, checksumDownloadResult.getErrorMessage()), null);
                        } else {
                            logger.error(String.format("Checksum validation failed for %s: %s", dependency, checksumResult), null);
                        }

                        if (options.isFailOnInvalidChecksum()) {
                            throw new IOException("Invalid checksum for dependency: " + dependency);
                        }
                    }

                    // Move temp file to library directory
                    logger.debug(String.format("Moving downloaded dependency at %s to %s", tempJarPath, jarPath));
                    Files.move(tempJarPath, jarPath, StandardCopyOption.REPLACE_EXISTING);
                    logger.info(String.format("Downloaded dependency %s to %s (took %d ms)", dependency, jarPath, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)));
                    dependencyPaths.add(jarPath);
                    downloadedSomething = true;
                } catch (Exception exception) {
                    lastException.set(exception);
                }
            });
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(1, TimeUnit.DAYS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (lastException.get() != null) {
            throw new IOException("An error occurred during bootstrapping.", lastException.get());
        }

        return dependencyPaths;
    }

    /**
     * Checks if the specified dependency is already downloaded in the preferred library directory as per the manifest and options.
     *
     * @param dependency the dependency to check
     * @param manifest   the Amber manifest containing the dependency
     * @param options    the bootstrap options to consider
     *
     * @return true if the dependency is already downloaded, false otherwise
     */
    protected boolean isDependencyDownloaded(Dependency dependency, AmberManifest manifest, BootstrapOptions options) {
        if (options.isForceRedownload()) {
            logger.debug("Skipping existence check for " + dependency + " as per configuration.");
            return false;
        }

        Path libraryDir = options.getPrefferedLibraryDirectory(manifest);
        Path jarPath = libraryDir.resolve(dependency.getFileName());
        logger.debug("Checking existence of " + jarPath);
        return Files.exists(jarPath);
    }
}
