package enterprises.iwakura.amber.impl;

import enterprises.iwakura.amber.AmberManifest;
import enterprises.iwakura.amber.ManifestLoader;
import lombok.Data;

import java.io.IOException;
import java.util.List;

@Data
public class TestManifestLoader implements ManifestLoader {

    private final List<AmberManifest> manifests;

    @Override
    public List<AmberManifest> loadManifest() throws IOException {
        return manifests;
    }
}
