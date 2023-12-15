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
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * <h1>LogManager对象上的所有方法都是多线程安全的</h1>
 *
 * <h3>ClassLoader LogManager</h3>
 * - 根据不同的ClassLoader，加载对应的日志信息
 * - -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager
 * <p>
 * Why：
 * - Apache Tomcat 提供了一个可识别类加载器的自定义 LogManager。这样做是为了避免日志混淆的问题，即多个 Web 应用程序（每个应用程序都有自己的类加载器）创建具有相同名称的记录器。
 * - 自定义 LogManager 还允许创建同一 Handler 类的多个实例。它通过在处理程序类名中添加数字前缀来实现此目的。因此，它以不同的方式对待“handlers”和“.handlers”属性。第一个定义所有处理程序，第二个定义根记录器的处理程序。
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
 *      属性“handlers” 这为处理程序类定义了一个以空格或逗号分隔的类名列表，以便在根Logger（名为“”的Logger）上加载和注册为处理程序。 每个类名必须是具有默认构造函数的Handler类。 请注意，这些处理程序可能会在首次使用时延迟创建。
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

    private static ThreadLocal<Boolean> addingLocalRootLogger = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * 根据不同的ClassLoader，加载对应的日志信息
     * 使用 WeakHashMap 避免应用重新部署导致的 ClassLoader引用泄露
     */
    protected final Map<ClassLoader, ClassLoaderLogInfo> classLoaderLoggers = new WeakHashMap<>();

    /**
     * 覆盖 addLogger 方法
     * <p>
     * 代码里面 Logger.getLogger(name) 时，会调用LogManager.getLogger(name), 如果没有该name的Logger对象，创建Logger.并会调用 LogManager.addLogger(Logger)
     * {@link Logger#getLogger(String)}
     * {@link LogManager#addLogger(java.util.logging.Logger)} ClassLoaderLogManager重新该方法
     *
     * <p>
     * synchronized 线程安全的，必须
     * 原LogManager的addLogger内部也使用了synchronized以保障LogManager的线程安全
     */
    @Override
    public synchronized boolean addLogger(final Logger logger) {
        final String loggerName = logger.getName();
        ClassLoader classLoader = getClassLoader();
        ClassLoaderLogInfo classLoaderInfo = getClassLoaderInfo(classLoader);

        if (classLoaderInfo.loggers.containsKey(loggerName)) {
            return false;
        }
        classLoaderInfo.loggers.put(loggerName, logger);

        // 为logger配置日志级别；rootLogger的loggerName是 ""， 所以配置文件中的 .level 将是rootLogger的配置级别
        final String levelString = getProperty(loggerName + ".level");
        if (levelString != null) {
            try {
                logger.setLevel(Level.parse(levelString.trim()));
            } catch (IllegalArgumentException e) {
                // Leave level set to null
            }
        }

        // 初始化 父Logger
        int dotIndex = loggerName.lastIndexOf('.');
        if (dotIndex >= 0) {
            final String parentName = loggerName.substring(0, dotIndex);
            Logger.getLogger(parentName);
        }

        // 查询or创建LogNode
        LogNode node = classLoaderInfo.rootNode.findNode(loggerName);
        node.logger = logger;

        // Set parent logger
        Logger parentLogger = node.findParentLogger();
        if (parentLogger != null) {
            doSetParentLogger(logger, parentLogger);
        }

        // Tell children we are their new parent
        node.setParentLogger(logger);

        // Add associated handlers, if any are defined using the .handlers property.
        // In this case, handlers of the parent logger(s) will not be used
        String handlers = getProperty(loggerName + ".handlers");
        if (handlers != null) {
            logger.setUseParentHandlers(false);
            StringTokenizer tok = new StringTokenizer(handlers, ",");
            while (tok.hasMoreTokens()) {
                String handlerName = (tok.nextToken().trim());
                Handler handler = null;
                ClassLoader current = classLoader;
                while (current != null) {
                    classLoaderInfo = classLoaderLoggers.get(current);
                    if (classLoaderInfo != null) {
                        handler = classLoaderInfo.handlers.get(handlerName);
                        if (handler != null) {
                            break;
                        }
                    }
                    current = current.getParent();
                }
                if (handler != null) {
                    logger.addHandler(handler);
                }
            }
        }

        // Parse useParentHandlers to set if the logger should delegate to its parent.
        // Unlike java.util.logging, the default is to not delegate if a list of handlers
        // has been specified for the logger.
        String useParentHandlersString = getProperty(loggerName + ".useParentHandlers");
        if (Boolean.parseBoolean(useParentHandlersString)) {
            logger.setUseParentHandlers(true);
        }

        return true;
    }

    /**
     * 覆盖 获取日志 配置方法
     */
    @Override
    public void readConfiguration() throws IOException, SecurityException {
        readConfiguration(getClassLoader());
    }

    /**
     * 覆盖 获取日志 配置方法
     * <p>
     * 从文件获取配置时，需重置当前Logger的配置
     */
    @Override
    public void readConfiguration(InputStream ins) throws IOException, SecurityException {
        readConfiguration(ins, getClassLoader());
    }

    @Override
    public String getProperty(String name) {

        // Use a ThreadLocal to work around
        // https://bugs.openjdk.java.net/browse/JDK-8195096
        if (".handlers".equals(name) && !addingLocalRootLogger.get().booleanValue()) {
            return null;
        }

//        String prefix = this.prefix.get();
        String result = null;

        // If a prefix is defined look for a prefixed property first
//        if (prefix != null) {
//            result = findProperty(prefix + name);
//        }

        // If there is no prefix or no property match with the prefix try just
        // the name
        if (result == null) {
            result = findProperty(name);
        }

        // Simple property replacement (mostly for folder names)
//        if (result != null) {
//            result = replace(result);
//        }
        return result;
    }


    private synchronized String findProperty(String name) {
        ClassLoader classLoader = getClassLoader();
        ClassLoaderLogInfo info = getClassLoaderInfo(classLoader);
        String result = info.props.getProperty(name);
        // If the property was not found, and the current classloader had no
        // configuration (property list is empty), look for the parent classloader
        // properties.
        if ((result == null) && (info.props.isEmpty())) {
            if (classLoader != null) {
                ClassLoader current = classLoader.getParent();
                while (current != null) {
                    info = classLoaderLoggers.get(current);
                    if (info != null) {
                        result = info.props.getProperty(name);
                        if ((result != null) || (!info.props.isEmpty())) {
                            break;
                        }
                    }
                    current = current.getParent();
                }
            }
            if (result == null) {
                result = super.getProperty(name);
            }
        }
        return result;
    }


    @Override
    public synchronized void reset() throws SecurityException {
        // todo impl
        super.reset();
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * 根据ClassLoader 获取 日志信息
     */
    protected synchronized ClassLoaderLogInfo getClassLoaderInfo(ClassLoader classLoader) {

        if (classLoader == null) {
            classLoader = this.getClass().getClassLoader();
        }

        ClassLoaderLogInfo classLoaderLogInfo = classLoaderLoggers.get(classLoader);
        if (classLoaderLogInfo == null) {
            try {
                readConfiguration(classLoader);
            } catch (IOException e) {
                // do nothing
            }
            classLoaderLogInfo = classLoaderLoggers.get(classLoader);
        }

        return classLoaderLogInfo;
    }

    /**
     * 读取 日志配置 for the specified classloader
     */
    protected synchronized void readConfiguration(ClassLoader classLoader) throws IOException {

        // 获取配置文件资源
        InputStream is = null;

        try {

            // todo WebappProperties，在web应用程序中，项目将会使用WEB-INF/classes/logging.properties日志配置文件

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
                // jdk11后放在conf目录下
                File defaultFile = new File(new File(System.getProperty("java.home"), "conf"), "logging.properties");
                try {
                    is = new FileInputStream(defaultFile);
                } catch (IOException e) {
                    System.err.println("Configuration error");
                    e.printStackTrace();
                }
            }
        }

        // rootLogger
        Logger rootLogger = new RootLogger();
        if (is == null) {
            // 检索 父classLoader的 root logger
            ClassLoader current = classLoader.getParent();
            ClassLoaderLogInfo info = null;
            while (current != null && info == null) {
                info = getClassLoaderInfo(current);
                current = current.getParent();
            }
            if (info != null) {
                // 作为 rootLogger 的 parent
                rootLogger.setParent(info.rootNode.logger);
            }
        }

        // 创建 ClassLoaderLogInfo， 用于维护日志信息
        ClassLoaderLogInfo classLoaderLogInfo = new ClassLoaderLogInfo(new LogNode(null, rootLogger));
        classLoaderLoggers.put(classLoader, classLoaderLogInfo);

        // 读取配置文件，加载到 ClassLoaderLogInfo 中
        if (is != null) {
            readConfiguration(is, classLoader);
        }

        try {
            // Use a ThreadLocal to work around
            // https://bugs.openjdk.java.net/browse/JDK-8195096
            addingLocalRootLogger.set(Boolean.TRUE);
            addLogger(rootLogger);
        } finally {
            addingLocalRootLogger.set(Boolean.FALSE);
        }
    }

    /**
     * 读取 日志配置 for the specified classloader with InputStream resource
     */
    protected synchronized void readConfiguration(InputStream is, ClassLoader classLoader) throws IOException {
        ClassLoaderLogInfo classLoaderLogInfo = classLoaderLoggers.get(classLoader);

        // 加载配置
        try {
            classLoaderLogInfo.props.load(is);
        } catch (IOException e) {
            System.err.println("Configuration error");
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException ioe) {
                // do nothing
            }
        }

        Logger rootLogger = classLoaderLogInfo.rootNode.logger;

        // 未这个classLoader的 root logger 创建 handlers
        String rootHandlers = classLoaderLogInfo.props.getProperty(".handlers");
        String handlers = classLoaderLogInfo.props.getProperty("handlers");

        if (handlers != null) {
            StringTokenizer handlersTok = new StringTokenizer(handlers, ",");
            while (handlersTok.hasMoreTokens()) {
                String handlerName = handlersTok.nextToken().trim();

                // 解析真正的Handler Class Name
                String handlerClassName = handlerName;
//                String prefix = "";
                if (handlerClassName.length() <= 0) {
                    continue;
                }
                if (Character.isDigit(handlerClassName.charAt(0))) {
                    int pos = handlerClassName.indexOf('.');
                    if (pos >= 0) {
//                        prefix = handlerClassName.substring(0, pos + 1);
                        handlerClassName = handlerClassName.substring(pos + 1);
                    }
                }

                // 加载Handler Class
                try {
                    Handler handler = (Handler) classLoader.loadClass(handlerClassName).getConstructor().newInstance();
                    // key : handlerName
                    classLoaderLogInfo.handlers.put(handlerName, handler);

                    // 兼容
                    if (rootHandlers == null) {
                        rootLogger.addHandler(handler);
                    }

                } catch (Exception e) {
                    System.err.println("Handler error");
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

        Logger findParentLogger() {
            Logger logger = null;
            LogNode node = parent;
            while (node != null && logger == null) {
                logger = node.logger;
                node = node.parent;
            }
            return logger;
        }

        void setParentLogger(final Logger parent) {
            for (final LogNode childNode : children.values()) {
                if (childNode.logger == null) {
                    childNode.setParentLogger(parent);
                } else {
                    doSetParentLogger(childNode.logger, parent);
                }
            }
        }
    }

    protected static void doSetParentLogger(final Logger logger, final Logger parent) {
        logger.setParent(parent);
    }

    // -------------------------------------------- ClassLoaderInfo Inner Class

    /**
     * 管理 LogInfo，每个ClassLoader对应一个ClassLoaderLogInfo
     * <p>
     * 这样做是为了避免日志混淆的问题，即多个 Web 应用程序（每个应用程序都有自己的类加载器）创建具有相同名称的记录器。
     */
    protected static final class ClassLoaderLogInfo {

        final LogNode rootNode;

        // 重写LogManager的addLogger方法后，统一管理在这个Map中
        final Map<String, Logger> loggers = new ConcurrentHashMap<>();

        // 配置文件中的 Handler，使用: handlers = 1catalina.org.apache.juli.AsyncFileHandler, 2localhost.org.apache.juli.AsyncFileHandler  配置
        final Map<String, Handler> handlers = new HashMap<>();

        // 用于加载 Properties 资源： 日志配置文件；每个 ClassLoaderLogInfo 对象，可加载自己的配置
        final Properties props = new Properties();

        ClassLoaderLogInfo(LogNode rootNode) {
            this.rootNode = rootNode;
        }
    }

    /**
     * 获取 ClassLoader，优先使用线程上下文
     */
    static ClassLoader getClassLoader() {
        ClassLoader result = Thread.currentThread().getContextClassLoader();
        if (result == null) {
            result = ClassLoaderLogManager.class.getClassLoader();
        }
        return result;
    }

    /**
     * 用于初始化每个ClassLoader层次机构的根
     */
    protected static class RootLogger extends Logger {
        public RootLogger() {
            super("", null);
        }
    }
}
