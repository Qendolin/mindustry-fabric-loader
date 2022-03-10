package com.qendolin.mindustryloader.gameprovider.services;

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

    private static Formatter formatter = DEFAULT_FORMATTER;

    private static final Formatter PROXY_FORMATTER = new SimpleFormatter() {
        @Override
        public synchronized String format(LogRecord lr) {
            return formatter.format(lr);
        }
    };

    public static Logger getLogger(String name) {
        Logger logger = Logger.getLogger(name);
        logger.setUseParentHandlers(false);

        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(PROXY_FORMATTER);

        logger.addHandler(handler);
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
