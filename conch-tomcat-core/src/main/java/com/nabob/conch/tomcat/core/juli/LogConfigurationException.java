package com.nabob.conch.tomcat.core.juli;

/**
 * Log Configuration Exception
 *
 * @author Adam
 * @since 2023/12/7
 */
public class LogConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LogConfigurationException() {
        super();
    }

    public LogConfigurationException(String message) {
        super(message);
    }

    public LogConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public LogConfigurationException(Throwable cause) {
        super(cause);
    }

    public LogConfigurationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
