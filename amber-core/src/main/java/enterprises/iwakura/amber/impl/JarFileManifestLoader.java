package enterprises.iwakura.amber.impl;

import enterprises.iwakura.amber.AmberManifest;
import enterprises.iwakura.amber.ManifestLoader;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Implementation of {@link ManifestLoader} to load manifest from jar files.
 */
@RequiredArgsConstructor
public class JarFileManifestLoader implements ManifestLoader {

    /**
     * The list of jar file paths to load manifests from.
     */
    protected final List<Path> jarFilePaths;

    @Override
    public List<AmberManifest> loadManifest() throws IOException {
        List<AmberManifest> manifests = new ArrayList<>();

        for (Path jarFilePath : jarFilePaths) {
            try (JarFile jarFile = new JarFile(jarFilePath.toFile())) {
                Manifest manifest = jarFile.getManifest();
                if (manifest != null) {
                    manifests.add(parseManifest(manifest));
                }
            }
        }

        return manifests;
    }
}
