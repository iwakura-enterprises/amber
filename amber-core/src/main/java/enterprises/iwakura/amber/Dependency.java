package enterprises.iwakura.amber;

import lombok.Data;

/**
 * Represents a dependency with its notation, group ID, name, and version.
 */
@Data
public class Dependency {

    /**
     * The dependency notation in the format "groupId:name:version".
     */
    private final String notation;

    /**
     * The group ID of the dependency.
     */
    private final String groupId;

    /**
     * The name of the dependency.
     */
    private final String name;

    /**
     * The version of the dependency.
     */
    private final String version;

    /**
     * Constructs a Dependency object by parsing the given notation.
     *
     * @param notation the dependency notation in the format "groupId:name:version"
     */
    public Dependency(String notation) {
        this.notation = notation;
        String[] parts = notation.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid dependency notation: " + notation);
        }
        this.groupId = parts[0];
        this.name = parts[1];
        this.version = parts[2];
    }

    /**
     * Converts the group ID to a path format by replacing dots with slashes.
     *
     * @return the group ID in path format
     */
    public String getGroupIdAsPath() {
        return groupId.replace('.', '/');
    }

    /**
     * Generates the expected filename for the dependency's JAR file.
     *
     * @return the filename in the format "name-version.jar"
     */
    public String getFileName() {
        return name + "-" + version + ".jar";
    }

    /**
     * Returns the string representation of the dependency, which is its notation.
     *
     * @return the dependency notation
     */
    @Override
    public String toString() {
        return notation;
    }
}
