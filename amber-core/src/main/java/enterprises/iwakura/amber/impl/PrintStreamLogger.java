package enterprises.iwakura.amber.impl;

import enterprises.iwakura.amber.Logger;
import lombok.RequiredArgsConstructor;

import java.io.PrintStream;

/**
 * Implementation of {@link Logger} that logs messages to specified {@link PrintStream} instances.
 */
@RequiredArgsConstructor
public class PrintStreamLogger implements Logger {

    /**
     * The stream to log informational and debug messages to.
     */
    private final PrintStream infoStream;

    /**
     * The stream to log error messages to.
     */
    private final PrintStream errorStream;

    /**
     * Whether debug logging is enabled.
     */
    private final boolean debugEnabled;

    @Override
    public void info(String message) {
        infoStream.println("[Amber] " + message);
    }

    @Override
    public void debug(String message) {
        if (debugEnabled) {
            infoStream.println("[Amber-DEBUG]  " + message);
        }
    }

    @Override
    public void error(String message, Throwable throwable) {
        errorStream.println("[Amber-ERROR] " + message);
        if (throwable != null) {
            throwable.printStackTrace(errorStream);
        }
    }
}
