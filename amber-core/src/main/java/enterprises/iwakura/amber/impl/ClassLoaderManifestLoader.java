package enterprises.iwakura.amber.impl;

import enterprises.iwakura.amber.AmberManifest;
import enterprises.iwakura.amber.ManifestLoader;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Manifest;

/**
 * Implementation of {@link ManifestLoader} to load manifest from a class loader's resources.
 */
@RequiredArgsConstructor
public class ClassLoaderManifestLoader implements ManifestLoader {

    /**
     * The class loader to load manifests from.
     */
    private final ClassLoader classLoader;

    @Override
    public List<AmberManifest> loadManifest() throws IOException {
        List<AmberManifest> amberManifests = new ArrayList<>();

        Enumeration<URL> manifests = classLoader.getResources(MANIFEST_FILE_PATH);
        while (manifests.hasMoreElements()) {
            URL manifestUrl = manifests.nextElement();
            try (InputStream inputStream = manifestUrl.openStream()) {
                Manifest manifest = new Manifest(inputStream);
                AmberManifest amberManifest = parseManifest(manifest);
                if (amberManifest != null) {
                    amberManifests.add(amberManifest);
                }
            }
        }

        return amberManifests;
    }
}
