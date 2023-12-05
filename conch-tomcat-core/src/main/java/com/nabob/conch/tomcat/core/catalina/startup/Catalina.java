package com.nabob.conch.tomcat.core.catalina.startup;

import com.nabob.conch.tomcat.core.catalina.Server;

/**
 * Catalina（创始人）
 * <p>
 * 负责组建团队，也就是创建Server以及它的子组件
 * <p>
 * 特性：
 * - 共享类加载
 * - 持有Server CEO对象
 *
 * @author Adam
 * @since 2023/12/5
 */
public class Catalina {

    /**
     * The shared extensions class loader for this server.
     */
    protected ClassLoader parentClassLoader = Catalina.class.getClassLoader();

    /**
     * CEO
     */
    protected Server server = null;
}
