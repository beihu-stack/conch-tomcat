package com.nabob.conch.tomcat.core.catalina.lifecycle;

/**
 * 生命周期 监听器
 *
 * @author Adam
 * @since 2023/12/5
 */
public interface LifecycleListener {

    void lifecycleEvent(LifecycleEvent event);
}
