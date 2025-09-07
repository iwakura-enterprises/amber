package enterprises.iwakura.amber;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum representing different types of repositories.
 */
@RequiredArgsConstructor
@Getter
public enum RepositoryType {
    MAVEN("Amber-Maven-Repositories");

    /**
     * The manifest attribute associated with the repository type.
     */
    private final String manifestAttribute;
}
