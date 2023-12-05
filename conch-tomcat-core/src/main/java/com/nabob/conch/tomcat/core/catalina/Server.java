package com.nabob.conch.tomcat.core.catalina;

import com.nabob.conch.tomcat.core.catalina.lifecycle.Lifecycle;
import com.nabob.conch.tomcat.core.catalina.startup.Catalina;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Server（CEO）
 * <p>
 * 负责管理多个事业群，每个事业群就是一个Service
 * <p>
 * Server自身特性：
 * - 端口 port：Server需要依据这个开启 server socket，用于监听特殊事件，比如第一个就是 shutdown 命令 - 简化工作量，暂不实现
 * - 实现Lifecycle，受生命周期管理
 * - 持有公司创始人信息
 * - 持有多个事业部信息
 * - await - 保持Server存活
 * <p>
 * - 持有一个工具线程（助理） - 干杂活的
 *
 * @author Adam
 * @since 2023/12/5
 */
public interface Server extends Lifecycle {

    /**
     * 获取 创始人
     */
    Catalina getCatalina();

    /**
     * 设置 上级 创始人
     */
    void setCatalina(Catalina catalina);

    /**
     * 添加 事业部
     */
    void addService(Service service);

    /**
     * 查找 事业部
     */
    Service findService(String name);

    Service[] findServices();

    void removeService(Service service);

    /**
     * 等待直到 showdown
     */
    void await();

    /**
     * 工具线程（助理） - 干杂活的
     */
    ScheduledExecutorService getUtilityExecutor();
}
