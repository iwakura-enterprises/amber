package enterprises.iwakura.amber.impl;

import enterprises.iwakura.amber.AmberManifest;

import java.nio.file.Path;
import java.util.List;
import java.util.jar.Manifest;

public class DirectoryEditingJarFileManifestLoader extends JarFileManifestLoader {

    private final Path directory;

    public DirectoryEditingJarFileManifestLoader(List<Path> jarFilePaths, Path directory) {
        super(jarFilePaths);
        this.directory = directory;
    }

    @Override
    public AmberManifest parseManifest(Manifest manifest) {
        AmberManifest amberManifest = super.parseManifest(manifest);
        return new AmberManifest(directory, amberManifest.getDependencies(), amberManifest.getRepositories());
    }
}
