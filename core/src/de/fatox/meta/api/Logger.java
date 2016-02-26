package de.fatox.meta.api;

public interface Logger {
    enum LogLevel {
        TRACE, DEBUG, INFO, WARNING, ERROR
    }

    void trace(String tag, String text);

    void info(String tag, String text);

    void debug(String tag, String text);

    void warning(String tag, String text);

    void error(String tag, String text);
}
