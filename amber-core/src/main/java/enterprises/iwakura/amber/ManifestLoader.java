package enterprises.iwakura.amber;

import enterprises.iwakura.amber.impl.ClassLoaderManifestLoader;
import enterprises.iwakura.amber.impl.JarFileManifestLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Interface for loading Amber manifests from various sources, such as {@link ClassLoaderManifestLoader} and {@link JarFileManifestLoader}
 */
public interface ManifestLoader {

    /**
     * The path to the manifest file within a JAR or classpath.
     */
    String MANIFEST_FILE_PATH = "META-INF/MANIFEST.MF";

    /**
     * The attribute name for the Amber directory in the manifest.
     */
    String ATTRIBUTE_AMBER_DIRECTORY = "Amber-Directory";

    /**
     * The attribute name for the Amber dependencies in the manifest.
     */
    String ATTRIBUTE_AMBER_DEPENDENCIES = "Amber-Dependencies";

    /**
     * The attribute name for the Amber Maven repositories in the manifest.
     */
    String ATTRIBUTE_AMBER_MAVEN_REPOSITORIES = "Amber-Maven-Repositories";

    /**
     * The delimiter used to split multiple values in manifest attributes.
     */
    String ATTRIBUTE_SPLITTER = ",";

    /**
     * Loads all Amber manifests from the source.
     *
     * @return A list of loaded Amber manifests.
     *
     * @throws IOException If an I/O error occurs during loading.
     */
    List<AmberManifest> loadManifest() throws IOException;

    /**
     * Parses a {@link Manifest} object to extract Amber-specific attributes and create an {@link AmberManifest} instance.
     *
     * @param manifest The manifest to parse.
     *
     * @return An {@link AmberManifest} instance if the manifest contains all Amber attributes, otherwise null.
     */
    default AmberManifest parseManifest(Manifest manifest) {
        Attributes attributes = manifest.getMainAttributes();

        String directoryAttribute = attributes.getValue(ATTRIBUTE_AMBER_DIRECTORY);
        String dependenciesAttribute = attributes.getValue(ATTRIBUTE_AMBER_DEPENDENCIES);
        String mavenRepositoriesAttribute = attributes.getValue(ATTRIBUTE_AMBER_MAVEN_REPOSITORIES);

        if (directoryAttribute == null && dependenciesAttribute == null && mavenRepositoriesAttribute == null) {
            return null; // Not an Amber manifest
        }

        Path directoryPath = directoryAttribute != null ? Paths.get(directoryAttribute) : null;
        List<Dependency> dependencies = new ArrayList<>();
        List<Repository> repositories = new ArrayList<>();

        if (dependenciesAttribute != null) {
            for (String dependency : dependenciesAttribute.split(ATTRIBUTE_SPLITTER)) {
                dependencies.add(new Dependency(dependency.trim()));
            }
        }

        if (mavenRepositoriesAttribute != null) {
            for (String repository : mavenRepositoriesAttribute.split(ATTRIBUTE_SPLITTER)) {
                repositories.add(new Repository(RepositoryType.MAVEN, repository.trim()));
            }
        }

        return new AmberManifest(directoryPath, dependencies, repositories);
    }
}
