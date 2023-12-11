package com.nabob.conch.tomcat.core.juli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * <h1>LogManager对象上的所有方法都是多线程安全的</h1>
 *
 * <h3>ClassLoader LogManager</h3>
 * - 根据不同的ClassLoader，加载对应的日志信息
 * - -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager
 * <p>
 * 架构：
 * ClassLoader -> ClassLoaderLogInfo
 *
 * <pre>
 * ClassLoaderLogInfo ->
 *  -> 管理JDK的Logger 使用 LogNode rootNode
 *      -> 因为日志是可以依据包路径进行设置的，所以这里会解析包路径，组装成一个LogNode
 *      -> LogNode: （使用一个Map加一个链表管理）
 *          -> Map用于当前LogNode所持有的所有同层子LogNode（只有一层）
 *          -> parent节点，链接存在父依赖关系的LogNode
 *              ->  RootNode:
 *                      parent: null
 *                      children: Map<String, LogNode>  | 存储 Child1Node
 *                      logger: JDK Logger
 *                  Child1Node:
 *                      parent: RootNode
 *                      children: Map<String, LogNode>  | 存储 Child2Node、Child22Node
 *                      logger: null
 *                  Child2Node:
 *                      parent: Child1Node
 *                      children: Map<String, LogNode>
 *                      logger: null
 *                  Child22Node:
 *                      parent: Child1Node
 *                      children: Map<String, LogNode>
 *                      logger: null
 *
 * </pre>
 *
 * <h3>JUL LogManager:</h3>
 * <pre>
 * 有一个全局LogManager对象，用于维护有关Loggers和日志服务的一组共享状态。
 * 这个LogManager对象：
 *      - 管理Logger对象的分层命名空间。 所有命名的记录器都存储在此命名空间中。
 *      - 管理一组日志记录控制属性。 这些是简单的键值对，处理程序和其他日志记录对象可以使用它们进行自我配置。
 *      - 可以使用LogManager.getLogManager（）检索全局LogManager对象。 LogManager对象是在类初始化期间创建的，随后无法更改。
 * 在启动时，使用java.util.logging.manager系统属性找到LogManager类。
 * LogManager配置
 * 在LogManager初始化期间，LogManager通过readConfiguration()方法初始化日志记录配置。 默认情况下，使用LogManager默认配置。 LogManager读取的日志记录配置必须采用properties file格式。
 * LogManager定义了两个可选的系统属性，允许控制初始配置，如readConfiguration()方法中所指定：
 *      “java.util.logging.config.class”
 *      “java.util.logging.config.file”
 * 可以在命令行上将这两个系统属性指定为“java”命令，或者作为传递给JNI_CreateJavaVM的系统属性定义。
 * 记录器和处理程序的properties将具有以处理程序或记录器的点分隔名称开头的名称。
 * 全局日志记录属性可能包括：
 *      属性“处理程序”。 这为处理程序类定义了一个以空格或逗号分隔的类名列表，以便在根Logger（名为“”的Logger）上加载和注册为处理程序。 每个类名必须是具有默认构造函数的Handler类。 请注意，这些处理程序可能会在首次使用时延迟创建。
 *      属性“<logger> .handlers”。 这为处理程序类定义了一个以空格或逗号分隔的类名列表，以便加载和注册为指定记录程序的处理程序。 每个类名必须是具有默认构造函数的Handler类。 请注意，这些处理程序可能会在首次使用时延迟创建。
 *      属性“<logger> .handlers.ensureCloseOnReset”。 这定义了一个布尔值。 如果未定义“<logger> .handlers”或为空，则忽略此属性。 否则默认为true 。 当值为true ，保证在reset()关闭与记录器关联的处理程序并关闭。 可以通过在配置中显式设置“<logger> .handlers.ensureCloseOnReset = false”来关闭此功能。 请注意，关闭此属性会导致引入资源泄漏的风险，因为在调用reset()之前，记录器可能会收集垃圾，从而阻止其处理程序在reset()上关闭。 在这种情况下，应用程序有责任确保在记录器被垃圾收集之前关闭处理程序。
 *      属性“<logger> .useParentHandlers”。 这定义了一个布尔值。 默认情况下，除了处理日志消息本身之外，每个记录器都会调用其父记录，这通常也会导致消息由根记录器处理。 将此属性设置为false时，需要为此记录器配置Handler，否则不会传递任何记录消息。
 *      属性“config”。 此属性旨在允许运行任意配置代码。 该属性定义了一个以空格或逗号分隔的类名列表。 将为每个命名类创建一个新实例。 每个类的默认构造函数可以执行任意代码来更新日志记录配置，例如设置记录器级别，添加处理程序，添加过滤器等。
 * 请注意，在任何用户类路径之前，首先在系统类路径上搜索在LogManager配置期间加载的所有类。 这包括LogManager类，任何配置类和任何处理程序类。
 * 记录器根据点分隔名称组织成命名层次结构。 因此“abc”是“ab”的孩子，但“a.b1”和a.b2“是同龄人。
 * 名称以“.level”结尾的所有属性都假定为Loggers定义日志级别。 因此，“foo.level”为命名层次结构中的任何子项定义了一个名为“foo”和（递归）的记录器的日志级别。 日志级别按属性文件中定义的顺序应用。 因此，树中子节点的级别设置应该在其父级的设置之后进行。 属性名称“.level”可用于设置树根的级别。
 * LogManager对象上的所有方法都是多线程安全的。
 * </pre>
 *
 * @author Adam
 * @since 2023/12/7
 */
public class ClassLoaderLogManager extends LogManager {

    /**
     * 根据不同的ClassLoader，加载对应的日志信息
     * 使用 WeakHashMap 避免应用重新部署导致的 ClassLoader引用泄露
     */
    protected final Map<ClassLoader, ClassLoaderLogInfo> classLoaderLoggers = new WeakHashMap<>();

    /**
     * 覆盖 addLogger 方法
     * <p>
     * synchronized 线程安全的，必须
     * 原LogManager的addLogger内部也使用了synchronized以保障LogManager的线程安全
     */
    @Override
    public synchronized boolean addLogger(Logger logger) {
        // todo impl
        return false;
    }

    // ------------------------------------------------------ Protected Methods


    /**
     * 读取 日志配置 for the specified classloader
     */
    protected synchronized void readConfiguration(ClassLoader classLoader) {

        // 获取配置文件资源
        InputStream is = null;

        try {
            if (classLoader instanceof URLClassLoader) {
                URL logConfig = ((URLClassLoader) classLoader).findResource("logging.properties");
                if (null != logConfig) {
                    is = classLoader.getResourceAsStream("logging.properties");
                }
            }
        } catch (Exception e) {
            // can log, if have Logger
        }

        // 未获取配置文件资源，尝试从 java.util.logging.config.file 获取
        if (is == null && classLoader == ClassLoader.getSystemClassLoader()) {
            String configFileStr = System.getProperty("java.util.logging.config.file");
            if (configFileStr != null) {
                try {
                    is = new FileInputStream(configFileStr);
                } catch (FileNotFoundException e) {
                    System.err.println("Configuration error");
                    e.printStackTrace();
                }
            }

            // 尝试使用JVM默认配置
            if (is == null) {
                File defaultFile = new File(new File(System.getProperty("java.home"), "conf"), "logging.properties");
                try {
                    is = new FileInputStream(defaultFile);
                } catch (IOException e) {
                    System.err.println("Configuration error");
                    e.printStackTrace();
                }
            }
        }
    }


    // ---------------------------------------------------- LogNode Inner Class

    /**
     * 管理 Logger，并提供父子链支持
     */
    protected static final class LogNode {

        // 该Node所管理的Logger
        Logger logger;
        // 管理该Node的所有子Node
        final Map<String, LogNode> children = new HashMap<>();
        // 指向该Node的父Node
        LogNode parent;

        public LogNode(LogNode parent, Logger logger) {
            this.parent = parent;
            this.logger = logger;
        }

        public LogNode(LogNode parent) {
            this(parent, null);
        }

        /**
         * 获取Node，没有则创建
         */
        LogNode findNode(String name) {
            if (logger.getName().equals(name)) {
                return this;
            }

            LogNode currentNode = this;

            while (name != null) {
                // 获取第一个 . 的位置
                final int dotIndex = name.indexOf('.');
                // 下一个 log name
                final String nextName;

                if (dotIndex < 0) {
                    // 没有 . 的话
                    nextName = name;
                    name = null;
                } else {
                    nextName = name.substring(0, dotIndex);
                    name = name.substring(dotIndex + 1);
                }

                LogNode childNode = children.get(nextName);
                if (childNode == null) {
                    childNode = new LogNode(currentNode);
                    // 放到当前Node的Map中
                    currentNode.children.put(nextName, childNode);
                }

                // 将子Node赋值给当前Node，那么可以发现children map中都是存放与当前Node只有一层的子Node
                currentNode = childNode;
            }

            return currentNode;
        }
    }

    // -------------------------------------------- ClassLoaderInfo Inner Class

    protected static final class ClassLoaderLogInfo {

        final LogNode rootNode;

        final Map<String, Logger> loggers = new ConcurrentHashMap<>();

        final Map<String, Handler> handlers = new HashMap<>();

        ClassLoaderLogInfo(LogNode rootNode) {
            this.rootNode = rootNode;
        }
    }

}
