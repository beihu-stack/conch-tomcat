package com.nabob.conch.tomcat.core.juli;

/**
 * 异步 文件日志 Handler
 * <p>
 * 使用队列 queue 实现异步日志
 * <p>
 * 继承 FileHandler 的日志配置
 * 新增系统配置：
 * - org.apache.juli.AsyncOverflowDropType 默认值：1   超出队列大小后的丢弃策略类型
 * - org.apache.juli.AsyncMaxRecordCount 默认值： 10000  队列最大存放日志Record容量
 *
 * @author Adam
 * @since 2023/12/15
 */
public class AsyncFileHandler extends FileHandler {

    static final String THREAD_PREFIX = "AsyncFileHandlerWriter-";



}
