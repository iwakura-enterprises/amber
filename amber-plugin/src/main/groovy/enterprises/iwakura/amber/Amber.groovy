package enterprises.iwakura.amber

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar

/**
 * Configuration extension for the Amber plugin.
 * Provides settings to customize how Amber handles dependencies.
 */
class AmberExtension {
    /**
     * Directory where Amber will store library files.
     * This is relative to the project directory.
     * <p></p>
     * Default: <code>amber-lib</code>
     */
    String libraryDir = 'amber-lib'
}

/**
 * Amber manifest generation task.
 */
class AmberManifestTask extends DefaultTask {

    @Internal
    def amberConfigurationInternal

    @Internal
    def amberExtensionInternal

    @Input
    String targetJarTaskNameInternal

    @OutputFile
    File getOutputManifestFile() {
        return new File(getProject().buildDir, "tmp/amber/${targetJarTaskNameInternal}Manifest.properties")
    }

    @Input
    Map<String, String> getManifestAttributes() {
        Jar jarTask = getProject().tasks.getByName(targetJarTaskNameInternal) as Jar

        // Directly get all resolved artifacts (including transitive dependencies)
        def allDependencies = project.configurations.amber.resolvedConfiguration.resolvedArtifacts.collect { artifact ->
            def id = artifact.moduleVersion.id
            [group: id.group, name: id.name, version: id.version]
        }

        // Dependency => notation
        def amberDependencies = allDependencies.collect { dep ->
            "${dep.group}:${dep.name}:${dep.version}"
        }.join(',')

        // Collect amber Class-Path entries
        def originalClassPath = [:]
        def amberClassPath = allDependencies.collect { dep ->
            "${amberExtensionInternal.libraryDir}/${dep.name}-${dep.version}.jar"
        }.join(' ')

        // Combine Class-Path entries, if found in the original manifest
        if (jarTask.manifest.hasProperty('inheritMergeSpecs') && jarTask.manifest.inheritMergeSpecs && !jarTask.manifest.inheritMergeSpecs.isEmpty()) {
            // Support for shadowJar's inheritMergeSpecs
            jarTask.manifest.inheritMergeSpecs.each { spec ->
                spec.mergePaths.each { path ->
                    originalClassPath.putAll(path.attributes)
                }
            }
        } else {
            // Fallback to direct attributes
            originalClassPath.putAll(jarTask.manifest.attributes)
        }

        // Get existing Class-Path, if any
        def existingClassPath = originalClassPath.get('Class-Path') ?: ''

        // Combine them together
        def combinedClassPath = existingClassPath
        if (amberClassPath) {
            if (existingClassPath) {
                combinedClassPath += ' ' + amberClassPath
            } else {
                combinedClassPath = amberClassPath
            }
        }

        // Collect maven repositories
        def mavenRepos = getProject().repositories.findAll { it instanceof MavenArtifactRepository }
                .collect { MavenArtifactRepository repo -> repo.url.toString() }
                .join(',')

        return [
                'Amber-Directory': amberExtensionInternal.libraryDir ?: 'amber-lib',
                'Amber-Dependencies': amberDependencies ?: '',
                'Amber-Maven-Repositories': mavenRepos ?: '',
                'Class-Path': combinedClassPath ?: ''
        ]
    }

    // Helper method to recursively collect runtime dependencies
    private void collectRuntimeDependencies(ResolvedDependency dependency, Set<ResolvedDependency> result) {
        dependency.children.each { child ->
            // Check if this is a runtime dependency (could be refined with configuration filter if needed)
            if (!result.contains(child)) {
                result.add(child)
                collectRuntimeDependencies(child, result)
            }
        }
    }

    @TaskAction
    void generateManifest() {
        logger.lifecycle("[>] Generating Amber manifest")

        Jar jarTask = getProject().tasks.getByName(targetJarTaskNameInternal) as Jar
        def amberAttributes = getManifestAttributes()
        def outputFile = getOutputManifestFile()

        logger.lifecycle("[+] Amber-Dependencies: ${amberAttributes['Amber-Dependencies']}")
        logger.lifecycle("[+] Amber-Maven-Repositories: ${amberAttributes['Amber-Maven-Repositories']}")
        logger.lifecycle("[+] Class-Path: ${amberAttributes['Class-Path']}")

        // Create output file, so this task can be UP-TO-DATE if no changes occur
        outputFile.parentFile.mkdirs()
        Properties props = new Properties()
        props.putAll(amberAttributes)
        outputFile.withOutputStream { outputStream ->
            props.store(outputStream, "Amber manifest properties for ${targetJarTaskNameInternal}")
        }

        // Merge attributes into the jar task's manifest
        jarTask.manifest {
            attributes(amberAttributes)
        }
    }

}

/**
 * Amber plugin
 */
class Amber implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def amberExtension = project.extensions.create('amber', AmberExtension)
        // Inside the apply(Project project) method in Amber.groovy
        def amberConfiguration = project.configurations.create('amber')

        // Make sure the configuration is included in the compilation classpath
        project.configurations.compileClasspath.extendsFrom amberConfiguration

        // Dependency handler for 'amber' configuration
        project.ext.amber = { dependencyNotation ->
            project.dependencies.add('amber', dependencyNotation)
        }

        // Configure jar tasks with Amber manifest
        project.afterEvaluate {
            // For all Jar tasks...
            project.tasks.withType(Jar) { jarTask ->
                def jarTaskName = jarTask.name

                if (jarTaskName == 'sourcesJar' || jarTaskName == 'javadocJar') {
                    // Skip sources and javadoc jars
                    return
                }

                // ...create a dedicated task for adding amber manifest attributes
                def manifestTaskName = "generate${jarTask.name.capitalize()}AmberManifest"
                def manifestTask = project.tasks.create(manifestTaskName, AmberManifestTask) {
                    amberConfigurationInternal = amberConfiguration
                    amberExtensionInternal = amberExtension
                    targetJarTaskNameInternal = jarTask.name
                    group = 'amber'
                    description = "Generates Amber manifest attributes for ${jarTask.name}"
                }
                jarTask.dependsOn(manifestTask)
                jarTask.inputs.file(manifestTask.outputManifestFile)

                // Apply the amber manifest attributes. This is needed
                // in case of UP-TO-DATE skip of the amber manifest task.
                jarTask.doFirst {
                    Properties props = new Properties()
                    manifestTask.outputManifestFile.withInputStream { stream ->
                        props.load(stream)
                    }
                    manifest.attributes(props as Map)
                }
            }
        }
    }
}