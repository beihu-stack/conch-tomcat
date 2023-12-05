package com.nabob.conch.tomcat.core.catalina.lifecycle;

/**
 * LifecycleEvent Type
 *
 * @author Adam
 * @since 2023/12/5
 */
public interface LifecycleEventType {

    /**
     * 初始化前
     */
    String BEFORE_INIT_EVENT = "before_init";
    /**
     * 初始化后
     */
    String AFTER_INIT_EVENT = "after_init";

    /**
     * 开始
     */
    String START_EVENT = "start";
    /**
     * 开始前
     */
    String BEFORE_START_EVENT = "before_start";
    /**
     * 开始后
     */
    String AFTER_START_EVENT = "after_start";

    /**
     * 停止
     */
    String STOP_EVENT = "stop";
    /**
     * 停止前
     */
    String BEFORE_STOP_EVENT = "before_stop";
    /**
     * 停止后
     */
    String AFTER_STOP_EVENT = "after_stop";

    /**
     * 销毁前
     */
    String BEFORE_DESTROY_EVENT = "before_destroy";
    /**
     * 销毁后
     */
    String AFTER_DESTROY_EVENT = "after_destroy";
}
