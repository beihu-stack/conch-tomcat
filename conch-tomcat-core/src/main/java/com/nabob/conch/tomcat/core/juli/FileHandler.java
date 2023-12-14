package com.nabob.conch.tomcat.core.juli;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;
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
     * 日志是否可滚动
     * <p>
     * 默认为True
     */
    private Boolean rotatable;

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
     * 日志写入Buffer大小
     */
    private Integer bufferSize;

    /**
     * 当前日志文件的日期，未null表示没有日志文件
     * <p>
     * 格式：yyyy-MM-dd
     */
    private volatile String date = null;

    /**
     * 写日志 Writer
     */
    private volatile PrintWriter writer = null;
    /**
     * 写日志 Writer 锁
     */
    protected final ReadWriteLock writerLock = new ReentrantReadWriteLock();

    public FileHandler() {
        configure();
    }

    @Override
    public void publish(LogRecord record) {
        // 是否可打日志，内部运行filter
        if (!isLoggable(record)) {
            return;
        }

        final String tsDate;
        if (rotatable) {
            Timestamp ts = new Timestamp(System.currentTimeMillis());
            // yyyy-MM-dd
            tsDate = ts.toString().substring(0, 10);
        } else {
            tsDate = "";
        }

        // 处理日志滚动

        // 读写锁：
        // - 公平性：读写锁支持非公平和公平的锁获取方式，非公平锁的吞吐量优于公平锁的吞吐量，默认构造的是非公平锁
        // - 可重入：在线程获取读锁之后能够再次获取读锁，但是不能获取写锁，而线程在获取写锁之后能够再次获取写锁，同时也能获取读锁
        // - 锁降级：线程获取写锁之后获取读锁，再释放写锁，这样实现了写锁变为读锁，也叫锁降级
        // 使用场景:
        // - 适合读多写少（存在写锁，则读锁不能使用）
        //      - 读锁ReentrantReadWriteLock.ReadLock可以被多个线程同时持有, 所以并发能力很高
        //      - 写锁ReentrantReadWriteLock.WriteLock是独占锁, 在一个线程持有写锁时候, 其他线程都不能在抢占, 包含抢占读锁都会阻塞
        // 总结：读读并发、读写互斥、写写互斥
        // 参考：https://www.cnblogs.com/mikechenshare/p/16743733.html
        writerLock.readLock().lock();
        try {
            // 如果当前日期与当前打开的日志日期不一致，则新创建日志文件
            if (!tsDate.equals(date)) {
                // 锁升级 换到写锁，释放读锁（有读锁是不能获取写锁的）
                writerLock.readLock().unlock();
                writerLock.writeLock().lock();
                try {

                    // double check
                    if (!tsDate.equals(date)) {
                        // 关闭当前Writer
                        closeWriter();

                        date = tsDate;

                        // 重新开启Writer
                        openWriter();

                        // 删除旧日志
                        clean();
                    }

                } finally {
                    // 锁降级
                    writerLock.readLock().lock();
                    writerLock.writeLock().unlock();
                }
            }

            // 处理 LogRecord
            // 格式化
            String result = null;
            try {
                result = getFormatter().format(record);
            } catch (Exception e) {
                reportError(null, e, ErrorManager.FORMAT_FAILURE);
                return;
            }

            // 写盘 - 多线程时，PrinterWriter使用了父类的lock进行同步操作
            try {
                if (writer != null) {
                    writer.write(result);
                    if (bufferSize < 0) {
                        writer.flush();
                    }
                } else {
                    reportError("FileHandler is closed or not yet initialized, unable to log [" + result + "]", null,
                        ErrorManager.WRITE_FAILURE);
                }
            } catch (Exception e) {
                reportError(null, e, ErrorManager.WRITE_FAILURE);
            }
        } finally {
            writerLock.readLock().unlock();
        }

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
     * <p>
     * 套路：使用读配置文件方式，来装配Handler（给Handler定制特殊的配置）
     */
    private void configure() {

        // this可以识别子类的ClassName
        String className = this.getClass().getName();

        ClassLoader cl = ClassLoaderLogManager.getClassLoader();

        if (rotatable == null) {
            rotatable = Boolean.valueOf(getProperty(className + ".rotatable", "true"));
        }
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

        // 日志格式化 formatter
        String formatterName = getProperty(className + ".formatter", null);
        if (formatterName != null) {
            try {
                setFormatter((Formatter) cl.loadClass(formatterName).getConstructor().newInstance());
            } catch (Exception e) {
                // Ignore and fallback to defaults
                setFormatter(new OneLineSimpleFormatter());
            }
        } else {
            setFormatter(new OneLineSimpleFormatter());
        }

        // 设置异常管理器
        setErrorManager(new ErrorManager());
    }

    private void openWriter() {
        if (writer != null) {
            return;
        }


        File dir = new File(directory);

        // 创建文件夹（如果需要）
        if (!dir.mkdirs() && !dir.isDirectory()) {
            reportError("无法创建 [" + dir + "]", null, ErrorManager.OPEN_FAILURE);
            writer = null;
            return;
        }

        // 打开当前日志
        writerLock.writeLock().lock();
        FileOutputStream fos = null;
        OutputStream os = null;
        try {
            if (writer != null) {
                return;
            }

            File pathName = new File(dir.getAbsoluteFile(), prefix + (rotatable ? date : "") + suffix);
            String encoding = getEncoding();

            fos = new FileOutputStream(pathName, true);
            os = bufferSize > 0 ? new BufferedOutputStream(fos, bufferSize) : fos;

            writer = new PrintWriter(os, false, Charset.forName(encoding));

            writer.write(getFormatter().getHead(this));

        } catch (FileNotFoundException e) {
            reportError(null, e, ErrorManager.OPEN_FAILURE);
            writer = null;
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    // Ignore
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e1) {
                    // Ignore
                }
            }
        } finally {
            writerLock.writeLock().unlock();
        }
    }

    private void closeWriter() {
        if (writer == null) {
            return;
        }

        // 加可重入写锁，防止别的地方也在调研closeWriter
        writerLock.writeLock().lock();
        try {
            if (writer == null) {
                return;
            }

            // 写一个结尾
            writer.write(getFormatter().getTail(this));
            writer.flush();
            writer.close();
            writer = null;
            date = null;
        } catch (Exception e) {
            reportError(null, e, ErrorManager.CLOSE_FAILURE);
        } finally {
            writerLock.writeLock().unlock();
        }
    }

    /**
     * 清理日志
     */
    private void clean() {
        // todo impl
    }

    /**
     * 获取 可删除的日志目录
     */
    private DirectoryStream<Path> streamFilesForDelete() {
        LocalDate maxDaysOffset = LocalDate.now().minus(maxDays.intValue(), ChronoUnit.DAYS);
        // todo impl
        return null;
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
