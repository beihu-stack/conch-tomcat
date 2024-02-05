package com.nabob.conch.tomcat.core.catalina.lifecycle;

import com.nabob.conch.tomcat.core.juli.logging.Log;
import com.nabob.conch.tomcat.core.juli.logging.LogFactory;
import com.nabob.conch.tomcat.core.tomcat.i18n.StringManager;
import com.nabob.conch.tomcat.core.tomcat.util.ExceptionUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Base impl of Lifecycle
 * <p>
 * 1.监听器 注册、查询、删除、fire
 * 2.状态值属性维护
 * 3.生命周期的通用逻辑
 *
 * @author Adam
 * @since 2023/12/5
 */
public abstract class LifecycleBase implements Lifecycle {

    private static final Log log = LogFactory.getLog(LifecycleBase.class);
    private static final StringManager sm = StringManager.getManager(LifecycleBase.class);

    /**
     * 监听器
     */
    private final List<LifecycleListener> lifecycleListeners = new CopyOnWriteArrayList<>();

    /**
     * 状态
     * <p>
     * 初始为 NEW 状态
     * <p>
     * 对生命周期状态的操作，必须加锁
     */
    private volatile LifecycleState state = LifecycleState.NEW;

    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycleListeners.add(listener);
    }

    @Override
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycleListeners.toArray(new LifecycleListener[0]);
    }

    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycleListeners.remove(listener);
    }

    /**
     * fire 生命周期事件
     */
    protected void fireLifecycleEvent(String type, Object data) {
        LifecycleEvent event = new LifecycleEvent(this, type, data);
        for (LifecycleListener listener : lifecycleListeners) {
            listener.lifecycleEvent(event);
        }
    }

    @Override
    public final synchronized void init() throws LifecycleException {
        if (!state.equals(LifecycleState.NEW)) {
            invalidTransition(BEFORE_INIT_EVENT);
        }

        try {
            // 设置为 初始化中
            setStateInternal(LifecycleState.INITIALIZING, null, false);

            // 模版初始化方法
            initInternal();

            // 设置为 初始化完成
            setStateInternal(LifecycleState.INITIALIZED, null, false);
        } catch (Throwable t) {
            handleSubClassException(t, "lifecycleBase.initFail");
        }
    }

    protected abstract void initInternal() throws LifecycleException;

    /**
     * start 如果 未 init ，则先触发init
     */
    @Override
    public final synchronized void start() throws LifecycleException {
        // 已经start判断
        if (LifecycleState.STARTING_PREP.equals(state) || LifecycleState.STARTING.equals(state) || LifecycleState.STARTED.equals(state)) {
            log.warn("lifecycleBase.alreadyStarted");
            return;
        }

        // 未初始化
        if (LifecycleState.NEW.equals(state)) {
            init();
        }
        // 失败了
        else if (LifecycleState.FAILED.equals(state)) {
            stop();
        }
        // 如果状态不是初始化完成或者已停止，则不可发起start
        else if (!state.equals(LifecycleState.INITIALIZED) && !state.equals(LifecycleState.STOPPED)) {
            invalidTransition(BEFORE_START_EVENT);
            return;
        }

        try {
            // 设置为 启动前
            setStateInternal(LifecycleState.STARTING_PREP, null, false);

            // 模版启动方法：内部必须设置状态为 LifecycleState.STARTING
            startInternal();

            // 如果启动失败
            if (state.equals(LifecycleState.FAILED)) {
                stop();
            }
            // 未设置 启动中 状态
            else if (!state.equals(LifecycleState.STARTING)) {
                invalidTransition(AFTER_START_EVENT);
            }
            // 启动成功
            else {
                // 设置为 启动完成
                setStateInternal(LifecycleState.STARTED, null, false);
            }
        } catch (Throwable t) {
            handleSubClassException(t, "lifecycleBase.startFail");
        }

    }

    protected abstract void startInternal() throws LifecycleException;

    @Override
    public final synchronized void stop() throws LifecycleException {

    }

    @Override
    public LifecycleState getState() {
        return state;
    }


    /**
     * 提供给子类进行状态设置
     */
    protected synchronized void setState(LifecycleState newState) throws LifecycleException {
        setStateInternal(newState, null, true);
    }

    /**
     * 提供给子类进行状态设置
     */
    protected synchronized void setState(LifecycleState newState, Object data) throws LifecycleException {
        setStateInternal(newState, data, true);
    }

    // private

    /**
     * 设置 生命周期 状态 【内部】
     *
     * @param newState 新状态
     * @param data     新状态对应的事件需要publish的事件数据
     * @param check    是否check状态转换流转合法性
     */
    private synchronized void setStateInternal(LifecycleState newState, Object data, boolean check) throws LifecycleException {
        log.debug(sm.getString("lifecycleBase.setState", this, state));

        if (newState == null) {
            invalidTransition("null");
            return;
        }

        if (check) {
            // 所有方法都可以 转义到 FAILED
            // STARTING_PREP 只允许转移到 STARTING
            // STOPPING_PREP 只允许转移到 STOPPING
            // FAILED 可以转移到 STOPPING
            if (!(newState == LifecycleState.FAILED ||
                (this.state == LifecycleState.STARTING_PREP && newState == LifecycleState.STARTING) ||
                (this.state == LifecycleState.STOPPING_PREP && newState == LifecycleState.STOPPING) ||
                (this.state == LifecycleState.FAILED && newState == LifecycleState.STOPPING))) {
                // 非法
                invalidTransition(newState.name());
                return;
            }
        }

        // 转移状态
        this.state = newState;

        // 触发新的状态的的事件
        String lifecycleEvent = newState.getLifecycleEvent();
        if (lifecycleEvent != null) {
            fireLifecycleEvent(lifecycleEvent, data);
        }
    }

    /**
     * 非法的状态转换 统一抛异常
     */
    private void invalidTransition(String type) throws LifecycleException {
        String msg = sm.getString("lifecycleBase.invalidTransition", type, toString(), state);
        throw new LifecycleException(msg);
    }

    /**
     * 处理子类的异常
     *
     * @param t       异常
     * @param message 异常提示
     * @throws LifecycleException LifecycleException
     */
    private void handleSubClassException(Throwable t, String message) throws LifecycleException {
        // 设置状态为失败
        setStateInternal(LifecycleState.FAILED, null, false);

        // 抛异常
        ExceptionUtils.handleThrowable(t);
        if (!(t instanceof LifecycleException)) {
            throw new LifecycleException(message, t);
        }
        throw (LifecycleException) t;
    }
}
