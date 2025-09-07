package enterprises.iwakura.amber;

import lombok.Getter;

/**
 * Extends {@link DownloadResult} to include the downloaded content as a String.
 */
@Getter
public class StringDownloadResult extends DownloadResult {

    /**
     * The downloaded content as a String, or null if the download failed.
     */
    private final String content;

    /**
     * Creates a new StringDownloadResult.
     *
     * @param success      whether the download was successful
     * @param content      the downloaded content as a String, or null if the download failed
     * @param errorMessage the error message if the download failed, or null if the download was successful
     */
    public StringDownloadResult(boolean success, String content, String errorMessage) {
        super(success, errorMessage);
        this.content = content;
    }

    /**
     * Creates a StringDownloadResult representing a failed download with the provided error message.
     *
     * @param errorMessage the error message describing the failure
     *
     * @return a StringDownloadResult indicating failure
     */
    public static StringDownloadResult error(String errorMessage) {
        return new StringDownloadResult(false, null, errorMessage);
    }

    /**
     * Creates a StringDownloadResult representing a successful download with the provided content.
     *
     * @param content the downloaded content as a String
     *
     * @return a StringDownloadResult indicating success
     */
    public static StringDownloadResult success(String content) {
        return new StringDownloadResult(true, content, null);
    }
}
