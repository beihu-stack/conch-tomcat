package com.nabob.conch.tomcat.core.juli;

/**
 * 日志API
 * <p>
 * 日志级别：
 * - trace (the least serious)
 * - debug
 * - info
 * - warn
 * - error
 * - fatal (the most serious)
 *
 * @author Adam
 * @since 2023/12/6
 */
public interface Log {

    /**
     * 是否支持 debug 级别日志
     */
    boolean isDebugEnabled();

    /**
     * 是否支持 error 级别日志
     */
    boolean isErrorEnabled();

    /**
     * 是否支持 fatal 级别日志
     */
    boolean isFatalEnabled();

    /**
     * 是否支持 info 级别日志
     */
    boolean isInfoEnabled();

    /**
     * 是否支持 trace 级别日志
     */
    boolean isTraceEnabled();

    /**
     * 是否支持 warn 级别日志
     */
    boolean isWarnEnabled();

    void trace(Object message);
    void trace(Object message, Throwable e);

    void debug(Object message);
    void debug(Object message, Throwable e);

    void info(Object message);
    void info(Object message, Throwable e);

    void warn(Object message);
    void warn(Object message, Throwable e);

    void error(Object message);
    void error(Object message, Throwable e);

    void fatal(Object message);
    void fatal(Object message, Throwable e);
}
