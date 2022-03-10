package com.qendolin.mindustryloader.gameprovider.services;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;
import java.util.logging.*;

public class LoggerFactory {
    private static final Formatter DEFAULT_FORMATTER = new SimpleFormatter() {
        private static final String format = "[%1$tT] [%2$-7s] (%3$s) %4$s%n";

        @Override
        public synchronized String format(LogRecord lr) {
            return String.format(format,
                    new Date(lr.getMillis()),
                    lr.getLevel().getName(),
                    lr.getLoggerName(),
                    lr.getMessage()
            );
        }
    };

    private static final Logger ROOT_LOGGER;

    private static Formatter formatter = DEFAULT_FORMATTER;

    static {
        Logger rootLogger = Logger.getLogger("mindustry-fabric");
        rootLogger.setUseParentHandlers(false);
        rootLogger.setLevel(Level.INFO);

        StreamHandler handler = new StreamHandler(System.out, new SimpleFormatter() {
            @Override
            public synchronized String format(LogRecord lr) {
                return formatter.format(lr);
            }
        });
        try {
            handler.setEncoding(StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ignored) {}

        rootLogger.addHandler(handler);
        ROOT_LOGGER = rootLogger;
    }

    public static Logger getRootLogger() {
        return ROOT_LOGGER;
    }

    public static Logger getLogger(String name) {
        Logger logger = Logger.getLogger(name);
        logger.setUseParentHandlers(true);
        logger.setParent(ROOT_LOGGER);
        return logger;
    }

    public static Formatter getDefaultFormatter() {
        return DEFAULT_FORMATTER;
    }

    public static Formatter getFormatter() {
        return formatter;
    }

    public static void setFormatter(Formatter formatter) {
        Objects.requireNonNull(formatter);
        LoggerFactory.formatter = formatter;
    }
}
