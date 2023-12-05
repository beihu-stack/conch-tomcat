package com.nabob.conch.tomcat.core.catalina.lifecycle;

/**
 * 生命周期状态 枚举
 *
 * @author Adam
 * @since 2023/3/10
 */
public enum LifecycleState {

    NEW(false, null),
    INITIALIZING(false, LifecycleEventType.BEFORE_INIT_EVENT),
    INITIALIZED(false, LifecycleEventType.AFTER_INIT_EVENT),
    STARTING_PREP(false, LifecycleEventType.BEFORE_START_EVENT),
    STARTING(true, LifecycleEventType.START_EVENT),
    STARTED(true, LifecycleEventType.AFTER_START_EVENT),
    STOPPING_PREP(true, LifecycleEventType.BEFORE_STOP_EVENT),
    STOPPING(false, LifecycleEventType.STOP_EVENT),
    STOPPED(false, LifecycleEventType.AFTER_STOP_EVENT),
    DESTROYING(false, LifecycleEventType.BEFORE_DESTROY_EVENT),
    DESTROYED(false, LifecycleEventType.AFTER_DESTROY_EVENT),
    FAILED(false, null);

    /**
     * 是否可用
     */
    private final boolean available;
    /**
     * 事件类型
     */
    private final String lifecycleEvent;

    private LifecycleState(boolean available, String lifecycleEvent) {
        this.available = available;
        this.lifecycleEvent = lifecycleEvent;
    }

    /**
     * May the public methods other than property getters/setters and lifecycle
     * methods be called for a component in this state? It returns
     * <code>true</code> for any component in any of the following states:
     * <ul>
     * <li>{@link #STARTING}</li>
     * <li>{@link #STARTED}</li>
     * <li>{@link #STOPPING_PREP}</li>
     * </ul>
     *
     * @return <code>true</code> if the component is available for use,
     * otherwise <code>false</code>
     */
    public boolean isAvailable() {
        return available;
    }

    public String getLifecycleEvent() {
        return lifecycleEvent;
    }
}
