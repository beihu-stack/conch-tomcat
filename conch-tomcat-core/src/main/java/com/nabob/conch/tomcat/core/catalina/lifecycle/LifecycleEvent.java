package com.nabob.conch.tomcat.core.catalina.lifecycle;

import java.util.EventObject;

/**
 * 生命周期事件
 *
 * @author Adam
 * @since 2023/12/5
 */
public final class LifecycleEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    /**
     * 事件类型
     *
     * @see LifecycleEventType
     */
    private String type;

    /**
     * 数据
     */
    private Object data;

    public LifecycleEvent(Lifecycle lifecycle, String type, Object data) {
        super(lifecycle);
        this.type = type;
        this.data = data;
    }

    public Object getData() {
        return data;
    }

    public String getType() {
        return type;
    }

    public Lifecycle getLifecycle() {
        return (Lifecycle) getSource();
    }
}
