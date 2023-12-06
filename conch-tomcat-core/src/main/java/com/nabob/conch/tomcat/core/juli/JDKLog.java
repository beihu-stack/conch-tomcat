package com.nabob.conch.tomcat.core.juli;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDK Log Support as default
 *
 * @author Adam
 * @since 2023/12/6
 */
class JDKLog implements Log {

    public final Logger logger;

    JDKLog(String name) {
        this.logger = Logger.getLogger(name);
    }

    static Log getInstance(String name) {
        return new JDKLog(name);
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isLoggable(Level.FINER);
    }

    @Override
    public boolean isDebugEnabled() {
        // JDK Level.SEVERE 严重的
        return logger.isLoggable(Level.FINE);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isLoggable(Level.INFO);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isLoggable(Level.WARNING);
    }

    @Override
    public boolean isErrorEnabled() {
        // JDK Level.SEVERE 严重的
        return logger.isLoggable(Level.SEVERE);
    }

    @Override
    public boolean isFatalEnabled() {
        return logger.isLoggable(Level.SEVERE);
    }

    @Override
    public void trace(Object message) {
        log(Level.FINER, String.valueOf(message), null);
    }

    @Override
    public void trace(Object message, Throwable e) {
        log(Level.FINER, String.valueOf(message), e);
    }

    @Override
    public void debug(Object message) {
        log(Level.FINE, String.valueOf(message), null);
    }

    @Override
    public void debug(Object message, Throwable e) {
        log(Level.FINE, String.valueOf(message), e);
    }

    @Override
    public void info(Object message) {
        log(Level.INFO, String.valueOf(message), null);
    }

    @Override
    public void info(Object message, Throwable e) {
        log(Level.INFO, String.valueOf(message), e);
    }

    @Override
    public void warn(Object message) {
        log(Level.WARNING, String.valueOf(message), null);
    }

    @Override
    public void warn(Object message, Throwable e) {
        log(Level.WARNING, String.valueOf(message), e);
    }

    @Override
    public void error(Object message) {
        log(Level.SEVERE, String.valueOf(message), null);
    }

    @Override
    public void error(Object message, Throwable e) {
        log(Level.SEVERE, String.valueOf(message), e);
    }

    @Override
    public void fatal(Object message) {
        log(Level.SEVERE, String.valueOf(message), null);
    }

    @Override
    public void fatal(Object message, Throwable e) {
        log(Level.SEVERE, String.valueOf(message), e);
    }

    private void log(Level level, String msg, Throwable ex) {
        if (logger.isLoggable(level)) {
            // Hack (?) to get the stack trace.
            // 尝试获取堆栈跟踪信息
            Throwable dummyException=new Throwable();
            StackTraceElement locations[]=dummyException.getStackTrace();
            // Caller will be the third element
            String cname = "unknown";
            String method = "unknown";
            if (locations != null && locations.length >2) {
                StackTraceElement caller = locations[2];
                cname = caller.getClassName();
                method = caller.getMethodName();
            }
            if (ex==null) {
                logger.logp(level, cname, method, msg);
            } else {
                logger.logp(level, cname, method, msg, ex);
            }
        }
    }
}
