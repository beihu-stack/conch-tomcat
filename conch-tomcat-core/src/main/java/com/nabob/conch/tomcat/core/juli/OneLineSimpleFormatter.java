package com.nabob.conch.tomcat.core.juli;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * OneLine Simple Formatter
 *
 * @author Adam
 * @since 2023/12/13
 */
public class OneLineSimpleFormatter extends Formatter {

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
        sb.append(threadName);
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
}
