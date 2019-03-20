package io.auklet.misc;

import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * <p>Adapts the Paho logging framework to use SLF4J. To use this, invoke the following
 * prior to starting any MQTT client:
 * {@code org.eclipse.paho.client.mqttv3.logging.LoggerFactory.setLogger("io.auklet.misc.PahoLogger");}</p>
 *
 * <p>When enabled, all Paho MQTT loggers will retain their original class-based names and will be configured
 * via SLF4J. JUL logging levels are mapped 1-to-1 to SLF4J logging levels, except that `FINE`, `FINER` and `FINEST`
 * all map to `TRACE`.</p>
 *
 * <p>These loggers are designed to work with JUL resource bundles, but do not require them.</p>
 */
@NotThreadSafe
public final class PahoLogger implements org.eclipse.paho.client.mqttv3.logging.Logger {

    private Logger logger = null;
    private ResourceBundle bundle;
    private String context;

    @Override public void initialise(ResourceBundle messageCatalog, String loggerID, String resourceName) {
        if (loggerID == null) throw new IllegalArgumentException("loggerID is null.");
        logger = LoggerFactory.getLogger(loggerID);
        bundle = messageCatalog;
        context = resourceName;
    }

    @Override
    public void setResourceName(String logContext) { context = logContext; }

    @Override public boolean isLoggable(int level) {
        switch (level) {
            case SEVERE:
                return logger.isErrorEnabled();
            case WARNING:
                return logger.isWarnEnabled();
            case INFO:
                return logger.isInfoEnabled();
            case CONFIG:
                return logger.isDebugEnabled();
            case FINE:
            case FINER:
            case FINEST:
                return logger.isTraceEnabled();
            default:
                // Impossible, so assert it is not loggable.
                return false;
        }
    }

    @Override public void severe(String sourceClass, String sourceMethod, String msg) {
        log(SEVERE, sourceClass, sourceMethod, msg, null, null);
    }

    @Override public void severe(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
        log(SEVERE, sourceClass, sourceMethod, msg, inserts, null);
    }

    @Override public void severe(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown) {
        log(SEVERE, sourceClass, sourceMethod, msg, inserts, thrown);
    }

    @Override public void warning(String sourceClass, String sourceMethod, String msg) {
        log(WARNING, sourceClass, sourceMethod, msg, null, null);
    }

    @Override public void warning(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
        log(WARNING, sourceClass, sourceMethod, msg, inserts, null);
    }

    @Override public void warning(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown) {
        log(WARNING, sourceClass, sourceMethod, msg, inserts, thrown);
    }

    @Override public void info(String sourceClass, String sourceMethod, String msg) {
        log(INFO, sourceClass, sourceMethod, msg, null, null);
    }

    @Override public void info(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
        log(INFO, sourceClass, sourceMethod, msg, inserts, null);
    }

    @Override public void info(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown) {
        log(INFO, sourceClass, sourceMethod, msg, inserts, thrown);
    }

    @Override public void config(String sourceClass, String sourceMethod, String msg) {
        log(CONFIG, sourceClass, sourceMethod, msg, null, null);
    }

    @Override public void config(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
        log(CONFIG, sourceClass, sourceMethod, msg, inserts, null);
    }

    @Override public void config(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown) {
        log(CONFIG, sourceClass, sourceMethod, msg, inserts, thrown);
    }

    @Override public void fine(String sourceClass, String sourceMethod, String msg) {
        trace(FINE, sourceClass, sourceMethod, msg, null, null);
    }

    @Override public void fine(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
        trace(FINE, sourceClass, sourceMethod, msg, inserts, null);
    }

    @Override public void fine(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable ex) {
        trace(FINE, sourceClass, sourceMethod, msg, inserts, ex);
    }

    @Override public void finer(String sourceClass, String sourceMethod, String msg) {
        trace(FINER, sourceClass, sourceMethod, msg, null, null);
    }

    @Override public void finer(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
        trace(FINER, sourceClass, sourceMethod, msg, inserts, null);
    }

    @Override public void finer(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable ex) {
        trace(FINER, sourceClass, sourceMethod, msg, inserts, ex);
    }

    @Override public void finest(String sourceClass, String sourceMethod, String msg) {
        trace(FINEST, sourceClass, sourceMethod, msg, null, null);
    }

    @Override public void finest(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
        trace(FINEST, sourceClass, sourceMethod, msg, inserts, null);
    }

    @Override public void finest(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable ex) {
        trace(FINEST, sourceClass, sourceMethod, msg, inserts, ex);
    }

    @Override
    public void log(int level, String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown) {
        logToSlf4j(level, sourceClass, sourceMethod, msg, inserts, thrown);
    }

    @Override
    public void trace(int level, String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable ex) {
        logToSlf4j(level, sourceClass, sourceMethod, msg, inserts, ex);
    }

    private void logToSlf4j(int level, String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable throwable) {
        if (!isLoggable(level)) return;
        StringBuilder message = new StringBuilder();
        if (!Util.isNullOrEmpty(sourceClass)) {
            message.append('[').append(sourceClass);
            if (!Util.isNullOrEmpty(sourceMethod)) message.append('#').append(sourceMethod);
            message.append("] ");
        }
        if (!Util.isNullOrEmpty(context)) message.append(context).append(": ");
        if (bundle != null) {
            try {
                message.append(bundle.getString(msg));
            } catch (MissingResourceException e) {
                message.append(msg);
            }
        } else message.append(msg);
        ArrayList<Object> insertList = inserts == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(inserts));
        if (throwable != null) insertList.add(throwable);
        String finalMessage = message.toString();
        Object[] finalInserts = insertList.toArray();
        switch (level) {
            case SEVERE:
                logger.error(finalMessage, finalInserts);
            case WARNING:
                logger.warn(finalMessage, finalInserts);
            case INFO:
                logger.info(finalMessage, finalInserts);
            case CONFIG:
                logger.debug(finalMessage, finalInserts);
            case FINE:
            case FINER:
            case FINEST:
                logger.trace(finalMessage, finalInserts);
            default:
                // Impossible, so do nothing.
        }
    }

    /**
     * <p>This method is unused in the built-in JSR47 implementation, and thus is unsed here as well;
     * it is overridden here for completeness.</p>
     *
     * @param msg unused.
     * @param inserts unused.
     * @return {@code null}.
     */
    @Override public String formatMessage(String msg, Object[] inserts) { return null; }

    /** <p>This method is specific to JUL logging, and thus does nothing</p> */
    @Override public void dumpTrace() {}

}
