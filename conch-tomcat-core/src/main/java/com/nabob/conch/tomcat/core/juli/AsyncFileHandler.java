package com.nabob.conch.tomcat.core.juli;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.LogRecord;

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

    protected volatile boolean closed = false;
    private final Object closeLock = new Object();

    // 丢弃最迟 - 队列尾
    public static final int OVERFLOW_DROP_LAST = 1;
    // 丢弃最早 - 队列头
    public static final int OVERFLOW_DROP_FIRST = 2;
    // 一直尝试再次放入队列，这种模式下，不允许任何丢失（最严格）
    public static final int OVERFLOW_DROP_FLUSH = 3;
    // 丢弃当前 - 当前被reject的task runnable
    public static final int OVERFLOW_DROP_CURRENT = 4;

    // 默认
    public static final int DEFAULT_OVERFLOW_DROP_TYPE = 1;
    public static final int DEFAULT_MAX_RECORDS = 10000;

    // 获取配置 from System Properties
    public static final int OVERFLOW_DROP_TYPE = Integer.parseInt(System.getProperty("org.apache.juli.AsyncOverflowDropType", Integer.toString(DEFAULT_OVERFLOW_DROP_TYPE)));
    public static final int MAX_RECORDS = Integer.parseInt(System.getProperty("org.apache.juli.AsyncMaxRecordCount", Integer.toString(DEFAULT_MAX_RECORDS)));

    /**
     * 异步写日志 线程池
     * <p>
     * - 只有一个线程
     * - 阻塞队列 LinkedBlockingDeque
     * - 拒绝策略
     */
    private static final LoggerExecutorService LOGGER_SERVICE = new LoggerExecutorService(OVERFLOW_DROP_TYPE, MAX_RECORDS);

    public AsyncFileHandler() {
        super();
        LOGGER_SERVICE.registerHandler();
    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }

        // 在给异步线程执行前，先记录下日志的原始方法名
        record.getSourceMethodName();

        LOGGER_SERVICE.execute(new Runnable() {

            @Override
            public void run() {
                /*
                 * During Tomcat shutdown, the Handlers are closed before the executor queue is flushed therefore the
                 * closed flag is ignored if the executor is shutting down.
                 */
                if (!closed || LOGGER_SERVICE.isTerminating()) {
                    publishInternal(record);
                }
            }
        });
    }

    protected void publishInternal(LogRecord record) {
        super.publish(record);
    }

    @Override
    public void open() {
        if (!closed) {
            return;
        }
        synchronized (closeLock) {
            if (!closed) {
                return;
            }
            closed = false;
        }
        LOGGER_SERVICE.registerHandler();
        super.open();
    }

    @Override
    public void close() throws SecurityException {
        if (closed) {
            return;
        }
        synchronized (closeLock) {
            if (closed) {
                return;
            }
            closed = true;
        }
        LOGGER_SERVICE.deregisterHandler();
        super.close();
    }

    static final class LoggerExecutorService extends ThreadPoolExecutor {

        private static final ThreadFactory THREAD_FACTORY = new FileHandlerThreadFactory(THREAD_PREFIX);

        // 记录当前LoggerExecutorService所持有的Handler数量，当Handler数量为0时，并且jvm shutting down 时，关闭线程池
        // 一般我们业务中写ThreadPoolExecutor线程池都不会特殊处理线程池的shutdown，这个提供了一个方式
        // 我们也可以在创建ThreadPoolExecutor时，直接就给JVM挂上shutdown钩子，这样比较粗暴点，没有业务因子判断，只关注JVM shut down
        private final AtomicInteger handlerCount = new AtomicInteger();

        LoggerExecutorService(final int overflowDropType, final int maxRecords) {
            super(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(maxRecords), THREAD_FACTORY);
            switch (overflowDropType) {
                case OVERFLOW_DROP_LAST:
                default:
                    setRejectedExecutionHandler(new DropLastPolicy());
                    break;
                case OVERFLOW_DROP_FIRST:
                    setRejectedExecutionHandler(new DiscardOldestPolicy());
                    break;
                case OVERFLOW_DROP_FLUSH:
                    setRejectedExecutionHandler(new DropFlushPolicy());
                    break;
                case OVERFLOW_DROP_CURRENT:
                    setRejectedExecutionHandler(new DiscardPolicy());
            }
        }

        @Override
        public LinkedBlockingDeque<Runnable> getQueue() {
            return (LinkedBlockingDeque<Runnable>) super.getQueue();
        }

        public void registerHandler() {
            handlerCount.incrementAndGet();
        }

        public void deregisterHandler() {
            int newCount = handlerCount.decrementAndGet();
            if (newCount == 0) {
                try {
                    Thread dummyHook = new Thread();
                    Runtime.getRuntime().addShutdownHook(dummyHook);
                    Runtime.getRuntime().removeShutdownHook(dummyHook);
                } catch (IllegalStateException ise) {
                    // JVM is shutting down.
                    // Allow up to 10s for the queue to be emptied
                    shutdown();
                    try {
                        awaitTermination(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                    shutdownNow();
                }
            }
        }
    }

    /**
     * 拒绝处理：
     * <p>
     * 丢弃队列中最迟的一个
     */
    private static class DropLastPolicy implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (!executor.isShutdown()) {
                // 丢掉一个
                ((LoggerExecutorService) executor).getQueue().pollLast();
                // 继续执行当前被reject的Runnable
                executor.execute(r);
            }
        }
    }

    /**
     * 拒绝处理：
     * <p>
     * 一直尝试再次放入队列，这种模式下，不允许任何丢失（最严格）
     */
    private static class DropFlushPolicy implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            while (true) {
                if (executor.isShutdown()) {
                    break;
                }
                try {
                    if (executor.getQueue().offer(r, 1000, TimeUnit.MILLISECONDS)) {
                        break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RejectedExecutionException("Interrupted", e);
                }
            }
        }
    }
}
