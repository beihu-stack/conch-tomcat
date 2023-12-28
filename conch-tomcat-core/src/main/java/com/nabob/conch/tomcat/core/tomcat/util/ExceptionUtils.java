package com.nabob.conch.tomcat.core.tomcat.util;

/**
 * 异常处理工具类
 *
 * @author Adam
 * @since 2023/12/28
 */
public class ExceptionUtils {

    /**
     * 判断 目标 Throwable 是否需要被 重新thrown 或者 吞掉，一般用于应用的clean场景
     */
    public static void handleThrowable(Throwable t) {
        if (t instanceof ThreadDeath) {
            throw (ThreadDeath) t;
        }

        if (t instanceof StackOverflowError) {
            // 吞掉
            return;
        }

        if (t instanceof VirtualMachineError) {
            throw (VirtualMachineError) t;
        }

        // 其他都吞掉
    }

}
