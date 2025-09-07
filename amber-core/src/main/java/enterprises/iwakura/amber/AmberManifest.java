package enterprises.iwakura.amber;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.util.List;

/**
 * Amber manifest containing the library directory, dependencies and repositories.
 */
@Data
@RequiredArgsConstructor
public class AmberManifest {

    /**
     * The directory where dependencies should be stored.
     */
    protected final Path directory;

    /**
     * The list of dependencies to download.
     */
    protected final List<Dependency> dependencies;

    /**
     * The list of repositories to use for downloading dependencies.
     */
    protected final List<Repository> repositories;

}
