package com.nabob.conch.tomcat.core.juli;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * OneLine Simple Formatter
 *
 * @author Adam
 * @since 2023/12/13
 */
public class OneLineSimpleFormatter extends Formatter {

    private static final Object threadMxBeanLock = new Object();

    /**
     * 线程的 MXBean 监控对象
     */
    private static volatile ThreadMXBean threadMxBean = null;

    private static final int THREAD_NAME_CACHE_SIZE = 10000;
    private static final ThreadLocal<ThreadNameCache> threadNameCache = ThreadLocal
        .withInitial(() -> new ThreadNameCache(THREAD_NAME_CACHE_SIZE));

    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();

        // 日期
        sb.append(timeStampToString(record.getMillis(), "dd-MMM-yyyy HH:mm:ss.SSS"));

        // 日志级别
        sb.append(' ');
        sb.append(record.getLevel().getLocalizedName());

        // 线程名
        sb.append(' ');
        sb.append('[');
        final String threadName = Thread.currentThread().getName();

        if (threadName != null && threadName.startsWith(AsyncFileHandler.THREAD_PREFIX)) {
            // 异步线程写日志的话，获取实际的异步线程名
            sb.append(getThreadName(record.getLongThreadID()));
        } else {
            sb.append(threadName);
        }
        sb.append(']');

        // 类名+方法名
        sb.append(' ');
        sb.append(record.getSourceClassName());
        sb.append('.');
        sb.append(record.getSourceMethodName());

        // 日志内容
        sb.append(' ');
        sb.append(formatMessage(record));

        // New line for next record
        sb.append(System.lineSeparator());

        // 异常堆栈
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            record.getThrown().printStackTrace(pw);
            pw.close();
            sb.append(sw.getBuffer());
        }

        return sb.toString();
    }

    public static String timeStampToString(Long timeStamp, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        String date;
        if (timeStamp.toString().length() == 10) {
            date = sdf.format(new Date(timeStamp * 1000L));
        } else {
            date = sdf.format(new Date(timeStamp));
        }

        return date;
    }

    /**
     * 根据线程ID获取线程名称
     * <p>
     * LogRecord有线程ID，但是没有线程名称
     *
     * @param logRecordThreadId 线程id
     * @return 线程名称
     */
    private static String getThreadName(long logRecordThreadId) {
        ThreadNameCache cache = threadNameCache.get();
        String result = cache.get(logRecordThreadId);
        if (result != null) {
            return result;
        }

        if (threadMxBean == null) {
            synchronized (threadMxBeanLock) {
                if (threadMxBean == null) {
                    // ManagementFactory 是整个jvm管理的标记有MXBeans的beans
                    threadMxBean = ManagementFactory.getThreadMXBean();
                }
            }
        }

        ThreadInfo threadInfo = threadMxBean.getThreadInfo(logRecordThreadId);
        if (threadInfo == null) {
            return Long.toString(logRecordThreadId);
        }

        cache.put(logRecordThreadId, threadInfo.getThreadName());

        return threadInfo.getThreadName();
    }

    /*
     * This is an LRU cache.
     */
    private static class ThreadNameCache extends LinkedHashMap<Long, String> {

        private static final long serialVersionUID = 1L;

        private final int cacheSize;

        ThreadNameCache(int cacheSize) {
            super(cacheSize, 0.75f, true);
            this.cacheSize = cacheSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, String> eldest) {
            return (size() > cacheSize);
        }
    }
}
