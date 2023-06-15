package com.redis.lecture.circuitbreake;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;
import java.util.function.Supplier;

public class CircuitBreaker {

    private static final Logger LOGGER = LoggerFactory.getLogger(CircuitBreaker.class);

    private State state;
    private final Config config;

    private final Counter counter;
    /**
     * 最后打开时间戳
     */
    private long lastOpenTime;

    public CircuitBreaker(Config config) {
        this.counter = new Counter(config.getFailureCount(), config.getFailureTimeInterval());
        this.state = State.CLOSED;
        this.config = config;
    }

    public <T> T run(Supplier<T> toRun, Function<Throwable, T> fallback) {
        try {
            if (state == State.OPEN) {
                LOGGER.info("当前OPEN状态");
                // 判断是否超时可以进入 half-open 状态
                if (halfOpenTimeout()) {
                    LOGGER.info("半打开超时状态");
                    return halfOpenHandle(toRun, fallback);
                }
                // 直接执行失败回调方法
                return fallback.apply(new DegradeException("degrade by circuit breaker"));
            } else if (state == State.CLOSED) {
                LOGGER.info("当前CLOSED状态");
                T result = toRun.get();
                // 注意重置错误数
                closed();
                return result;
            } else {
                LOGGER.info("当前HALF-OPEN状态");
                return halfOpenHandle(toRun, fallback);
            }
        } catch (Exception e) {
            // 执行失败,错误次数+1
            counter.incrFailureCount();
            if (counter.failureThresholdReached()) {
                LOGGER.info("失败次数超过阈值,变成OPEN状态");
                open();
            }
            return fallback.apply(e);
        }
    }


    private <T> T halfOpenHandle(Supplier<T> toRun, Function<Throwable, T> fallback) {
        try {
            // closed 状态超时进入 half-open 状态
            halfOpen();
            T result = toRun.get();
            int halfOpenSuccessCount = counter.incrSuccessHalfOpenCount();
            if (halfOpenSuccessCount >= this.config.getHalfOpenSuccessCount()) {
                LOGGER.info("半打开次数超过阈值,变成CLOSED状态");
                closed();
            }
            return result;
        } catch (Exception e) {
            // half-open 状态发生一次错误进入 oepn 状态
            open();
            return fallback.apply(new DegradeException("degrade by circuit breaker"));
        }
    }


    /**
     * 判断 Open 是否超时,如果是则进入 Half-Open 状态
     *
     * @return
     */
    private boolean halfOpenTimeout() {
        return System.currentTimeMillis() - lastOpenTime > config.getHalfOpenTimeout();
    }

    private void closed() {
        counter.reset();
        state = State.CLOSED;
    }

    private void open() {
        state = State.OPEN;
        lastOpenTime = System.currentTimeMillis();
    }

    private void halfOpen() {
        state = State.HALF_OPEN;
    }


}
