package com.nabob.conch.tomcat.core.catalina.lifecycle;

/**
 * 生命周期
 *
 * @author Adam
 * @since 2023/12/5
 */
public interface Lifecycle {

    /**
     * 初始化
     */
    void init();

    /**
     * 开始
     */
    void start();

    /**
     * 停止
     */
    void stop();

    /**
     * 销毁
     */
    void destroy();

}
