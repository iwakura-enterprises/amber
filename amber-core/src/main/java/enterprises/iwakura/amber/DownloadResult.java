package enterprises.iwakura.amber;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents the result of a download operation, indicating success or failure and providing an error message if applicable.
 */
@Data
@AllArgsConstructor
public class DownloadResult {

    /**
     * Indicates whether the download was successful.
     */
    private final boolean success;

    /**
     * The error message if the download failed; null if the download was successful.
     */
    private final String errorMessage;

    /**
     * Creates a DownloadResult representing a failed download with the provided error message.
     *
     * @param errorMessage the error message describing the failure
     *
     * @return a DownloadResult indicating failure
     */
    public static DownloadResult error(String errorMessage) {
        return new DownloadResult(false, errorMessage);
    }

    /**
     * Creates a DownloadResult representing a successful download.
     *
     * @return a DownloadResult indicating success
     */
    public static DownloadResult success() {
        return new DownloadResult(true, null);
    }
}
