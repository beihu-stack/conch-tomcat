package com.nabob.conch.tomcat.core.catalina.lifecycle;

/**
 * Lifecycle Exception
 *
 * @author Adam
 * @since 2023/12/5
 */
public class LifecycleException extends Exception {

    private static final long serialVersionUID = 1L;

    public LifecycleException() {
        super();
    }

    public LifecycleException(String message) {
        super(message);
    }

    public LifecycleException(String message, Throwable cause) {
        super(message, cause);
    }

    public LifecycleException(Throwable cause) {
        super(cause);
    }

    public LifecycleException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
