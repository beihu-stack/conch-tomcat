package com.nabob.conch.tomcat.core.juli;

import java.io.UnsupportedEncodingException;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

/**
 * FileHandler
 *
 * @author Adam
 * @since 2023/12/12
 */
public class FileHandler extends Handler {

    // 永久
    public static final int DEFAULT_MAX_DAYS = -1;
    public static final int DEFAULT_BUFFER_SIZE = -1;

    /**
     * 日志存储的目录
     */
    private String directory;


    /**
     * 日志文件名的前置
     */
    private String prefix;

    private String suffix = ".log";

    /**
     * 日志文件名 正则
     */
    private Pattern pattern;

    /**
     * 日志保存的最大天数
     */
    private Integer maxDays;

    /**
     * 日志Buffer大小
     */
    private Integer bufferSize;

    public FileHandler() {
        configure();
    }

    @Override
    public void publish(LogRecord record) {

    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {

    }

    /**
     * 配置
     * - 读取配置文件
     */
    private void configure() {

        // this可以识别子类的ClassName
        String className = this.getClass().getName();

        ClassLoader cl = ClassLoaderLogManager.getClassLoader();

        if (directory == null) {
            directory = getProperty(className + ".directory", "logs");
        }
        if (prefix == null) {
            prefix = getProperty(className + ".prefix", "juli.");
        }

        pattern = Pattern
            .compile("^(" + Pattern.quote(prefix) + ")\\d{4}-\\d{1,2}-\\d{1,2}(" + Pattern.quote(suffix) + ")$");


        if (maxDays == null) {
            String sMaxDays = getProperty(className + ".maxDays", String.valueOf(DEFAULT_MAX_DAYS));
            try {
                maxDays = Integer.valueOf(sMaxDays);
            } catch (NumberFormatException ignore) {
                maxDays = Integer.valueOf(DEFAULT_MAX_DAYS);
            }
        }

        if (bufferSize == null) {
            String sBufferSize = getProperty(className + ".bufferSize", String.valueOf(DEFAULT_BUFFER_SIZE));
            try {
                bufferSize = Integer.valueOf(sBufferSize);
            } catch (NumberFormatException ignore) {
                bufferSize = Integer.valueOf(DEFAULT_BUFFER_SIZE);
            }
        }

        // 日志编码 encoding
        String encoding = getProperty(className + ".encoding", null);
        if (encoding != null && encoding.length() > 0) {
            try {
                setEncoding(encoding);
            } catch (UnsupportedEncodingException ex) {
                // Ignore
            }
        }

        // 日志级别 level
        setLevel(Level.parse(getProperty(className + ".level", Level.ALL.toString())));

        // 日志Handler的过滤器 filter
        String filterName = getProperty(className + ".filter", null);
        if (filterName != null) {
            try {
                setFilter((Filter) cl.loadClass(filterName).getConstructor().newInstance());
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    private String getProperty(String name, String defaultValue) {
        String value = LogManager.getLogManager().getProperty(name);
        if (value == null) {
            value = defaultValue;
        } else {
            value = value.trim();
        }
        return value;
    }
}
