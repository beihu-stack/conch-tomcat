package com.nabob.conch.tomcat.core.juli;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * FileHandler
 *
 * @author Adam
 * @since 2023/12/12
 */
public class FileHandler extends Handler {

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

    }
}
