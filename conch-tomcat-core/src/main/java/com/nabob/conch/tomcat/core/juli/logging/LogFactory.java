package com.nabob.conch.tomcat.core.juli.logging;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ServiceLoader;

/**
 * 日志 工厂
 * <p>
 * - 支持ServiceLoader加载
 * - 默认使用JDK Logger
 *
 * @author Adam
 * @since 2023/12/6
 */
public class LogFactory {

    private static final LogFactory singleton = new LogFactory();

    /**
     * 获取 目前日志（Log）的 构造器
     */
    private final Constructor<? extends Log> discoveredLogConstructor;

    private LogFactory() {
        // SPI
        ServiceLoader<Log> logLoader = ServiceLoader.load(Log.class);
        Constructor<? extends Log> constructor = null;
        // 识别有String类型构造器的Log
        for (Log log : logLoader) {
            Class<? extends Log> c = log.getClass();
            try {
                constructor = c.getConstructor(String.class);
                break;
            } catch (NoSuchMethodException | SecurityException e) {
                // 升级为Error异常
                throw new Error(e);
            }
        }
        discoveredLogConstructor = constructor;
    }

    public Log getInstance(String name) throws LogConfigurationException {
        if (discoveredLogConstructor == null) {
            // 默认 JDK Logger
            return new JDKLog(name);
        }

        try {
            return discoveredLogConstructor.newInstance(name);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            // ReflectiveOperationException 通用反射操作异常
            throw new LogConfigurationException(e);
        }
    }

    public Log getInstance(Class<?> clazz) throws LogConfigurationException {
        return getInstance(clazz.getName());
    }

    // static

    public static LogFactory getFactory() {
        return singleton;
    }

    public static Log getLog(Class<?> clazz) throws LogConfigurationException {
        return getFactory().getInstance(clazz);
    }

    public static Log getLog(String name) throws LogConfigurationException {
        return getFactory().getInstance(name);
    }
}
