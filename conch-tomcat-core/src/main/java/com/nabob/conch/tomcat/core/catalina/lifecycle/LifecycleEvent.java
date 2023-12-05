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
     * 数据
     */
    private Object data;

    /**
     * 事件类型
     */
    private String type;

    public LifecycleEvent(Lifecycle lifecycle, Object data, String type) {
        super(lifecycle);
        this.data = data;
        this.type = type;
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
