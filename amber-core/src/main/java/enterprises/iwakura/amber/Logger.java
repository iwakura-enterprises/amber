package enterprises.iwakura.amber;

/**
 * Logger interface for Amber. Provides methods for logging informational, debug, and error messages.
 */
public interface Logger {

    /**
     * Logs an informational message.
     *
     * @param message The message to log.
     */
    void info(String message);

    /**
     * Logs a debug message. Debug messages are typically more detailed and are used for diagnosing issues during development or troubleshooting.
     *
     * @param message The debug message to log.
     */
    void debug(String message);

    /**
     * Logs an error message along with an optional throwable (exception).
     *
     * @param message   The error message to log.
     * @param throwable An optional throwable associated with the error (can be null).
     */
    void error(String message, Throwable throwable);

}
