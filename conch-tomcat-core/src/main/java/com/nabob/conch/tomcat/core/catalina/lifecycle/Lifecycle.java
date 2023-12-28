package com.nabob.conch.tomcat.core.catalina.lifecycle;

/**
 * 生命周期
 *
 * @author Adam
 * @since 2023/12/5
 */
public interface Lifecycle extends LifecycleEventType {

    // Lifecycle

    /**
     * 初始化
     */
    void init() throws LifecycleException;

    /**
     * 开始
     */
    void start() throws LifecycleException;

    /**
     * 停止
     */
    void stop() throws LifecycleException;

    /**
     * 销毁
     */
    void destroy() throws LifecycleException;

    // LifecycleListener

    /**
     * 添加 生命周期监听器
     */
    void addLifecycleListener(LifecycleListener listener);

    /**
     * 查询 生命周期监听器
     */
    LifecycleListener[] findLifecycleListeners();

    /**
     * 移除 生命周期监听器
     */
    void removeLifecycleListener(LifecycleListener listener);

    // LifecycleState

    /**
     * 获取状态
     */
    LifecycleState getState();
}
