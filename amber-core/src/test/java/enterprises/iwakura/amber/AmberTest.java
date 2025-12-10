package enterprises.iwakura.amber;

import enterprises.iwakura.amber.impl.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AmberTest {

    @TempDir
    Path tempDir;

    @Test
    public void testDownload() throws IOException {
        // Arrange
        List<ProgressHintContext> hints = new ArrayList<>();
        List<Path> testFiles = new ArrayList<>();
        testFiles.add(Paths.get("../test-file/leyline-1.0-SNAPSHOT-all.jar"));

        Path manifest1Directory = tempDir.resolve("manifest1/");
        Path manifest2Directory = tempDir.resolve("manifest2/");
        Path manifest3Directory = tempDir.resolve("manifest3/");
        AmberManifest manifest1 = new AmberManifest(manifest1Directory, new ArrayList<>(), new ArrayList<>());
        AmberManifest manifest2 = new AmberManifest(manifest2Directory, new ArrayList<>(), new ArrayList<>());
        AmberManifest manifest3 = new DirectoryEditingJarFileManifestLoader(testFiles, manifest3Directory).loadManifest().get(0);

        addRepositories(manifest1, RepositoryType.MAVEN, "https://repo.maven.apache.org/maven2/");
        addRepositories(manifest1, RepositoryType.MAVEN, "https://repo.aikar.co/content/groups/aikar/");
        addRepositories(manifest2, RepositoryType.MAVEN, "https://repo.maven.apache.org/maven2/");

        addDependencies(manifest1,
                "org.apache.commons:commons-lang3:3.18.0",
                "com.google.code.gson:gson:2.13.1",
                "co.aikar:acf-paper:0.5.1-SNAPSHOT",
                "io.prometheus:prometheus-metrics-core:1.3.7",
                "io.prometheus:prometheus-metrics-instrumentation-jvm:1.3.7",
                "io.prometheus:prometheus-metrics-exporter-httpserver:1.3.7",
                "org.hibernate.orm:hibernate-core:7.0.0.CR1",
                "org.hibernate.orm:hibernate-processor:7.0.0.CR1",
                "org.hibernate.orm:hibernate-hikaricp:7.0.0.CR1",
                "jakarta.transaction:jakarta.transaction-api:2.0.0",
                "org.mariadb.jdbc:mariadb-java-client:3.5.3",
                "org.liquibase:liquibase-core:3.10.3",
                "com.mattbertolini:liquibase-slf4j:5.1.0",
                "io.sentry:sentry-log4j2:8.12.0",
                "dev.mayuna:mayus-json-utilities:2.1"
        );

        addDependencies(manifest2,
                "org.apache.commons:commons-collections4:4.5.0",
                "org.slf4j:slf4j-api:1.7.36"
        );

        List<AmberManifest> manifests = new ArrayList<>();
        manifests.add(manifest1);
        manifests.add(manifest2);
        manifests.add(manifest3);

        Map<RepositoryType, DependencyDownloader> downloaders = new HashMap<>();
        downloaders.put(RepositoryType.MAVEN, new MavenDependencyDownloader());

        Amber amber = new Amber(
                new TestManifestLoader(manifests),
                downloaders,
                new ChecksumValidatorImpl(),
                new ConsoleLogger(false)
        );

        // Act
        amber.bootstrap(BootstrapOptions.builder()
                .validateChecksums(true)
                .failOnInvalidChecksum(true)
                .forceRedownload(false)
                .failOnMissingDependency(true)
                .libraryDirectoryOverride(null)
                .progressHintCallback(hints::add)
                .build()
        );

        // Assert
        // Some files in manifest1
        assertEquals(15, Files.list(manifest1Directory).count());

        // Some files in manifest2
        assertEquals(2, Files.list(manifest2Directory).count());

        // Some files in manifest3
        assertEquals(19, Files.list(manifest3Directory).count());

        // There were progress hints
        assertFalse(hints.isEmpty());
    }

    private void addDependencies(AmberManifest manifest, String... dependencies) {
        for (String dep : dependencies) {
            manifest.getDependencies().add(new Dependency(dep));
        }
    }

    private void addRepositories(AmberManifest manifest, RepositoryType type, String... urls) {
        for (String url : urls) {
            manifest.getRepositories().add(new Repository(type, url));
        }
    }
}
