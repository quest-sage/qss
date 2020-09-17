package com.thirds.qss.langserver;

import com.google.common.flogger.AbstractLogger;
import com.google.common.flogger.LogContext;
import com.google.common.flogger.LoggingApi;
import com.google.common.flogger.backend.LoggerBackend;
import com.google.common.flogger.backend.Platform;
import com.google.common.flogger.backend.system.SimpleLoggerBackend;
import com.google.common.flogger.parser.DefaultPrintfMessageParser;
import com.google.common.flogger.parser.MessageParser;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.*;
import java.util.stream.Stream;

/**
 * Adapted from FluentLogger.
 */
public final class QssLogger extends AbstractLogger<QssLogger.Api> {
    public static final QssLogger logger;

    // Singleton instance of the no-op API. This variable is purposefully declared as an instance of
    // the NoOp type instead of the Api type. This helps ProGuard optimization recognize the type of
    // this field more easily. This allows ProGuard to strip away low-level logs in Android apps in
    // fewer optimization passes. Do not change this to 'Api', or any less specific type.
    // VisibleForTesting
    static final NoOp NO_OP = new NoOp();

    private static final Logger l = Logger.getGlobal();

    private static Path getLogDir() {
        if (QssLanguageServer.getInstance().rootUri == null)
            return null;
        return Paths.get(QssLanguageServer.getInstance().rootUri.getPath(), ".qss", "logs");
    }

    static {
        // Copy the handlers list so we don't end up modifying the list as we're editing it.
        for (Handler handler : List.of(l.getHandlers())) {
            l.removeHandler(handler);
        }

        setLogLevel(Level.FINE);

        if (getLogDir() != null) {
            getLogDir().toFile().mkdirs();

            // Delete old log files, so that there are only a maximum of nine remaining.
            // Then, another log is generated, leaving a maximum of ten logs in the log folder at once.
            try (Stream<Path> walk = Files.walk(getLogDir())) {
                TreeSet<Path> paths = new TreeSet<>();
                walk.forEach(paths::add);
                Iterator<Path> it = paths.iterator();
                for (int i = 0; i < paths.size() - 9; i++) {
                    it.next().toFile().delete();
                }
            } catch (IOException e) {
                // can't print to stdout
            }

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String datetime = format.format(new Date(System.currentTimeMillis()));
            File logfile = getLogDir().resolve(datetime + ".log").toFile();
            try {
                logfile.createNewFile();
                FileHandler fh = new FileHandler(getLogDir().resolve(datetime + ".log").toString(), false);
                fh.setFormatter(new LogFormatter());
                l.addHandler(fh);
            } catch (IOException e) {
                // can't print to stdout
            }
        }

        logger = new QssLogger(new SimpleLoggerBackend(l));

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                logger.atSevere().withCause(e).log("Thread %s threw an unrecoverable error", t.getName());
            }
        });
    }

    // Visible for testing.
    QssLogger(LoggerBackend backend) {
        super(backend);
    }

    public static void setLogLevel(Level level) {
        l.setLevel(level);
    }

    @Override
    public Api at(Level level) {
        boolean isLoggable = isLoggable(level);
        boolean isForced = Platform.shouldForceLogging(getName(), level, isLoggable);
        return (isLoggable || isForced) ? new Context(level, isForced) : NO_OP;
    }

    /**
     * The non-wildcard, fully specified, logging API for this logger. Fluent logger implementations
     * should specify a non-wildcard API like this with which to generify the abstract logger.
     * <p>
     * It is possible to add methods to this logger-specific API directly, but it is recommended that
     * a separate top-level API and LogContext is created, allowing it to be shared by other
     * implementations.
     */
    public interface Api extends LoggingApi<Api> {
    }

    private static class LogFormatter extends Formatter {

        private final String format = "%4$s [%1$tD %1$tT @ %2$s]: %5$s%6$s%n";

        @Override
        public String format(LogRecord record) {
            ZonedDateTime zdt = ZonedDateTime.ofInstant(
                    record.getInstant(), ZoneId.systemDefault());
            String source;
            if (record.getSourceClassName() != null) {
                source = record.getSourceClassName();
                if (record.getSourceMethodName() != null) {
                    source += " " + record.getSourceMethodName();
                }
            } else {
                source = record.getLoggerName();
            }
            String message = formatMessage(record);
            String throwable = "";
            if (record.getThrown() != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.println();
                record.getThrown().printStackTrace(pw);
                pw.close();
                throwable = sw.toString();
            }
            return String.format(format,
                    zdt,
                    source,
                    record.getLoggerName(),
                    record.getLevel().getLocalizedName(),
                    message.replace("\n", "\n  "),
                    throwable.replace("\n", "\n  "));
        }
    }

    /**
     * The non-wildcard, fully specified, no-op API implementation. This is required to provide a
     * no-op implementation whose type is compatible with this logger's API.
     */
    private static final class NoOp extends LoggingApi.NoOp<Api> implements Api {
    }

    /**
     * Logging context implementing the fully specified API for this logger.
     */
    // VisibleForTesting
    final class Context extends LogContext<QssLogger, Api> implements Api {
        private Context(Level level, boolean isForced) {
            super(level, isForced);
        }

        @Override
        protected QssLogger getLogger() {
            return QssLogger.this;
        }

        @Override
        protected Api api() {
            return this;
        }

        @Override
        protected Api noOp() {
            return NO_OP;
        }

        @Override
        protected MessageParser getMessageParser() {
            return DefaultPrintfMessageParser.getInstance();
        }
    }
}
