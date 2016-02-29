package de.fatox.meta;

import de.fatox.meta.api.Logger;

public class MetaLogger implements Logger {
    public LogLevel logLevel = LogLevel.TRACE;

    @Override
    public void trace(String tag, String text) {
        if (logLevel.ordinal() <= LogLevel.TRACE.ordinal()) {
            System.out.println("[trace] - " + tag + " : " + text);
        }
    }

    @Override
    public void info(String tag, String text) {
        if (logLevel.ordinal() <= LogLevel.INFO.ordinal()) {
            System.out.println("[info] - " + tag + " : " + text);
        }
    }

    @Override
    public void debug(String tag, String text) {
        if (logLevel.ordinal() <= LogLevel.DEBUG.ordinal()) {
            System.out.println("[debug] - " + tag + " : " + text);
        }
    }

    @Override
    public void warning(String tag, String text) {
        if (logLevel.ordinal() <= LogLevel.WARNING.ordinal()) {
            System.out.println("[warning] - " + tag + " : " + text);
        }
    }

    @Override
    public void error(String tag, String text) {
        if(logLevel.ordinal() <= LogLevel.ERROR.ordinal()) {
            System.err.println("[error] - " + tag + " : " + text);
        }
    }
}
