package enterprises.iwakura.amber;

import lombok.Data;

/**
 * Progress hint context for download progress updates.
 */
@Data
public class ProgressHintContext {

    private final Dependency dependency;
    private final AmberManifest manifest;
    private final Type type;

    /**
     * Type of the progress hint.
     */
    public enum Type {
        EXISTING,
        START_DOWNLOAD,
        FINISH_DOWNLOAD
    }
}
