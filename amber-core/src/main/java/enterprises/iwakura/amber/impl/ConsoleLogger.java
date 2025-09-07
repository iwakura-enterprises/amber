package enterprises.iwakura.amber.impl;

/**
 * Extension of {@link PrintStreamLogger} that logs to the console ({@link System#out} for info/debug and {@link System#err} for errors).
 */
public class ConsoleLogger extends PrintStreamLogger {

    /**
     * Creates a new {@link ConsoleLogger} instance with the specified debug mode.
     *
     * @param debugEnabled Whether debug logging is enabled.
     */
    public ConsoleLogger(boolean debugEnabled) {
        super(System.out, System.err, debugEnabled);
    }
}
