package com.nabob.conch.tomcat.core.juli;

import java.util.logging.LogManager;

/**
 * ClassLoader LogManager
 * - 根据不同的ClassLoader，加载对应的日志信息
 * - -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager
 *
 * <p>
 * JUL LogManager:
 * <p>
 * 有一个全局LogManager对象，用于维护有关Loggers和日志服务的一组共享状态。
 * 这个LogManager对象：
 * <p>
 * 管理Logger对象的分层命名空间。 所有命名的记录器都存储在此命名空间中。
 * 管理一组日志记录控制属性。 这些是简单的键值对，处理程序和其他日志记录对象可以使用它们进行自我配置。
 * 可以使用LogManager.getLogManager（）检索全局LogManager对象。 LogManager对象是在类初始化期间创建的，随后无法更改。
 * <p>
 * 在启动时，使用java.util.logging.manager系统属性找到LogManager类。
 * <p>
 * LogManager配置
 * 在LogManager初始化期间，LogManager通过readConfiguration()方法初始化日志记录配置。 默认情况下，使用LogManager默认配置。 LogManager读取的日志记录配置必须采用properties file格式。
 * LogManager定义了两个可选的系统属性，允许控制初始配置，如readConfiguration()方法中所指定：
 * <p>
 * “java.util.logging.config.class”
 * “java.util.logging.config.file”
 * 可以在命令行上将这两个系统属性指定为“java”命令，或者作为传递给JNI_CreateJavaVM的系统属性定义。
 * <p>
 * 记录器和处理程序的properties将具有以处理程序或记录器的点分隔名称开头的名称。
 * 全局日志记录属性可能包括：
 * <p>
 * 属性“处理程序”。 这为处理程序类定义了一个以空格或逗号分隔的类名列表，以便在根Logger（名为“”的Logger）上加载和注册为处理程序。 每个类名必须是具有默认构造函数的Handler类。 请注意，这些处理程序可能会在首次使用时延迟创建。
 * 属性“<logger> .handlers”。 这为处理程序类定义了一个以空格或逗号分隔的类名列表，以便加载和注册为指定记录程序的处理程序。 每个类名必须是具有默认构造函数的Handler类。 请注意，这些处理程序可能会在首次使用时延迟创建。
 * 属性“<logger> .handlers.ensureCloseOnReset”。 这定义了一个布尔值。 如果未定义“<logger> .handlers”或为空，则忽略此属性。 否则默认为true 。 当值为true ，保证在reset()关闭与记录器关联的处理程序并关闭。 可以通过在配置中显式设置“<logger> .handlers.ensureCloseOnReset = false”来关闭此功能。 请注意，关闭此属性会导致引入资源泄漏的风险，因为在调用reset()之前，记录器可能会收集垃圾，从而阻止其处理程序在reset()上关闭。 在这种情况下，应用程序有责任确保在记录器被垃圾收集之前关闭处理程序。
 * 属性“<logger> .useParentHandlers”。 这定义了一个布尔值。 默认情况下，除了处理日志消息本身之外，每个记录器都会调用其父记录，这通常也会导致消息由根记录器处理。 将此属性设置为false时，需要为此记录器配置Handler，否则不会传递任何记录消息。
 * 属性“config”。 此属性旨在允许运行任意配置代码。 该属性定义了一个以空格或逗号分隔的类名列表。 将为每个命名类创建一个新实例。 每个类的默认构造函数可以执行任意代码来更新日志记录配置，例如设置记录器级别，添加处理程序，添加过滤器等。
 * 请注意，在任何用户类路径之前，首先在系统类路径上搜索在LogManager配置期间加载的所有类。 这包括LogManager类，任何配置类和任何处理程序类。
 * <p>
 * 记录器根据点分隔名称组织成命名层次结构。 因此“abc”是“ab”的孩子，但“a.b1”和a.b2“是同龄人。
 * <p>
 * 名称以“.level”结尾的所有属性都假定为Loggers定义日志级别。 因此，“foo.level”为命名层次结构中的任何子项定义了一个名为“foo”和（递归）的记录器的日志级别。 日志级别按属性文件中定义的顺序应用。 因此，树中子节点的级别设置应该在其父级的设置之后进行。 属性名称“.level”可用于设置树根的级别。
 * <p>
 * LogManager对象上的所有方法都是多线程安全的。
 *
 * @author Adam
 * @since 2023/12/7
 */
public class ClassLoaderLogManager extends LogManager {


}
